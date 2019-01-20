package com.soapboxrace.core.bo;

import java.time.LocalDateTime;
import java.util.Date;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;

import com.soapboxrace.core.api.util.GeoIp2;
import com.soapboxrace.core.api.util.UUIDGen;
import com.soapboxrace.core.dao.TokenSessionDAO;
import com.soapboxrace.core.dao.UserDAO;
import com.soapboxrace.core.jpa.PersonaEntity;
import com.soapboxrace.core.jpa.TokenSessionEntity;
import com.soapboxrace.core.jpa.UserEntity;
import com.soapboxrace.jaxb.*;
import com.soapboxrace.jaxb.login.LoginStatusVO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;

@Stateless
public class TokenSessionBO {
	@EJB
	private TokenSessionDAO tokenDAO;

	@EJB
	private UserDAO userDAO;

	@EJB
	private ParameterBO parameterBO;

	@EJB
	private GetServerInformationBO serverInfoBO;

	@EJB
	private OnlineUsersBO onlineUsersBO;

	public boolean verifyToken(Long userId, String securityToken) {
		TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
		if (tokenSessionEntity == null || !tokenSessionEntity.getUserId().equals(userId)) {
			return false;
		}
		long time = new Date().getTime();
		long tokenTime = tokenSessionEntity.getExpirationDate().getTime();
		if (time > tokenTime) {
			return false;
		}
		tokenSessionEntity.setExpirationDate(getMinutes(3));
		tokenDAO.update(tokenSessionEntity);
		return true;
	}

	public void updateToken(String securityToken) {
		TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
		Date expirationDate = getMinutes(3);
		tokenSessionEntity.setExpirationDate(expirationDate);
		tokenDAO.update(tokenSessionEntity);
	}

	public String createToken(Long userId, String clientHostName) {
		TokenSessionEntity tokenSessionEntity = new TokenSessionEntity();
		Date expirationDate = getMinutes(15);
		tokenSessionEntity.setExpirationDate(expirationDate);
		String randomUUID = UUIDGen.getRandomUUID();
		tokenSessionEntity.setSecurityToken(randomUUID);
		tokenSessionEntity.setUserId(userId);
		UserEntity userEntity = userDAO.findById(userId);
		tokenSessionEntity.setPremium(userEntity.isPremium());
		tokenSessionEntity.setClientHostIp(clientHostName);
		tokenSessionEntity.setActivePersonaId(0L);
		tokenDAO.insert(tokenSessionEntity);
		return randomUUID;
	}

	public boolean verifyPersona(String securityToken, Long personaId) {
		TokenSessionEntity tokenSession = tokenDAO.findById(securityToken);
		if (tokenSession == null) {
			throw new NotAuthorizedException("Invalid session...");
		}

		UserEntity user = userDAO.findById(tokenSession.getUserId());
		if (!user.ownsPersona(personaId)) {
			throw new NotAuthorizedException("Persona is not owned by user");
		}
		return true;
	}

	public void deleteByUserId(Long userId) {
		TokenSessionEntity token = tokenDAO.findByUserId(userId);
		if (token != null) {
			if (token.getCryptoTicket() != null) {
				revokeCryptoTicket(token.getCryptoTicket());
			}
			tokenDAO.deleteByUserId(userId);
		}
	}

	private Date getMinutes(int minutes) {
		long time = new Date().getTime();
		time = time + (minutes * 60000);
		Date date = new Date(time);
		return date;
	}

	public LoginStatusVO checkGeoIp(String ip) {
		LoginStatusVO loginStatusVO = new LoginStatusVO(0L, "", false);
		String allowedCountries = serverInfoBO.getServerInformation().getAllowedCountries();
		if (allowedCountries != null && !allowedCountries.isEmpty()) {
			String geoip2DbFilePath = parameterBO.getStrParam("GEOIP2_DB_FILE_PATH");
			GeoIp2 geoIp2 = GeoIp2.getInstance(geoip2DbFilePath);
			if (geoIp2.isCountryAllowed(ip, allowedCountries)) {
				return new LoginStatusVO(0L, "", true);
			} else {
				loginStatusVO.setDescription("GEOIP BLOCK ACTIVE IN THIS SERVER, ALLOWED COUNTRIES: [" + allowedCountries + "]");
			}
		} else {
			return new LoginStatusVO(0L, "", true);
		}
		return loginStatusVO;
	}

