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

public class tryVehicles {

    public static void main(String[] args) {
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
                vt.setDescription("BEGIN_EMISSIONS" + vehCat[0] + ";" + tech[0] + ";" + vehSize.get(vehSizeKey) + ";" + euroNormPetrol.get(euroNormPetrolKey) + ";" + "END_EMISSIONS");
                VT.add(vt);
                System.out.println(vt.getDescription());
            }

            for (String euroNormDieselKey : euroNormDiesel.keySet()) {

                VehicleType vt = factory.createVehicleType(Id.create(tech[1] + "_" + vehSizeKey + "_" + euroNormDieselKey, VehicleType.class));
                vt.setDescription("BEGIN_EMISSIONS" + vehCat[0] + ";" + tech[1] + ";" + vehSize.get(vehSizeKey) + ";" + euroNormDiesel.get(euroNormDieselKey) + ";" + "END_EMISSIONS");
                VT.add(vt);
                System.out.println(vt.getDescription());
            }

        }



    }
}








