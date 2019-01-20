package com.soapboxrace.core.api;

import com.soapboxrace.core.api.util.ConcurrentUtil;
import com.soapboxrace.core.api.util.LauncherChecks;
import com.soapboxrace.core.api.util.Secured;
import com.soapboxrace.core.bo.*;
import com.soapboxrace.core.dao.FriendDAO;
import com.soapboxrace.core.dao.PersonaDAO;
import com.soapboxrace.core.jpa.BanEntity;
import com.soapboxrace.core.jpa.FriendEntity;
import com.soapboxrace.core.jpa.PersonaEntity;
import com.soapboxrace.core.jpa.UserEntity;
import com.soapboxrace.core.xmpp.OpenFireSoapBoxCli;
import com.soapboxrace.jaxb.*;
import com.soapboxrace.jaxb.http.ArrayOfBadgePacket;
import com.soapboxrace.jaxb.http.PersonaBase;
import com.soapboxrace.jaxb.http.UserInfo;
import com.soapboxrace.jaxb.login.LoginStatusVO;
import com.soapboxrace.jaxb.xmpp.XMPP_ResponseTypePersonaBase;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@Path("User")
public class User
{

	@Context
	UriInfo uri;

	@Context
	private HttpServletRequest sr;

	@EJB
	private AuthenticationBO authenticationBO;

	@EJB
	private UserBO userBO;

	@EJB
	private TokenSessionBO tokenBO;

	@EJB
	private OnlineUsersBO onlineUsersBO;

	@EJB
	private ParameterBO parameterBO;

	@EJB
	private PresenceManager presenceManager;

	@EJB
	private FriendDAO friendDAO;

	@EJB
	private PersonaDAO personaDAO;

	@EJB
	private OpenFireSoapBoxCli openFireSoapBoxCli;

	@POST
	@Secured
	@Path("GetPermanentSession")
	@Produces(MediaType.APPLICATION_XML)
	public Response getPermanentSession(@HeaderParam("securityToken") String securityToken, @HeaderParam("userId") Long userId)
	{
		UserEntity userEntity = tokenBO.getUser(securityToken);
		BanEntity ban = authenticationBO.checkUserBan(userEntity);

		if (ban != null)
		{
			// Ideally this will never happen. Then again, plenty of weird stuff has happened.
			tokenBO.deleteByUserId(userId);

			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(
					"<EngineExceptionTrans xmlns=\"http://schemas.datacontract.org/2004/07/Victory.Service\">" +
							"<ErrorCode>-1613</ErrorCode>" +
							"<InnerException>" +
							"<ErrorCode>-1613</ErrorCode>" +
							"</InnerException>" +
							"</EngineExceptionTrans>").build();
		}

		int numberOfUsersOnlineNow = onlineUsersBO.getNumberOfUsersOnlineNow();
		int maxOnlinePlayers = parameterBO.getIntParam("MAX_ONLINE_PLAYERS");

		if (maxOnlinePlayers != -1) {
			if (numberOfUsersOnlineNow >= maxOnlinePlayers && !userEntity.isPremium()) {
				return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(
						"<EngineExceptionTrans xmlns=\"http://schemas.datacontract.org/2004/07/Victory.Service\">" +
								"<ErrorCode>-521</ErrorCode>" +
								"<InnerException>" +
								"<ErrorCode>-521</ErrorCode>" +
								"</InnerException>" +
								"</EngineExceptionTrans>").build();
			}
		}

		tokenBO.deleteByUserId(userId);
		URI myUri = uri.getBaseUri();
		String randomUUID = tokenBO.createToken(userId, myUri.getHost());
		UserInfo userInfo = userBO.getUserById(userId);
		userInfo.getUser().setSecurityToken(randomUUID);
		userBO.createXmppUser(userInfo);
		return Response.ok(userInfo).build();
	}

	@POST
	@Secured
	@Path("SecureLoginPersona")
	@Produces(MediaType.APPLICATION_XML)
	public String secureLoginPersona(@HeaderParam("securityToken") String securityToken, @HeaderParam("userId") Long userId,
									 @QueryParam("personaId") Long personaId)
	{
		tokenBO.setActivePersonaId(securityToken, personaId, false);
		userBO.secureLoginPersona(userId, personaId);
		return "";
	}

