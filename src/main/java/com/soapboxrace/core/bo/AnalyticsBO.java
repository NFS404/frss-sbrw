package com.soapboxrace.core.bo;

import com.soapboxrace.core.jpa.UserEntity;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

@Singleton
@Startup
public class AnalyticsBO {
    private String analyticsId;
    private Boolean analyticsEnabled;

    @EJB
    private ParameterBO parameterBO;

    @PostConstruct
    public void init() {
        analyticsId = parameterBO.getStrParam("ANALYTICS_ID");
        analyticsEnabled = analyticsId != null;
    }

    private String authActionHit(String action, UserEntity user) {
        StringBuilder body = new StringBuilder("v=1&t=event&ec=Auth");
        body.append("&ea=");
        body.append(action);
        body.append("&tid=");
        body.append(analyticsId);
        body.append("&uid=");
        body.append(user.getId());
        body.append("&uip=");
        body.append(user.getIpAddress());
        body.append("&cd1=");
        body.append(user.getUserAgent());
        return body.toString();
    }

    public void trackUserLogin(UserEntity user) {
        if (!analyticsEnabled) return;

        String body = authActionHit("Login", user);
        ClientBuilder.newClient()
                .target("https://www.google-analytics.com")
                .path("/collect")
                .request()
                .post(Entity.text(body));
    }

    public void trackUserRegister(UserEntity user) {
        if (!analyticsEnabled) return;

        String body = authActionHit("Register", user);
        ClientBuilder.newClient()
                .target("https://www.google-analytics.com")
                .path("/collect")
                .request()
                .post(Entity.text(body));
    }
}
