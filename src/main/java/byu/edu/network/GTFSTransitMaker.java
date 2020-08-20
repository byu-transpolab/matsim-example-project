package byu.edu.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt2matsim.config.PublicTransitMappingConfigGroup;
import org.matsim.pt2matsim.gtfs.GtfsConverter;
import org.matsim.pt2matsim.gtfs.GtfsFeed;
import org.matsim.pt2matsim.gtfs.GtfsFeedImpl;
import org.matsim.pt2matsim.mapping.PTMapper;
import org.matsim.pt2matsim.tools.ScheduleTools;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

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
        Config config = scenario.getConfig();
        PublicTransitMappingConfigGroup ptConfig = ConfigUtils.addOrGetModule(config, PublicTransitMappingConfigGroup.class);
        Set<String> modes = new HashSet<>();
        modes.add("car");
        ptConfig.setModesToKeepOnCleanUp(modes);
        ptConfig.setTravelCostType(PublicTransitMappingConfigGroup.TravelCostType.linkLength);
        ptConfig.setMaxTravelCostFactor(10.0);

        GtfsFeed gtfsFeed = new GtfsFeedImpl(gtfsFolder.toString());

        log.info("Converting GTFS into MATSim schedule");
        GtfsConverter converter = new GtfsConverter(gtfsFeed);
        converter.convert(GtfsConverter.DAY_WITH_MOST_TRIPS, scenario.getConfig().global().getCoordinateSystem());
        schedule = converter.getSchedule();


        log.info("Mapping transit schedule to network");
        PTMapper.mapScheduleToNetwork(schedule, scenario.getNetwork(), ptConfig);
    }

    public void writeTransitOutputFiles(File outDir) {
        log.info("Writing transit files");
        ScheduleTools.writeTransitSchedule(schedule,  outDir.toString() + "/transit_schedule.xml.gz");
        ScheduleTools.writeVehicles(vehicles, outDir.toString() + "/transit_vehicles.xml.gz");
    }
}
