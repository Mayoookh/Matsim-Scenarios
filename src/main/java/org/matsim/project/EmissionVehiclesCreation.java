
package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Random;

/**
 * @author Sikka
 *
 */

public class EmissionVehiclesCreation {
     public void run(String inputPopulationFilename,String inputTransitVehiclesFilename, String outputVehiclesFilename){

     //creates vehicles and assign them to population and transit vehicle

     Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
     Vehicles vehicles = scenario.getVehicles();
     VehiclesFactory factory = vehicles.getFactory();

     LinkedList<VehicleType> VT = new LinkedList();

    String [] vehCat = new String[]{"PASSENGER_CAR","HEAVY_GOODS_VEHICLE"};

    String [] tech = new String[]{"petrol (4S)","diesel"};

    LinkedHashMap <String, String> vehSize = new LinkedHashMap<String, String>()
        {{
            put("small","<1,4L");
            put("medium","1,4L-<2L");
            put("large","≥2L");
        }};

    LinkedHashMap <String, String> euroNormPetrol = new LinkedHashMap<String, String>()
        {{
          put("euro1","PC P Euro-1");
          put("euro2","PC P Euro-2");
          put("euro3","PC P Euro-3");
          put("euro4","PC P Euro-4");
          put("euro5","PC P Euro-5");
          put("euro6","PC P Euro-6");

        }};

    LinkedHashMap <String, String> euroNormDiesel = new LinkedHashMap<String, String>()
         {{
        put("euro1","PC D Euro-1");
        put("euro2","PC D Euro-2");
        put("euro3","PC D Euro-3");
        put("euro4","PC D Euro-4");
        put("euro5","PC D Euro-5");
        put("euro6","PC D Euro-6");
         }};

    for (String vehSizeKey : vehSize.keySet()) {
            for (String euroNormPetrolKey : euroNormPetrol.keySet()) {

                VehicleType vt = factory.createVehicleType(Id.create(tech[0] + "_" + vehSizeKey + "_" + euroNormPetrolKey, VehicleType.class));
                vt.setDescription("BEGIN_EMISSIONS" + vehCat[0] + ";" + tech[0] + ";" + vehSize.get(vehSizeKey) + ";" + euroNormPetrol.get(euroNormPetrolKey) + "END_EMISSIONS");
                VT.add(vt);
                vehicles.addVehicleType(vt);
                System.out.println(vt.getDescription());
            }

            for (String euroNormDieselKey : euroNormDiesel.keySet()) {

                VehicleType vt = factory.createVehicleType(Id.create(tech[1] + "_" + vehSizeKey + "_" + euroNormDieselKey, VehicleType.class));
                vt.setDescription("BEGIN_EMISSIONS" + vehCat[0] + ";" + tech[1] + ";" + vehSize.get(vehSizeKey) + ";" + euroNormDiesel.get(euroNormDieselKey) + "END_EMISSIONS");
                VT.add(vt);
                vehicles.addVehicleType(vt);
                System.out.println(vt.getDescription());

        }}

    VehicleType Bus =factory.createVehicleType(Id.create("Bus",VehicleType.class));
    Bus.setDescription("BEGIN_EMISSIONSHEAVY_GOODS_VEHICLE;average;average;averageEND_EMISSIONS");
    vehicles.addVehicleType(Bus);

    VehicleType Subway =factory.createVehicleType(Id.create("Subway",VehicleType.class));
    Subway.setDescription("BEGIN_EMISSIONSZERO_EMISSION_VEHICLE;average;average;averageEND_EMISSIONS");
    vehicles.addVehicleType(Subway);

    VehicleType Tram =factory.createVehicleType(Id.create("Tram",VehicleType.class));
    Tram.setDescription("BEGIN_EMISSIONSZERO_EMISSION_VEHICLE;average;average;averageEND_EMISSIONS");
    vehicles.addVehicleType(Tram);

    // ASSIGNING TO POPULATION

     new PopulationReader(scenario).readFile(inputPopulationFilename);
     Random random = new Random(12345);
     double typeProbability[] = {0.95,2.58,5.36,10.00,16.09,25.91,25.92,25.93,26.26,28.18,31.41,35.44,
                                35.6,36.31,37.71,38.95,40.15,42.05,42.45,43.53,47.07,57.87,75.76,94.71,
                                94.73,94.79,94.93,95.05,95.13,95.21,95.31,95.52,96.16,97.48,98.7,100};


     for (Person person : scenario.getPopulation().getPersons().values()) {

          Id<Vehicle>  vehicleId = Id.create(person.getId(), Vehicle.class);
          double x = random.nextDouble() * 100;

          VehicleType vehicleType = VT.get(0);
          for (int i = 0; i < typeProbability.length; i++) {
             if  (x <= typeProbability[i]) {
                 vehicleType = VT.get(i);
                 i = 0;
                 break;
             }
         }

     Vehicle vehicle = factory.createVehicle(vehicleId, vehicleType);
            vehicles.addVehicle(vehicle);
     }

     // // ASSIGNING TO TRANSIT

     new VehicleReaderV1(scenario.getTransitVehicles()).readFile(inputTransitVehiclesFilename);

     for (Vehicle ptVehicle : scenario.getTransitVehicles().getVehicles().values()) {
         Id<Vehicle>  vehicleId = Id.create(ptVehicle.getId(), Vehicle.class);
         VehicleType vehicleType = Bus;

         if (ptVehicle.getType().getId().toString().equals("Bus"))
             vehicleType = Bus;
         else if (ptVehicle.getType().getId().toString().equals("Subway"))
                 vehicleType = Subway;
         else if (ptVehicle.getType().getId().toString().equals("Tram"))
             vehicleType = Tram;

         Vehicle vehicle = factory.createVehicle(vehicleId, vehicleType);
         vehicles.addVehicle(vehicle);
     }

     new VehicleWriterV1(vehicles).writeFile(outputVehiclesFilename);

    }

