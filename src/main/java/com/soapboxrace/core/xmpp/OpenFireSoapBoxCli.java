package com.soapboxrace.core.xmpp;

import com.soapboxrace.jaxb.util.MarshalXML;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class OpenFireSoapBoxCli {

	@EJB
	private OpenFireRestApiCli restApi;

	public void send(String msg, Long to) {
		restApi.sendMessage(to, msg);
	}

	public void send(Object object, Long to) {
		String responseXmlStr = MarshalXML.marshal(object);
		this.send(responseXmlStr, to);
	}

}