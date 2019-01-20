package com.soapboxrace.core.jpa;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "TOKEN_SESSION")
@NamedQueries({ //
		@NamedQuery(name = "TokenSessionEntity.findByUserId", query = "SELECT obj FROM TokenSessionEntity obj WHERE obj.userId = :userId"), //
		@NamedQuery(name = "TokenSessionEntity.deleteByUserId", query = "DELETE FROM TokenSessionEntity obj WHERE obj.userId = :userId"), //
		@NamedQuery(name = "TokenSessionEntity.updateRelayCrytoTicket", //
				query = "UPDATE TokenSessionEntity obj " //
						+ "SET obj.relayCryptoTicket = :relayCryptoTicket WHERE obj.activePersonaId = :personaId"), //
		@NamedQuery(name = "TokenSessionEntity.updateLobbyId", //
				query = "UPDATE TokenSessionEntity obj " //
						+ "SET obj.activeLobbyId = :activeLobbyId WHERE obj.activePersonaId = :personaId") //
})
public class TokenSessionEntity {

	@Id
	@Column(name = "ID", nullable = false)
	private String securityToken;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Date expirationDate;

	private Long activePersonaId;

	private String relayCryptoTicket;

	private Long activeLobbyId;

	private boolean premium = false;

	private String clientHostIp;

	private String cryptoTicket;

	public String getSecurityToken() {
		return securityToken;
	}

	public void setSecurityToken(String securityToken) {
		this.securityToken = securityToken;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Date getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	public Long getActivePersonaId() {
		return activePersonaId;
	}

	public void setActivePersonaId(Long activePersonaId) {
		this.activePersonaId = activePersonaId;
	}

	public String getRelayCryptoTicket() {
		return relayCryptoTicket;
	}

	public void setRelayCryptoTicket(String relayCryptoTicket) {
		this.relayCryptoTicket = relayCryptoTicket;
	}

	public Long getActiveLobbyId() {
		return activeLobbyId;
	}

	public void setActiveLobbyId(Long activeLobbyId) {
		this.activeLobbyId = activeLobbyId;
	}

	public boolean isPremium() {
		return premium;
	}

	public void setPremium(boolean premium) {
		this.premium = premium;
	}

	public String getClientHostIp() {
		return clientHostIp;
	}

	public void setClientHostIp(String clientHostIp) {
		this.clientHostIp = clientHostIp;
	}

	public String getCryptoTicket() {
		return cryptoTicket;
	}

	public void setCryptoTicket(String cryptoTicket) {
		this.cryptoTicket = cryptoTicket;
	}
}