	@POST
	@Secured
	@Path("SecureLogoutPersona")
	@Produces(MediaType.APPLICATION_XML)
	public String secureLogoutPersona(@HeaderParam("securityToken") String securityToken, @HeaderParam("userId") Long userId,
									  @QueryParam("personaId") Long personaId)
	{
		long activePersonaId = tokenBO.getActivePersonaId(securityToken);
		PersonaEntity personaEntity = personaDAO.findById(activePersonaId);
		tokenBO.setActivePersonaId(securityToken, 0L, true);

		presenceManager.removePresence(activePersonaId);

		ConcurrentUtil.EXECUTOR_SERVICE.submit(() -> {
			List<FriendEntity> friends = friendDAO.findByUserId(personaEntity.getUser().getId());

			for (FriendEntity friend : friends)
			{
				XMPP_ResponseTypePersonaBase personaPacket = new XMPP_ResponseTypePersonaBase();
				PersonaBase xmppPersonaBase = new PersonaBase();

				xmppPersonaBase.setBadges(new ArrayOfBadgePacket());
				xmppPersonaBase.setIconIndex(personaEntity.getIconIndex());
				xmppPersonaBase.setLevel(personaEntity.getLevel());
				xmppPersonaBase.setMotto(personaEntity.getMotto());
				xmppPersonaBase.setName(personaEntity.getName());
				xmppPersonaBase.setPersonaId(personaEntity.getPersonaId());
				xmppPersonaBase.setPresence(0);
				xmppPersonaBase.setScore(personaEntity.getScore());
				xmppPersonaBase.setUserId(personaEntity.getUser().getId());

				personaPacket.setPersonaBase(xmppPersonaBase);

				openFireSoapBoxCli.send(personaPacket, friend.getPersonaId());
			}
		});

		return "";
	}

	@POST
	@Secured
	@Path("SecureLogout")
	@Produces(MediaType.APPLICATION_XML)
	public String secureLogout(@HeaderParam("securityToken") String securityToken)
	{
		Long activePersonaId = tokenBO.getActivePersonaId(securityToken);

		if (Objects.isNull(activePersonaId) || activePersonaId == 0L)
		{
			return "";
		}

		PersonaEntity personaEntity = personaDAO.findById(activePersonaId);
		tokenBO.setActivePersonaId(securityToken, 0L, true);
		presenceManager.removePresence(activePersonaId);

		ConcurrentUtil.EXECUTOR_SERVICE.submit(() -> {
			List<FriendEntity> friends = friendDAO.findByUserId(personaEntity.getUser().getId());

			for (FriendEntity friend : friends)
			{
				XMPP_ResponseTypePersonaBase personaPacket = new XMPP_ResponseTypePersonaBase();
				PersonaBase xmppPersonaBase = new PersonaBase();

				xmppPersonaBase.setBadges(new ArrayOfBadgePacket());
				xmppPersonaBase.setIconIndex(personaEntity.getIconIndex());
				xmppPersonaBase.setLevel(personaEntity.getLevel());
				xmppPersonaBase.setMotto(personaEntity.getMotto());
				xmppPersonaBase.setName(personaEntity.getName());
				xmppPersonaBase.setPersonaId(personaEntity.getPersonaId());
				xmppPersonaBase.setPresence(0);
				xmppPersonaBase.setScore(personaEntity.getScore());
				xmppPersonaBase.setUserId(personaEntity.getUser().getId());

				personaPacket.setPersonaBase(xmppPersonaBase);

				openFireSoapBoxCli.send(personaPacket, friend.getPersonaId());
			}
		});

		return "";
	}

	@GET
	@Path("authenticateUser")
	@Produces(MediaType.APPLICATION_XML)
	@LauncherChecks
	public Response authenticateUser(@QueryParam("email") String email, @QueryParam("password") String password)
	{
		LoginStatusVO loginStatusVO = tokenBO.login(email, password, sr);
		if (loginStatusVO.isLoginOk())
		{
			return Response.ok(loginStatusVO).build();
		}
		return Response.serverError().entity(loginStatusVO).build();
	}

	@GET
	@Path("createUser")
	@Produces(MediaType.APPLICATION_XML)
	@LauncherChecks
	public Response createUser(@QueryParam("email") String email, @QueryParam("password") String password, @QueryParam("inviteTicket") String inviteTicket)
	{
		LoginStatusVO loginStatusVO = userBO.createUserWithTicket(email, password, inviteTicket);
		if (loginStatusVO != null && loginStatusVO.isLoginOk())
		{
			loginStatusVO = tokenBO.login(email, password, sr);
			return Response.ok(loginStatusVO).build();
		}
		return Response.serverError().entity(loginStatusVO).build();
	}

	@POST
	@Path("modernRegister")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@LauncherChecks
	public Response modernRegister(ModernRegisterRequest req)
	{
		try {
			userBO.createModernUser(req.getEmail(), req.getPassword(), req.getTicket());
		} catch (AuthException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(new JSONError(e.getMessage())).build();
		}
		return Response.ok(new ModernRegisterResponse("Account created!")).build();
	}

	@POST
	@Path("modernAuth")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@LauncherChecks
	public Response modernAuth(ModernAuthRequest req)
	{
		try {
			ModernAuthResponse resp = tokenBO.modernLogin(req.getEmail(), req.getPassword(), req.getUpgrade());
			return Response.ok(resp).build();
		} catch (AuthException e) {
			return Response.status(Response.Status.BAD_REQUEST).entity(new JSONError(e.getMessage())).build();
		}
	}
}