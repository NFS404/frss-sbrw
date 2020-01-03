package com.soapboxrace.core.xmpp;

import com.soapboxrace.jaxb.xmpp.XMPP_EventTimedOutType;
import com.soapboxrace.jaxb.xmpp.XMPP_ResponseTypeEventTimedOut;

import javax.annotation.Resource;
import javax.ejb.*;
import java.io.Serializable;

@Singleton
public class DNFTimerManager {
    @Resource
    private TimerService timerService;

    @EJB
    private OpenFireSoapBoxCli soapBoxCli;

    public void schedule(Long eventSessionId, Long persona) {
        TimerConfig config = new TimerConfig();
        config.setInfo(new TimerInfo(eventSessionId, persona));
        timerService.createSingleActionTimer(60000, config);
    }

    @Timeout
    public void timeout(Timer timer) {
        TimerInfo info = (TimerInfo) timer.getInfo();
        XMPP_EventTimedOutType eventTimedOut = new XMPP_EventTimedOutType();
        eventTimedOut.setEventSessionId(info.eventSessionId);
        XMPP_ResponseTypeEventTimedOut eventTimedOutResponse = new XMPP_ResponseTypeEventTimedOut();
        eventTimedOutResponse.setEventTimedOut(eventTimedOut);
        soapBoxCli.send(eventTimedOutResponse, info.persona);
    }

    private static class TimerInfo implements Serializable {
        Long eventSessionId;
        Long persona;

        TimerInfo(Long eventSessionId, Long persona) {
            this.eventSessionId = eventSessionId;
            this.persona = persona;
        }
    }
}
