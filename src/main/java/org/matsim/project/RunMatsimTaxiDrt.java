/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareModule;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFaresConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * This class runs an example robotaxi scenario including fares. The simulation runs for 10 iterations, this takes
 * quite a bit time (25 minutes or so). You may switch on OTFVis visualisation in the main method below. The scenario
 * should run out of the box without any additional files. If required, you may find all input files in the resource
 * path or in the jar maven has downloaded). There are two vehicle files: 2000 vehicles and 5000, which may be set in
 * the config. Different fleet sizes can be created using
 * {@link org.matsim.contrib.av.robotaxi.vehicles.CreateTaxiVehicles}
 */
public class RunMatsimTaxiDrt {

	public static void main(String[] args) {
		String configFile = "scenarios/provo_orem/taxi_drt_config.xml";
		RunMatsimTaxiDrt.run(configFile, false);
	}

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new TaxiFaresConfigGroup(),
				new MultiModeTaxiConfigGroup(), new DrtFaresConfigGroup(), new MultiModeDrtConfigGroup(),
				new OTFVisConfigGroup());

		//problem with running both at the same time, either one or the other. Find way to combine
		//createTaxiControler(config, otfvis).run();
		createDrtControler(config,otfvis).run();
	}

	public static Controler createTaxiControler(Config config, boolean otfvis) {
		Controler controler=TaxiControlerCreator.createControler(config, otfvis);
		controler.addOverridingModule(new TaxiFareModule());

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}

	public static Controler createDrtControler(Config config, boolean otfvis) {
		Controler controler = DrtControlerCreator.createControler(config, otfvis);
		controler.addOverridingModule(new DrtFareModule());

		return controler;
	}

}
