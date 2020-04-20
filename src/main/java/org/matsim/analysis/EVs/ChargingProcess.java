package org.matsim.analysis.EVs;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.infrastructure.Charger;


public class ChargingProcess {

	Id<Link> linkId;
	Id<Charger> chargerId;
	double startTime;
	String actType;
	double charge;

	public ChargingProcess(Id<Link> linkId, Id<Charger> chargerId, double startTime, String actType, double charge) {
		this.linkId = linkId;
		this.chargerId = chargerId;
		this.startTime = startTime;
		this.actType = actType;
		this.charge = charge;
	}
}
