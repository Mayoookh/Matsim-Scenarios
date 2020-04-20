package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WaitingTimeInTraffic implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {
    Map<Id<Vehicle>,Double> lookupTable = new HashMap<>();
    Network network;
    Map<Id<Link>,Double> results = new HashMap<>();

    WaitingTimeInTraffic(Network network){
        this.network = network;
    }

    @Override
    public void handleEvent(LinkEnterEvent event){
        lookupTable.put(event.getVehicleId(),event.getTime());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event){
        Double linkEnteredTime = lookupTable.get(event.getVehicleId());
        if (linkEnteredTime == null){
            return;
        }
        double linkTravelTime = event.getTime() - linkEnteredTime;
        Link link = network.getLinks().get(event.getLinkId());
        double freespeedTravelTime = link.getLength() / link.getFreespeed();
        double waitingTime = linkTravelTime - freespeedTravelTime;

        //upper-case Double -> class instead of primitive -> can be null
        Double sum = results.get(link.getId());
        if (sum == null){
            results.put(link.getId(), waitingTime);
        } else {
            results.put(link.getId(), waitingTime + sum );
        }
        //or
        //result.merge(link.getId(), waitingTime, (oldVal, val) -> oldVal + val);

    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event){
        lookupTable.remove(event.getVehicleId());
    }

    public void writeResults(String filename){
        BufferedWriter writer = IOUtils.getBufferedWriter(filename);
        try {
            writer.write("LinkId,WaitingTime\n");
            for (Id<Link> linkId: results.keySet()){
                writer.write(linkId.toString());
                writer.write(",");
                writer.write(results.get(linkId).toString());
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("C:/localStorage/toulouse/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        EventsManager events = EventsUtils.createEventsManager();
        WaitingTimeInTraffic dataCollector = new WaitingTimeInTraffic(scenario.getNetwork());

        events.addHandler(dataCollector);
        new MatsimEventsReader(events).readFile("output/output_events.xml.gz");
        dataCollector.writeResults("C:/localStorage/toulouse/WaitingTimeInTraffic.csv");
    }

}
