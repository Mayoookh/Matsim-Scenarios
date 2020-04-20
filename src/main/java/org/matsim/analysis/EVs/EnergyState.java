package org.matsim.analysis.EVs;


import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;


import java.util.ArrayList;
import java.util.List;


public class EnergyState {

	double travelledDistance = 0;
	double maxCharge;
	double charge; // kiloWatt
	boolean isCharging = false;
	double chargingStartTime;
	ChargerSpecification charger;
	String actType;

	List<ChargingProcess> chargings = new ArrayList<>();

	public EnergyState(double maxCharge, double charge) {
		this.maxCharge = maxCharge;
		this.charge = charge;
	}

	public void distanceTravelled(Link link) {
		double length = link.getLength(); // in meters
		double usedCharge = (length / 1000) * 0.1; // 10 kWh/100 km --> 0.1 kWh / km
		this.charge = this.charge - usedCharge;
		travelledDistance += length;
	}

	public void parkOnLink(Link link, ChargerSpecification charger, double time) {
		if (charge < 0.1 * maxCharge) { // assumption : a person charges if battery is less than 10 %
			// assume a plug is always available
			isCharging = true;
			chargingStartTime = time;
			this.charger = charger;
		}
	}

	public void depart(double time) {
		if (!isCharging) {
			return;
		}

		isCharging = false;
		double chargingTime = time - chargingStartTime;
		double potentialCharge = (chargingTime / 3600) * 22; // assumption: 22 kW / h

		double finalCharge = Math.min(maxCharge, charge + potentialCharge);
		double actualCharge = finalCharge - charge;
		charge = finalCharge;

		ChargingProcess process = new ChargingProcess(charger.getLinkId(), charger.getId(), chargingStartTime, actType, actualCharge);
		chargings.add(process);

	}
}
