package com.soapboxrace.core.bo;

import com.soapboxrace.core.dao.ChatAnnouncementDAO;
import com.soapboxrace.core.jpa.ChatAnnouncementEntity;
import com.soapboxrace.core.xmpp.OpenFireRestApiCli;
import com.soapboxrace.core.xmpp.XmppChat;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import java.util.List;

@Stateless
public class ChatAnnouncementsBO
{
    @EJB
    private ChatAnnouncementDAO chatAnnouncementDAO;

    @EJB
    private OpenFireRestApiCli restApiCli;

    private Long ticks = 0L;

    @Schedule(minute = "*", hour = "*", second = "*/5", persistent = false)
    public void sendMessages()
    {
        ticks += 5;

        List<Long> sessions = null;

        for (ChatAnnouncementEntity announcementEntity : chatAnnouncementDAO.findAll())
        {
            if (announcementEntity.getAnnouncementInterval() % 5 != 0) continue;

            if (ticks % announcementEntity.getAnnouncementInterval() == 0)
            {
                if (sessions == null) {
                    sessions = restApiCli.getOnlinePersonas();
                }

                String message = XmppChat.createSystemMessage(announcementEntity.getAnnouncementMessage());

                for (Long member : sessions) {
                    restApiCli.sendMessage(member, message);
                }
            }
        }
    }
}