    public void addRoadTypes(String inputNetworkFilename, String outputNetworkFilename){

         //assign link attributes as per emission module requirement

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(network).readFile(inputNetworkFilename);

        //bounding box of urban area:
        double minX = 545686.26325705;
        double maxX = 589503.69384617;
        double minY = 6265436.8074908;
        double maxY = 6297837.99362724;

        for (Link link : network.getLinks().values()){
            //link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
            Double freespeed = link.getFreespeed();
            Object highway = link.getAttributes().getAttribute("osm:way:highway");

            double linkX =link.getCoord().getX();
            double linkY =link.getCoord().getY();
            String areaType = "URB";
            if (linkX < minX || linkX > maxX || linkY < minY || linkY > maxY){
                areaType = "RUR";

            }


            //https://github.com/matsim-org/josm-matsim-plugin/blob/master/src/main/java/org/matsim/contrib/josm/model/LinkConversionRules.java
           // if (highway != null && freespeed != null) {
                if ("motorway".equals(highway) || "motorway_link".equals(highway)) {
                    if (areaType.equals("URB")){
                        link.getAttributes().putAttribute("hbefa_road_type", String.format("%s/%d", areaType.concat("/MW-Nat."), 110));
                    }
                    else{
                        link.getAttributes().putAttribute("hbefa_road_type", String.format("%s/%d", areaType.concat("/MW"), 100));
                    }
                }
                else if ("primary".equals(highway) || "primary_link".equals(highway) || "trunk".equals(highway) || "trunk_link".equals(highway)) {
                    if (areaType.equals("URB")){
                        link.getAttributes().putAttribute("hbefa_road_type", String.format("%s/%d", areaType.concat("/Trunk-Nat."), 90));
                    } else{
                        link.getAttributes().putAttribute("hbefa_road_type", String.format("%s/%d", areaType.concat("/Trunk"), 80));
                    }
                }
                else if ("secondary".equals(highway) || "secondary_link".equals(highway)) {
                    link.getAttributes().putAttribute("hbefa_road_type", String.format("%s/%d", areaType.concat("/Distr"), 70));
                }
                else if ("tertiary".equals(highway) || "tertiary_link".equals(highway)) {
                    link.getAttributes().putAttribute("hbefa_road_type", String.format("%s/%d", areaType.concat("/Local"), 50));
                }
                else {
                    link.getAttributes().putAttribute("hbefa_road_type", String.format("%s/%d", areaType.concat("/Access"), 40));
                }
            }
        //}
        new NetworkWriter(network).write(outputNetworkFilename);
    }

    public static void main(String[] args) {
        new EmissionVehiclesCreation().run("data/population.xml","data/01_transitVehicles.xml.gz","data/vehiclesWithEmission.xml" );
        new EmissionVehiclesCreation().addRoadTypes("data/02_ptnetwork_mapped.xml.gz", "data/03_emission_ptnetwork_mapped.xml.gz");
    }
}
