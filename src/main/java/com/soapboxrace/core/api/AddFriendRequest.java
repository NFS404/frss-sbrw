package com.soapboxrace.core.api;

import com.soapboxrace.core.api.util.Secured;
import com.soapboxrace.core.bo.TokenSessionBO;
import com.soapboxrace.core.dao.FriendDAO;
import com.soapboxrace.core.dao.PersonaDAO;
import com.soapboxrace.core.jpa.FriendEntity;
import com.soapboxrace.core.jpa.PersonaEntity;
import com.soapboxrace.core.xmpp.OpenFireSoapBoxCli;
import com.soapboxrace.core.xmpp.XmppChat;
import com.soapboxrace.jaxb.http.FriendPersona;
import com.soapboxrace.jaxb.http.FriendResult;
import com.soapboxrace.jaxb.util.MarshalXML;
import com.soapboxrace.jaxb.xmpp.XMPP_FriendPersonaType;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/addfriendrequest")
public class AddFriendRequest
{
	@EJB
	private TokenSessionBO sessionBO;

	@EJB
	private PersonaDAO personaDAO;

	@EJB
	private OpenFireSoapBoxCli openFireSoapBoxCli;

	@EJB
	private FriendDAO friendDAO;

	@GET
	@Secured
	@Produces(MediaType.APPLICATION_XML)
	public FriendResult addFriendRequest(@HeaderParam("securityToken") String securityToken, @QueryParam("displayName") String displayName)
	{
		long activePersonaId = sessionBO.getActivePersonaId(securityToken);
		PersonaEntity active = personaDAO.findById(activePersonaId);
		PersonaEntity target = personaDAO.findByName(displayName);

		FriendResult friendResult = new FriendResult();
		// Result codes:
		// 0 -> success
		// 1 -> cannot add yourself
		// 2 -> already in list
		// 3 -> does not exist
		// 4 -> list full

		if (target == null || active == null) {
			friendResult.setResult(3);
			return friendResult;
		}

		if (active.getUser().getId().equals(target.getUser().getId())) {
			friendResult.setResult(1);
			return friendResult;
		}

		if (friendDAO.findBySenderAndRecipient(target.getUser().getId(), activePersonaId) != null)
		{
			friendResult.setResult(2);
			return friendResult;
		}

		openFireSoapBoxCli.send(XmppChat.createSystemMessage(String.format("You received a friend request from %s!", active.getName())), target.getPersonaId());

		FriendPersona resultFriendPersona = new FriendPersona();

		resultFriendPersona.setIconIndex(target.getIconIndex());
		resultFriendPersona.setLevel(target.getLevel());
		resultFriendPersona.setName(target.getName());
		resultFriendPersona.setOriginalName(null);
		resultFriendPersona.setPersonaId(target.getPersonaId());
		resultFriendPersona.setPresence(0);
		resultFriendPersona.setSocialNetwork(0);
		resultFriendPersona.setUserId(target.getUser().getId());

		friendResult.setResult(0);
		friendResult.setFriendPersona(resultFriendPersona);

		XMPP_FriendPersonaType friendPersona = new XMPP_FriendPersonaType();
		friendPersona.setIconIndex(active.getIconIndex());
		friendPersona.setLevel(active.getLevel());
		friendPersona.setName(active.getName());
		friendPersona.setOriginalName(active.getName());
		friendPersona.setPersonaId(activePersonaId);
		friendPersona.setPresence(3);
		friendPersona.setUserId(active.getUser().getId());

		openFireSoapBoxCli.send(MarshalXML.marshal(friendPersona), target.getPersonaId());

		FriendEntity friendEntity = new FriendEntity();
		friendEntity.setPersonaId(active.getPersonaId());
		friendEntity.setUser(target.getUser());
		friendEntity.setStatus(0);

		friendDAO.insert(friendEntity);

		return friendResult;
	}
}