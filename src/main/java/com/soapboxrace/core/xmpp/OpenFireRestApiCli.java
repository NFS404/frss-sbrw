package com.soapboxrace.core.xmpp;

import com.soapboxrace.core.bo.ParameterBO;
import org.igniterealtime.restclient.entity.MUCRoomEntities;
import org.igniterealtime.restclient.entity.MUCRoomEntity;
import org.igniterealtime.restclient.entity.UserEntity;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Startup
@Singleton
public class OpenFireRestApiCli
{
	private String openFireToken;
	private String openFireAddress;
	private boolean restApiEnabled = false;

	@EJB
	private ParameterBO parameterBO;

	@PostConstruct
	public void init()
	{
		openFireToken = parameterBO.getStrParam("OPENFIRE_TOKEN");
		openFireAddress = parameterBO.getStrParam("OPENFIRE_ADDRESS");
		if (openFireToken != null && openFireAddress != null)
		{
			restApiEnabled = true;
		}
		createUpdatePersona("sbrw.engine.engine", openFireToken);
	}

	private Builder getBuilder(String path)
	{
		Client client = ClientBuilder.newClient();
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
		Builder builder = getBuilder("users/" + user);
		Response response = builder.get();
		if (response.getStatus() == 200)
		{
			response.close();
			UserEntity userEntity = builder.get(UserEntity.class);
			userEntity.setPassword(password);
			builder = getBuilder("users/" + user);
			builder.put(Entity.entity(userEntity, MediaType.APPLICATION_XML));
		} else
		{
			response.close();
			builder = getBuilder("users");
			UserEntity userEntity = new UserEntity(user, null, null, password);
			builder.post(Entity.entity(userEntity, MediaType.APPLICATION_XML));
		}
		response.close();
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
		Builder builder = getBuilder("system/statistics/sessions");
		SessionsCount sessionsCount = builder.get(SessionsCount.class);
		int clusterSessions = sessionsCount.getClusterSessions();
		if (clusterSessions > 1)
		{
			return clusterSessions - 1;
		}
		return 0;
	}

	public List<Long> getAllPersonaByGroup(Long personaId)
	{
		if (!restApiEnabled)
		{
			return new ArrayList<>();
		}
		Builder builder = getBuilder("chatrooms");
		MUCRoomEntities roomEntities = builder.get(MUCRoomEntities.class);
		List<MUCRoomEntity> listRoomEntity = roomEntities.getMucRooms();
		for (MUCRoomEntity entity : listRoomEntity)
		{
			String roomName = entity.getRoomName();
			if (roomName.contains("group.channel."))
			{
				List<Long> groupMembers = getAllOccupantsInRoom(roomName);
				if (groupMembers.contains(personaId)) {
					return groupMembers;
				}
			}
		}
		return new ArrayList<>();
	}

	public List<Long> getAllOccupantsInRoom(String roomName)
	{
		Builder builder = getBuilder("chatrooms/" + roomName + "/occupants");
		OccupantEntities occupantEntities = builder.get(OccupantEntities.class);
		List<Long> listOfPersona = new ArrayList<Long>();
		if (occupantEntities.getOccupants() != null) {
			for (OccupantEntity entity : occupantEntities.getOccupants()) {
				String jid = entity.getJid();
				try {
					Long personaId = Long.parseLong(jid.substring(jid.lastIndexOf('.') + 1));
					listOfPersona.add(personaId);
				} catch (Exception e) {
					//
				}
			}
		}
		return listOfPersona;
	}

	public List<MUCRoomEntity> getAllRooms() {
		Builder builder = getBuilder("chatrooms");
		MUCRoomEntities roomEntities = builder.get(MUCRoomEntities.class);

		return roomEntities.getMucRooms();
	}
}