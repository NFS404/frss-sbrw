package com.soapboxrace.core.api;

import com.soapboxrace.core.api.util.Secured;
import com.soapboxrace.core.bo.HardwareInfoBO;
import com.soapboxrace.core.bo.TokenSessionBO;
import com.soapboxrace.core.dao.UserDAO;
import com.soapboxrace.core.jpa.UserEntity;
import com.soapboxrace.jaxb.http.HardwareInfo;
import com.soapboxrace.jaxb.util.UnmarshalXML;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/Reporting")
public class Reporting {

	@EJB
	private HardwareInfoBO hardwareInfoBO;

	@EJB
	private TokenSessionBO tokenBO;

	@EJB
	private UserDAO userDAO;

	@POST
	@Secured
	@Path("/SendHardwareInfo")
	@Produces(MediaType.APPLICATION_XML)
	public String sendHardwareInfo(InputStream is, @HeaderParam("securityToken") String securityToken) {
		HardwareInfo hardwareInfo = UnmarshalXML.unMarshal(is, HardwareInfo.class);
		UserEntity user = tokenBO.getUser(securityToken);
		user.setGameHardwareHash(hardwareInfoBO.calcHardwareInfoHash(hardwareInfo));
		userDAO.update(user);
		return "";
	}

	@POST
	@Secured
	@Path("/SendUserSettings")
	@Produces(MediaType.APPLICATION_XML)
	public String sendUserSettings() {
		return "";
	}

	@GET
	@Secured
	@Path("/SendMultiplayerConnect")
	@Produces(MediaType.APPLICATION_XML)
	public String sendMultiplayerConnect() {
		return "";
	}

	@GET
	@Secured
	@Path("/SendClientPingTime")
	@Produces(MediaType.APPLICATION_XML)
	public String sendClientPingTime() {
		return "";
	}

	@GET
	@Secured
	@Path("/LoginAnnouncementClicked")
	@Produces(MediaType.APPLICATION_XML)
	public String loginAnnouncementClicked() {
		return "";
	}

	@PUT
	@Path("{path:.*}")
	@Produces(MediaType.APPLICATION_XML)
	public String genericEmptyPut(@PathParam("path") String path) {
		System.out.println("empty PUT!!!");
		return "";
	}
}
