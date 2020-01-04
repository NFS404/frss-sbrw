package com.soapboxrace.core.bo;

import com.soapboxrace.core.jpa.EventSessionEntity;
import com.soapboxrace.jaxb.http.*;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class LegitRaceBO {

	@EJB
	private ParameterBO parameterBO;

	@EJB
	private SocialBO socialBo;

	public boolean isLegit(Long activePersonaId, ArbitrationPacket arbitrationPacket, EventSessionEntity sessionEntity) {
		int minimumTime = 0;

		if (arbitrationPacket instanceof PursuitArbitrationPacket)
			minimumTime = parameterBO.getIntParam("PURSUIT_MINIMUM_TIME");
		else if (arbitrationPacket instanceof RouteArbitrationPacket)
			minimumTime = parameterBO.getIntParam("ROUTE_MINIMUM_TIME");
		else if (arbitrationPacket instanceof TeamEscapeArbitrationPacket)
			minimumTime = parameterBO.getIntParam("TE_MINIMUM_TIME");
		else if (arbitrationPacket instanceof DragArbitrationPacket)
			minimumTime = parameterBO.getIntParam("DRAG_MINIMUM_TIME");

		final long timeDiff = sessionEntity.getEnded() - sessionEntity.getStarted();
		boolean legit = timeDiff > minimumTime + 1;

		if (!legit) {
			socialBo.sendReport(0L, activePersonaId, 3, String.format("Abnormal event time: %d", timeDiff), (int) arbitrationPacket.getCarId(), 0, arbitrationPacket.getHacksDetected());
		}
		return legit;
	}
}
