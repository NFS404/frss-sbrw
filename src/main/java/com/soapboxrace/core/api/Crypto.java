package com.soapboxrace.core.api;

import com.soapboxrace.core.api.util.Secured;
import com.soapboxrace.core.api.util.UUIDGen;
import com.soapboxrace.core.bo.TokenSessionBO;
import com.soapboxrace.jaxb.http.UdpRelayCryptoTicket;
import com.soapboxrace.udp.UDPClient;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Base64;

@Path("/crypto")
public class Crypto {

	@EJB
	private TokenSessionBO tokenBO;

	@EJB
	private UDPClient udpClient;

	@GET
	@Secured
	@Path("/relaycryptoticket/{personaId}")
	@Produces(MediaType.APPLICATION_XML)
	public UdpRelayCryptoTicket relayCryptoTicket(@HeaderParam("securityToken") String securityToken, @PathParam("personaId") Long personaId) {
		byte[] randomUUIDBytes = UUIDGen.getRandomUUIDBytes();
		String ticketIV = Base64.getEncoder().encodeToString(randomUUIDBytes);
		udpClient.sendRaceUdpKey(randomUUIDBytes);
		UdpRelayCryptoTicket udpRelayCryptoTicket = new UdpRelayCryptoTicket();
		String activeRelayCryptoTicket = tokenBO.getActiveRelayCryptoTicket(securityToken);
		udpRelayCryptoTicket.setCryptoTicket(activeRelayCryptoTicket);
		udpRelayCryptoTicket.setSessionKey("AAAAAAAAAAAAAAAAAAAAAA==");
		udpRelayCryptoTicket.setTicketIv(ticketIV);
		return udpRelayCryptoTicket;
	}

	@GET
	@Secured
	@Path("/cryptoticket")
	@Produces(MediaType.APPLICATION_XML)
	public String cryptoticket(@HeaderParam("securityToken") String securityToken) {
		byte[] randomUUIDBytes = UUIDGen.getRandomUUIDBytes();
		String ticketIV = Base64.getEncoder().encodeToString(randomUUIDBytes);
		udpClient.sendFreeroamUdpKey(randomUUIDBytes);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<ClientServerCryptoTicket>\n");
		stringBuilder.append("<CryptoTicket>");
		stringBuilder.append(tokenBO.getCryptoTicket(securityToken));
		stringBuilder.append("</CryptoTicket>\n");
		stringBuilder.append("<SessionKey>AAAAAAAAAAAAAAAAAAAAAA==</SessionKey>\n");
		stringBuilder.append("<TicketIv>");
		stringBuilder.append(ticketIV);
		stringBuilder.append("</TicketIv>\n");
		stringBuilder.append("</ClientServerCryptoTicket>");
		return stringBuilder.toString();
	}
}