	public LoginStatusVO login(String email, String password, HttpServletRequest httpRequest) {
		LoginStatusVO loginStatusVO = checkGeoIp(httpRequest.getRemoteAddr());
		if (!loginStatusVO.isLoginOk()) {
			return loginStatusVO;
		}
		loginStatusVO = new LoginStatusVO(0L, "", false);

		if (email != null && !email.isEmpty() && password != null && !password.isEmpty()) {
			UserEntity userEntity = userDAO.findByEmail(email);
			if (userEntity != null) {
				int numberOfUsersOnlineNow = onlineUsersBO.getNumberOfUsersOnlineNow();
				int maxOlinePayers = parameterBO.getIntParam("MAX_ONLINE_PLAYERS");
				if (numberOfUsersOnlineNow >= maxOlinePayers && !userEntity.isPremium()) {
					loginStatusVO.setDescription("SERVER FULL");
					return loginStatusVO;
				}
				if (password.equals(userEntity.getPassword())) {
					if (userEntity.getHwid() == null || userEntity.getHwid().trim().isEmpty()) {
						userEntity.setHwid(httpRequest.getHeader("X-HWID"));
					}

					if (userEntity.getIpAddress() == null || userEntity.getIpAddress().trim().isEmpty()) {
						String forwardedFor;
						if ((forwardedFor = httpRequest.getHeader("X-Forwarded-For")) != null && parameterBO.getBoolParam("USE_FORWARDED_FOR")) {
							userEntity.setIpAddress(parameterBO.getBoolParam("GOOGLE_LB_ENABLED") ? forwardedFor.split(",")[0] : forwardedFor);
						} else {
							userEntity.setIpAddress(httpRequest.getRemoteAddr());
						}
					}

					userEntity.setLastLogin(LocalDateTime.now());
					userDAO.update(userEntity);
					Long userId = userEntity.getId();
					deleteByUserId(userId);
					String randomUUID = createToken(userId, null);
					loginStatusVO = new LoginStatusVO(userId, randomUUID, true);
					loginStatusVO.setDescription("");

					return loginStatusVO;
				}
			}
		}
		loginStatusVO.setDescription("LOGIN ERROR");
		return loginStatusVO;
	}

	public Long getActivePersonaId(String securityToken) {
		TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
		return tokenSessionEntity.getActivePersonaId();
	}

	public void setActivePersonaId(String securityToken, Long personaId, Boolean isLogout) {
		TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);

		if (!isLogout) {
			if (!userDAO.findById(tokenSessionEntity.getUserId()).ownsPersona(personaId)) {
				throw new NotAuthorizedException("Persona not owned by user");
			}
		}

		tokenSessionEntity.setActivePersonaId(personaId);
		tokenDAO.update(tokenSessionEntity);
	}

	public String getActiveRelayCryptoTicket(String securityToken) {
		TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
		return tokenSessionEntity.getRelayCryptoTicket();
	}

	public Long getActiveLobbyId(String securityToken) {
		TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
		return tokenSessionEntity.getActiveLobbyId();
	}

	public void setActiveLobbyId(String securityToken, Long lobbyId) {
		TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
		tokenSessionEntity.setActiveLobbyId(lobbyId);
		tokenDAO.update(tokenSessionEntity);
	}

	public String getCryptoTicket(String securityToken) {
		if (!parameterBO.getBoolParam("FREEROAM_TS_ENABLED")) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(32);
			byteBuffer.put(new byte[] { 10, 11, 12, 13 });
			byte[] cryptoTicketBytes = byteBuffer.array();
			return Base64.getEncoder().encodeToString(cryptoTicketBytes);
		}
		TokenSessionEntity token = tokenDAO.findById(securityToken);
		if (token.getCryptoTicket() != null) {
			return token.getCryptoTicket();
		}
		UserEntity user = userDAO.findById(token.getUserId());
		TSTicketRequest req = new TSTicketRequest();
		for (PersonaEntity persona : user.getListOfProfile()) {
			req.addPersona(persona.getPersonaId());
		}
		TSTicketResponse res = ClientBuilder.newClient()
				.target(parameterBO.getStrParam("FREEROAM_TS_ENDPOINT"))
				.path("/api/v1/tickets/request")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.header("Authorization", "Bearer " + parameterBO.getStrParam("FREEROAM_TS_APIKEY"))
				.post(Entity.json(req), TSTicketResponse.class);
		token.setCryptoTicket(res.getTicket());
		tokenDAO.update(token);
		return res.getTicket();
	}

	public void revokeCryptoTicket(String ticket) {
		if (!parameterBO.getBoolParam("FREEROAM_TS_ENABLED")) return;
		TSRevokeRequest req = new TSRevokeRequest(ticket);
		ClientBuilder.newClient()
				.target(parameterBO.getStrParam("FREEROAM_TS_ENDPOINT"))
				.path("/api/v1/tickets/revoke")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.header("Authorization", "Bearer " + parameterBO.getStrParam("FREEROAM_TS_APIKEY"))
				.post(Entity.json(req));
	}

	public boolean isPremium(String securityToken) {
		return tokenDAO.findById(securityToken).isPremium();
	}

	public boolean isAdmin(String securityToken) {
		return getUser(securityToken).isAdmin();
	}

	public UserEntity getUser(String securityToken) {
		return userDAO.findById(tokenDAO.findById(securityToken).getUserId());
	}
}