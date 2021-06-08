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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
public class RunMatsim{

	public static void main(String[] args) {

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios/provo_orem/config.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );

		// possibly modify config here
		// set network
		config.network().setInputFile("highway_network.xml.gz");
		// set plans
		config.plans().setInputFile("final_population.xml");

		// set number of iterations
		config.controler().setLastIteration(9);

		// set output folder if necessary
		// config.controler().setOutputDirectory();
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// possibly modify scenario here
		// network population and plans
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		// possibly modify controler here

		// controler.addOverridingModule( new OTFVisLiveModule() ) ;
		
		// ---
		
		controler.run();


	}

	
}
