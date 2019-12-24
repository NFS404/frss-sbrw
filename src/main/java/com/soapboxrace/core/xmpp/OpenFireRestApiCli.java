package com.soapboxrace.core.xmpp;

import com.soapboxrace.core.bo.ParameterBO;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Startup
@Singleton
public class OpenFireRestApiCli
{
	private String openFireToken;
	private String openFireAddress;
	private String domain;
	private boolean restApiEnabled = false;
	private Client client;

	@EJB
	private ParameterBO parameterBO;

	@PostConstruct
	public void init()
	{
		openFireToken = parameterBO.getStrParam("OPENFIRE_TOKEN");
		openFireAddress = parameterBO.getStrParam("OPENFIRE_ADDRESS");
		domain = parameterBO.getStrParam("XMPP_IP");
		if (openFireToken != null && openFireAddress != null)
		{
			restApiEnabled = true;
			client = ClientBuilder.newClient();
		}
	}

	private Builder getBuilder(String path)
	{
		WebTarget target = client.target(openFireAddress).path(path);
		Builder request = target.request(MediaType.APPLICATION_XML);
		request.header("Authorization", openFireToken);
		return request;
	}

	public void createUpdatePersona(String user, String password)
	{
		if (!restApiEnabled)
		{
			return;
		}
		Builder builder = getBuilder("users");
		UserEntity userEntity = new UserEntity(user, password);
		builder.post(Entity.entity(userEntity, MediaType.APPLICATION_JSON)).close();
	}

	private void deletePersona(String user) {
		Builder builder = getBuilder("users/"+user);
		builder.delete().close();
	}

	public void createUpdatePersona(Long personaId, String password)
	{
		String user = "sbrw." + personaId.toString();
		createUpdatePersona(user, password);
	}

	public int getTotalOnlineUsers()
	{
		if (!restApiEnabled)
		{
			return 0;
		}
		return getSessions().size();
	}

	private List<String> getSessions() {
		Builder builder = getBuilder("sessions");
		return builder.get(new GenericType<List<String>>() {});
	}

	public List<RoomEntity> getAllRooms() {
		Builder builder = getBuilder("rooms");
		return builder.get(new GenericType<List<RoomEntity>>() {});
	}

	public List<Long> getAllPersonaByGroup(Long personaId)
	{
		if (!restApiEnabled)
		{
			return new ArrayList<>();
		}
		List<RoomEntity> roomEntities = getAllRooms();
		for (RoomEntity entity : roomEntities)
		{
			String roomName = entity.getName();
			if (roomName.startsWith("group.channel."))
			{
				List<Long> groupMembers = namesToPersonas(entity.getMembers());
				if (groupMembers.contains(personaId)) {
					return groupMembers;
				}
			}
		}
		return new ArrayList<>();
	}

	public List<Long> getOnlinePersonas() {
		List<String> entities = getSessions();
		return namesToPersonas(entities);
	}

	public void sendMessage(Long to, String message) {
		sendMessage("sbrw." + to, message);
	}

	public void sendMessage(String to, String message) {
		Builder builder = getBuilder("users/" + to + "/message");
		MessageEntity entity = new MessageEntity();
		entity.setBody(message);
		entity.setFrom("sbrw.engine.engine@" + domain + "/EA_Chat");
		entity.calculateHash(to + "@" + domain);
		builder.post(Entity.json(entity)).close();
	}

	private List<Long> namesToPersonas(List<String> names) {
		List<Long> personaList = new ArrayList<>();

		for (String name : names) {
			try {
				Long personaId = Long.parseLong(name.substring(name.lastIndexOf('.') + 1));
				personaList.add(personaId);
			} catch (Exception e) {
				//
			}
		}
		return personaList;
	}
}