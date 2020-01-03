package com.soapboxrace.jaxb.xmpp;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "XMPP_ResponseTypeEventTimedOut", propOrder = { "eventTimedOut" })
@XmlRootElement(name = "response")
public class XMPP_ResponseTypeEventTimedOut {
	@XmlElement(name = "EventTimedOut", required = true)
	protected XMPP_EventTimedOutType eventTimedOut;

	@XmlAttribute(name = "status")
	protected int status = 1;
	@XmlAttribute(name = "ticket")
	protected int ticket = 0;

	public XMPP_EventTimedOutType getEventTimedOut() {
		return eventTimedOut;
	}

	public void setEventTimedOut(XMPP_EventTimedOutType eventTimedOut) {
		this.eventTimedOut = eventTimedOut;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getTicket() {
		return ticket;
	}

	public void setTicket(int ticket) {
		this.ticket = ticket;
	}
}