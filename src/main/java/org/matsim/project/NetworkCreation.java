package org.matsim.project;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.pt2matsim.config.PublicTransitMappingConfigGroup;
import org.matsim.pt2matsim.run.Gtfs2TransitSchedule;
import org.matsim.pt2matsim.run.Osm2MultimodalNetwork;
import org.matsim.pt2matsim.run.Osm2TransitSchedule;
import org.matsim.pt2matsim.run.PublicTransitMapper;

import java.util.HashSet;
import java.util.Set;

public class NetworkCreation {
    public static void main(String[] args) {

        String fullNetworkFile = "data/network.xml.gz";
        String unmappedScheduleFile = "data/01_transitSchedule.xml.gz";
        String transitVehiclesFile = "data/01_transitVehicles.xml.gz";
        String mappedScheduleFile = "data/02_transitSchedule.xml.gz";
        String mappedNetworkFile = "data/02_ptnetwork_mapped.xml.gz";
        //String osmFile = "C:/localStorage/toulouse/planet_1.33,43.527_1.645,43.67.osm";
        String osmFile = "data/highways.osm";
        String gtfsDirectory = "data/gtfs";
        String targetCrs = "EPSG:2154";

        // create multimodal network

        Osm2MultimodalNetwork.run(osmFile, fullNetworkFile, targetCrs);

        //clean the network
        Set<String> modes = new HashSet<>();
        modes.add("car");
        modes.add("pt");
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(fullNetworkFile);
        new MultimodalNetworkCleaner(network).run(modes);
        new NetworkWriter(network).write(fullNetworkFile);

        // convert gtfs schedule
        Gtfs2TransitSchedule.run(gtfsDirectory,"dayWithMostServices", targetCrs, unmappedScheduleFile, transitVehiclesFile);

        //Osm2TransitSchedule.run(osmFile, "C:/localStorage/toulouse/OSM_transitSchedule.xml.gz", targetCrs);

        //map schedule and network
        // now comes the mapping of the schedule to the network
        String mappingConfigFile = "01_ptmappingconfig.xml";

        // prepare mapping config, mostly copied from org.matsim.pt2matsim.run.CreateDefaultPTMapperConfig
        Config config = ConfigUtils.createConfig();
        Set<String> toRemove = new HashSet<>(config.getModules().keySet());
        toRemove.forEach(config::removeModule);// basically, remove all existing modules
        PublicTransitMappingConfigGroup group = PublicTransitMappingConfigGroup.createDefaultConfig();
        config.addModule(group);

        // now only add our special module
        group.setInputNetworkFile(fullNetworkFile);
        group.setInputScheduleFile(unmappedScheduleFile);
        group.setOutputScheduleFile(mappedScheduleFile);
        group.setOutputNetworkFile(mappedNetworkFile);
        new ConfigWriter(config).write(mappingConfigFile);

        // do the mapping
        PublicTransitMapper.run(mappingConfigFile);
    }
}
