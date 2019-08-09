package com.soapboxrace.core.bo;

import com.soapboxrace.core.dao.AchievementDAO;
import com.soapboxrace.core.dao.EventDataDAO;
import com.soapboxrace.core.dao.EventSessionDAO;
import com.soapboxrace.core.dao.PersonaDAO;
import com.soapboxrace.core.jpa.AchievementDefinitionEntity;
import com.soapboxrace.core.jpa.EventDataEntity;
import com.soapboxrace.core.jpa.EventSessionEntity;
import com.soapboxrace.core.jpa.PersonaEntity;
import com.soapboxrace.core.xmpp.OpenFireSoapBoxCli;
import com.soapboxrace.core.xmpp.XmppEvent;
import com.soapboxrace.jaxb.http.*;
import com.soapboxrace.jaxb.xmpp.XMPP_ResponseTypeRouteEntrantResult;
import com.soapboxrace.jaxb.xmpp.XMPP_RouteEntrantResultType;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class EventResultRouteBO {

	@EJB
	private EventSessionDAO eventSessionDao;

	@EJB
	private EventDataDAO eventDataDao;
	
	@EJB
	private AchievementDAO achievementDAO;
	
	@EJB
	private PersonaDAO personaDAO;

	@EJB
	private AchievementsBO achievementsBO;
	
	@EJB
	private OpenFireSoapBoxCli openFireSoapBoxCli;

	@EJB
	private RewardRouteBO rewardRouteBO;

	@EJB
	private CarDamageBO carDamageBO;
	

	public RouteEventResult handleRaceEnd(EventSessionEntity eventSessionEntity, Long activePersonaId, RouteArbitrationPacket routeArbitrationPacket) {
		Long eventSessionId = eventSessionEntity.getId();
		eventSessionEntity.setEnded(System.currentTimeMillis());

		eventSessionDao.update(eventSessionEntity);

		EventDataEntity eventDataEntity = eventDataDao.findByPersonaAndEventSessionId(activePersonaId, eventSessionId);
		updateEventDataEntity(eventDataEntity, routeArbitrationPacket);

		// RouteArbitrationPacket
		eventDataEntity.setBestLapDurationInMilliseconds(routeArbitrationPacket.getBestLapDurationInMilliseconds());
		eventDataEntity.setFractionCompleted(routeArbitrationPacket.getFractionCompleted());
		eventDataEntity.setLongestJumpDurationInMilliseconds(routeArbitrationPacket.getLongestJumpDurationInMilliseconds());
		eventDataEntity.setNumberOfCollisions(routeArbitrationPacket.getNumberOfCollisions());
		eventDataEntity.setPerfectStart(routeArbitrationPacket.getPerfectStart());
		eventDataEntity.setSumOfJumpsDurationInMilliseconds(routeArbitrationPacket.getSumOfJumpsDurationInMilliseconds());
		eventDataEntity.setTopSpeed(routeArbitrationPacket.getTopSpeed());

		eventDataEntity.setEventModeId(eventDataEntity.getEvent().getEventModeId());
		eventDataEntity.setPersonaId(activePersonaId);
		eventDataDao.update(eventDataEntity);

		ArrayOfRouteEntrantResult arrayOfRouteEntrantResult = new ArrayOfRouteEntrantResult();
		for (EventDataEntity racer : eventDataDao.getRacers(eventSessionId)) {
			RouteEntrantResult routeEntrantResult = new RouteEntrantResult();
			routeEntrantResult.setBestLapDurationInMilliseconds(racer.getBestLapDurationInMilliseconds());
			routeEntrantResult.setEventDurationInMilliseconds(racer.getEventDurationInMilliseconds());
			routeEntrantResult.setEventSessionId(eventSessionId);
			routeEntrantResult.setFinishReason(racer.getFinishReason());
			routeEntrantResult.setPersonaId(racer.getPersonaId());
			routeEntrantResult.setRanking(racer.getRank());
			routeEntrantResult.setTopSpeed(racer.getTopSpeed());
			arrayOfRouteEntrantResult.getRouteEntrantResult().add(routeEntrantResult);
		}

		RouteEventResult routeEventResult = new RouteEventResult();
		routeEventResult.setAccolades(rewardRouteBO.getRouteAccolades(activePersonaId, routeArbitrationPacket, eventSessionEntity));
		routeEventResult.setDurability(carDamageBO.updateDamageCar(activePersonaId, routeArbitrationPacket, routeArbitrationPacket.getNumberOfCollisions()));
		routeEventResult.setEntrants(arrayOfRouteEntrantResult);
		routeEventResult.setEventId(eventDataEntity.getEvent().getId());
		routeEventResult.setEventSessionId(eventSessionId);
		routeEventResult.setExitPath(ExitPath.EXIT_TO_FREEROAM);
		routeEventResult.setInviteLifetimeInMilliseconds(0);
		routeEventResult.setLobbyInviteId(0);
		routeEventResult.setPersonaId(activePersonaId);
		sendXmppPacket(eventSessionId, activePersonaId, routeArbitrationPacket);
		
		// Achievements
		if (routeArbitrationPacket.getRank() == 1)
        {
            switch (eventDataEntity.getEvent().getCarClassHash())
            {
                case -405837480: { // A class
                    achievementsBO.update(personaDAO.findById(activePersonaId), achievementDAO.findByName("achievement_ACH_WIN_RACES_ACLASS"), 1L);
                    break;
                }
                case -406473455: { // B class
                    achievementsBO.update(personaDAO.findById(activePersonaId), achievementDAO.findByName("achievement_ACH_WIN_RACES_BCLASS"), 1L);
                    break;
                }
                case 1866825865: { // C class
                    achievementsBO.update(personaDAO.findById(activePersonaId), achievementDAO.findByName("achievement_ACH_WIN_RACES_CCLASS"), 1L);
                    break;
                }
                case 415909161: { // D class
                    achievementsBO.update(personaDAO.findById(activePersonaId), achievementDAO.findByName("achievement_ACH_WIN_RACES_DCLASS"), 1L);
                    break;
                }
                case 872416321: { // E class
                    achievementsBO.update(personaDAO.findById(activePersonaId), achievementDAO.findByName("achievement_ACH_WIN_RACES_ECLASS"), 1L);
                    break;
                }
                case -2142411446: { // S class
                    achievementsBO.update(personaDAO.findById(activePersonaId), achievementDAO.findByName("achievement_ACH_WIN_RACES_SCLASS"), 1L);
                    break;
                }
                default:break;
            }
        }

        PersonaEntity persona = personaDAO.findById(activePersonaId);

        achievementsBO.update(persona,
                achievementDAO.findByName("achievement_ACH_CLOCKED_AIRTIME"), 
                routeArbitrationPacket.getSumOfJumpsDurationInMilliseconds());

		AchievementDefinitionEntity achievement1 = achievementDAO.findByName("achievement_ACH_DRIVE_MILES");
		Float distance = eventDataEntity.getEvent().getRanksDistance();
		if (achievement1 != null && distance != null) {
			achievementsBO.update(persona, achievement1, (long) (distance * 1000f));
		}
		
		return routeEventResult;
	}

	private void updateEventDataEntity(EventDataEntity eventDataEntity, ArbitrationPacket arbitrationPacket) {
		eventDataEntity.setAlternateEventDurationInMilliseconds(arbitrationPacket.getAlternateEventDurationInMilliseconds());
		eventDataEntity.setCarId(arbitrationPacket.getCarId());
		eventDataEntity.setEventDurationInMilliseconds(arbitrationPacket.getEventDurationInMilliseconds());
		eventDataEntity.setFinishReason(arbitrationPacket.getFinishReason());
		eventDataEntity.setHacksDetected(arbitrationPacket.getHacksDetected());
		eventDataEntity.setRank(arbitrationPacket.getRank());
	}

	private void sendXmppPacket(Long eventSessionId, Long activePersonaId, RouteArbitrationPacket routeArbitrationPacket) {
		XMPP_RouteEntrantResultType xmppRouteResult = new XMPP_RouteEntrantResultType();
		xmppRouteResult.setBestLapDurationInMilliseconds(routeArbitrationPacket.getBestLapDurationInMilliseconds());
		xmppRouteResult.setEventDurationInMilliseconds(routeArbitrationPacket.getEventDurationInMilliseconds());
		xmppRouteResult.setEventSessionId(eventSessionId);
		xmppRouteResult.setFinishReason(routeArbitrationPacket.getFinishReason());
		xmppRouteResult.setPersonaId(activePersonaId);
		xmppRouteResult.setRanking(routeArbitrationPacket.getRank());
		xmppRouteResult.setTopSpeed(routeArbitrationPacket.getTopSpeed());

		XMPP_ResponseTypeRouteEntrantResult routeEntrantResultResponse = new XMPP_ResponseTypeRouteEntrantResult();
		routeEntrantResultResponse.setRouteEntrantResult(xmppRouteResult);

		for (EventDataEntity racer : eventDataDao.getRacers(eventSessionId)) {
			if (!racer.getPersonaId().equals(activePersonaId)) {
				XmppEvent xmppEvent = new XmppEvent(racer.getPersonaId(), openFireSoapBoxCli);
				xmppEvent.sendRaceEnd(routeEntrantResultResponse);
				if (routeArbitrationPacket.getRank() == 1) {
					xmppEvent.sendEventTimingOut(eventSessionId);
				}
			}
		}
	}

}
