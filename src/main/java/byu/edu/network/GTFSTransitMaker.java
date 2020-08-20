package byu.edu.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt2matsim.tools.ScheduleTools;
import org.matsim.vehicles.Vehicles;

import java.io.File;

public class GTFSTransitMaker {
    private static final Logger log = Logger.getLogger(GTFSTransitMaker.class);

    private Scenario scenario;
    private TransitSchedule schedule;
    private Vehicles vehicles;

    public GTFSTransitMaker(Scenario scenario) {
        this.scenario = scenario;
        this.schedule = this.scenario.getTransitSchedule();
        this.vehicles = this.scenario.getVehicles();
    }

    public void readGtfsFolder(File gtfsFolder) {

    }

    public void writeTransitOutputFiles(File outDir) {
        log.info("Writing transit files");
        ScheduleTools.writeTransitSchedule(schedule,  outDir.toString() + "/transit_schedule.xml.gz");
        ScheduleTools.writeVehicles(vehicles, outDir.toString() + "/transit_vehicles.xml.gz");
    }
}
