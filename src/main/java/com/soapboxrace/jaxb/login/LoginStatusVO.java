package com.soapboxrace.jaxb.login;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LoginStatusVO")
public class LoginStatusVO {

	@XmlElement(name = "UserId")
	private Long userId;
	@XmlElement(name = "LoginToken")
	private String loginToken;
	@XmlElement(name = "Description")
	private String description;
	@XmlElement(name = "Ban")
	private Ban ban;
	@XmlElement(name = "Warning")
	private String warning;

	@XmlTransient
	private boolean loginOk;

	public LoginStatusVO() {
	}

	public LoginStatusVO(Long userId, String loginToken, boolean loginOk) {
		this.userId = userId;
		this.loginToken = loginToken;
		this.loginOk = loginOk;
	}

	public Long getUserId() {
		return userId;
	}

	public String getLoginToken() {
		return loginToken;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Ban getBan() {
		return ban;
	}

	public void setBan(Ban ban) {
		this.ban = ban;
	}

	public boolean isLoginOk() {
		return loginOk;
	}

	public String getWarning() {
		return warning;
	}

	public void setWarning(String warning) {
		this.warning = warning;
	}

	@XmlRootElement
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Ban {
		@XmlElement(name = "Reason")
		private String reason;

		@XmlElement(name = "Expires")
		private String expires;

		public Ban() {
		}

		public Ban(String reason, String expires) {
			this.reason = reason;
			this.expires = expires;
		}

		public String getReason() {
			return reason;
		}

		public void setReason(String reason) {
			this.reason = reason;
		}

		public String getExpires() {
			return expires;
		}

		public void setExpires(String expires) {
			this.expires = expires;
		}
	}
}
