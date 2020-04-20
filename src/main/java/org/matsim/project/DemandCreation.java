package org.matsim.project;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import java.util.Random;

public class DemandCreation {
    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        Random r = new Random(123456);
        for (int i = 0; i < 100;i++){

            String mode = "car";
            if(r.nextDouble() < 0.4){
                mode = "pt";
            }
            //create person
            Person person = factory.createPerson(Id.createPersonId(i));
            population.addPerson(person);

            //create plan for person
            Plan plan = factory.createPlan();
            person.addPlan(plan);
            //create Activities and Legg
            Activity homeActivity = factory.createActivityFromCoord("home", new Coord(566_424 + r.nextInt(1000), 6_280_000 + r.nextInt(1000)));
            homeActivity.setEndTime(25200 + r.nextInt(1800));
            //homeActivity.setEndTime(Time.parseTime("7:00:00"));
            plan.addActivity(homeActivity);

            Leg leg1 = factory.createLeg(mode);
            plan.addLeg(leg1);

            Activity workActivity = factory.createActivityFromCoord("work", new Coord(574_960 + r.nextInt(1000), 6_278_000+r.nextInt(1000)));
            workActivity.setEndTime(17*3600 + r.nextInt(1800));
            plan.addActivity(workActivity);

            Leg leg2 = factory.createLeg(mode);
            plan.addLeg(leg2);

            Activity finalhomeActivity = factory.createActivityFromCoord("home", homeActivity.getCoord());
            plan.addActivity(finalhomeActivity);

        }




        new PopulationWriter(population).write("C:/localStorage/toulouse/population.xml");

    }
}
