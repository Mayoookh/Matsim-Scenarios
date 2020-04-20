/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.project;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.api.core.v01.*;
import org.matsim.contrib.ev.routing.EvNetworkRoutingProvider;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;

/**
 * @author nagel
 *
 */
public class RunMatsim{

	public static void main(String[] args) {
		if ( args.length==0 ) {
			args = new String [] { "input/Scenarios/with excessive bikes/data/config_compact.xml" } ;
		} else {
			Gbl.assertIf( args[0] != null && !args[0].equals( "" ) );
		}
		//Emission contrib and EV module
		Config config = ConfigUtils.loadConfig(args, new EmissionsConfigGroup() );

		//Config config = ConfigUtils.loadConfig( args ) ;
		
		// possibly modify config here
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// possibly modify scenario here
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		// possibly modify controler here
		// To use the fast pt router (Part 1 of 1)
		controler.addOverridingModule(new SwissRailRaptorModule());

		//Emission contrib
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install(){
				bind(EmissionModule.class).asEagerSingleton();
				}
			});
		//controler.addOverridingModule( new OTFVisLiveModule() ) ;
		
		// ---

		controler.run();
	}
	
}
