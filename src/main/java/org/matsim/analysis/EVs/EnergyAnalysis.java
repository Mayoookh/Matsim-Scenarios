package org.matsim.analysis.EVs;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.fleet.ElectricFleetReader;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecificationImpl;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerReader;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationImpl;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class EnergyAnalysis implements LinkEnterEventHandler, VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler, ActivityStartEventHandler {

	Map<Id<Vehicle>, EnergyState> energyStates;
	Network network;
	ChargingInfrastructureSpecification chargers;
	Map<Id<Person>, Id<Vehicle>> lastUsedVehicle = new HashMap<>();

	public EnergyAnalysis(Map<Id<Vehicle>, EnergyState> energyStates, Network network, ChargingInfrastructureSpecification chargers) {
		this.energyStates = energyStates;
		this.network = network;
		this.chargers = chargers;
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		EnergyState state = this.energyStates.get(event.getVehicleId());
		if (state == null) {
			return;
		}
		Link link = network.getLinks().get(event.getLinkId());
		state.distanceTravelled(link);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		EnergyState state = this.energyStates.get(event.getVehicleId());
		if (state == null) {
			return;
		}
		state.depart(event.getTime());

	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		EnergyState state = this.energyStates.get(event.getVehicleId());
		if (state == null) {
			return;
		}
		ChargerSpecification chargerOnLink = null;
		for (ChargerSpecification charger : chargers.getChargerSpecifications().values()) {
			if (charger.getLinkId().equals(event.getLinkId())) {
				chargerOnLink = charger;
				break;
			}
		}

		if (chargerOnLink == null) {
			return;
		}
		lastUsedVehicle.put(event.getPersonId(), event.getVehicleId());
		Link link = network.getLinks().get(event.getLinkId());
		state.parkOnLink(link, chargerOnLink, event.getTime());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id<Vehicle> vehicleId = lastUsedVehicle.get(event.getPersonId());
		if (vehicleId == null) {
			return;
		}
		EnergyState state = this.energyStates.get(vehicleId);
		if (state == null) {
			return;
		}
		state.actType = event.getActType();
	}

	@Override
	public void reset(int iteration) {

	}

	public void writeEndState(String filename) {
		try (BufferedWriter writer =IOUtils.getBufferedWriter(filename)) {
			writer.write("VehicleId,Distance,FinalCharge\n");
			for (Id<Vehicle> vehicleId : this.energyStates.keySet()) {
				EnergyState state = this.energyStates.get(vehicleId);
				writer.write(vehicleId.toString());
				writer.write(",");
				writer.write(Double.toString(state.travelledDistance / 1000.0));
				writer.write(",");
				writer.write(Double.toString(state.charge));
				writer.write("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeCharingProcesses(String filename) {
		try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
			writer.write("VehicleId,LinkId,ActType,Charge,StartTime\n");
			for (Id<Vehicle> vehicleId : this.energyStates.keySet()) {
				EnergyState state = this.energyStates.get(vehicleId);
				for (ChargingProcess process : state.chargings) {
					writer.write(vehicleId.toString());
					writer.write(",");
					writer.write(process.linkId.toString());
					writer.write(",");
					writer.write(process.actType);
					writer.write(",");
					writer.write(Double.toString(process.charge));
					writer.write(",");
					writer.write(Time.writeTime(process.startTime));
					writer.write("\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile("data/network.xml.gz");

		ChargingInfrastructureSpecification allChargers = new ChargingInfrastructureSpecificationImpl();
		new ChargerReader(allChargers).readFile("/data/coding/tutorialEifer/output/chargers.xml");

		ElectricFleetSpecification fleet = new ElectricFleetSpecificationImpl();
		new ElectricFleetReader(fleet).readFile("/data/coding/tutorialEifer/output/electricVehicles.xml");

		Map<Id<Vehicle>, EnergyState> energyStates = new HashMap<>();
		for (Id<ElectricVehicle> evId : fleet.getVehicleSpecifications().keySet()) {
			ElectricVehicleSpecification ev = fleet.getVehicleSpecifications().get(evId);

			Id<Vehicle> vehicleId = Id.create(evId, Vehicle.class);
			EnergyState state = new EnergyState(ev.getBatteryCapacity() / 1000 / 3600, ev.getInitialSoc() / 1000 / 3600);
			energyStates.put(vehicleId, state);
		}

		EnergyAnalysis analysis = new EnergyAnalysis(energyStates, network, allChargers);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(analysis);
		new MatsimEventsReader(events).readFile("/data/coding/tutorial/output/output_events.xml.gz");

		analysis.writeEndState("/data/coding/tutorialEifer/output/evStates.csv");
		analysis.writeCharingProcesses("/data/coding/tutorialEifer/output/chargings.csv");

	}

}
