package byu.edu.cubeutils;

import com.google.common.io.Files;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.DijkstraWithDijkstraTreeCache;
import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt2matsim.tools.debug.ScheduleCleaner;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.io.*;
import java.util.*;

public class CubeTransitReader {
    private static final Logger log = Logger.getLogger(CubeTransitReader.class);


    private Scenario sc;
    private Network network;
    private DijkstraWithDijkstraTreeCache dijkstraTree = null;
    private NetworkFactory nf;
    private TransitSchedule schedule;
    private Vehicles vehiclesContainer;
    private TransitScheduleFactory factory;

    private Map<String, CubeLine> cubeLineHashMap = new HashMap<>();

    private final boolean BLOCKS_DEFAULT = false;
    private final boolean AWAIT_DEPARTURE_TIME_DEFAULT = true;

    HashMap<String, HashMap<Integer, Integer>> periods = new HashMap<>();

    /**
     * Class constructor
     * @param scenario
     */
    public CubeTransitReader(Scenario scenario) {
        this.sc = scenario;
        this.network = scenario.getNetwork();
        this.nf = network.getFactory();
        this.schedule = scenario.getTransitSchedule();
        this.factory = scenario.getTransitSchedule().getFactory();
        this.vehiclesContainer = scenario.getTransitVehicles();

        TravelTime travelTimeCalc = new FreeSpeedTravelTime();
        TravelDisutility tCostCalc = new DistanceAsTravelDisutility();
        dijkstraTree = new DijkstraWithDijkstraTreeCache(this.network,
                tCostCalc, travelTimeCalc, TimeDiscretizer.CYCLIC_24_HOURS);

        // peak period is 6 to 9 and 4 to 6
        //HashMap<start, duration>
        HashMap<Integer, Integer> peakPeriods = new HashMap<>();
        peakPeriods.put(6, 3);
        peakPeriods.put(16, 2);
        this.periods.put("peak", peakPeriods);

        // off peak is 9 to 4 and 6 to 1 am
        //HashMap<start, duration>
        HashMap<Integer, Integer> offPeakPeriods = new HashMap<>();
        offPeakPeriods.put(9, 7);
        offPeakPeriods.put(18, 7);
        this.periods.put("offpeak", offPeakPeriods);

        // no overnight service
    }

    /**
     * Create transit routes based on information contained in the Cube Line Files
     */
    public void createTransit(){
        log.info("Making transit schedule");
        createStopFacilities();
        //createTransitLines();

        //cleanSchedule();
        //createVehicles(this.schedule, this.vehiclesContainer);
    }

    private void createVehicles(TransitSchedule schedule, Vehicles vehiclesContainer) {
    }

    private void cleanSchedule() {
        ScheduleCleaner.removeNotUsedStopFacilities(schedule);
    }

    private void createTransitLines() {
        log.info("Creating TransitLines from routes and TransitRoutes from trips...");
        for(CubeLine l: cubeLineHashMap.values()) {
            // Create transit line
            TransitLine transitLine = createTransitLine(l);
            schedule.addTransitLine(transitLine);

            // create transit Route for the forward direction
            TransitRoute transitRoute = createTransitRoute(l, "f");
            addDepartures(transitRoute, l);
            transitLine.addRoute(transitRoute);

            if(!l.getOneWay()){
                TransitRoute transitRouteR = createTransitRoute(l, "r");
                addDepartures(transitRouteR, l);
                transitLine.addRoute(transitRouteR);

            }

        }
    }

    private void addDepartures(TransitRoute transitRoute, CubeLine l) {
        Integer headway = 0;

        for(String period: periods.keySet()){
            if(period.equals("peak")) {
                headway = l.getHeadway()[0];
            } else {
                headway = l.getHeadway()[1];
            }

            if(headway > 0) {

                for (Integer startHour : periods.get(period).keySet()) {
                    // start time in seconds
                    Double startTime = startHour.doubleValue() * 3600;
                    // period duration in minutes
                    Integer duration = periods.get(period).get(startHour) * 60;
                    // number of trips is duration / headway
                    Integer trips = duration / headway;

                    for (int t = 0; t < trips; t++) {
                        Double departureTime = startTime + (headway * 60) * t;
                        Id<Departure> departureId = Id.create(
                                transitRoute.getId() + "_" + period + "_" + departureTime/3600.,
                                Departure.class);
                        Departure departure = factory.createDeparture(departureId, departureTime);
                        transitRoute.addDeparture(departure);
                    }


                }
            }
        }


    }

    private TransitRoute createTransitRoute(CubeLine line, String direction){

        // get stop sequence
        ArrayList<Integer> stops = line.getStopNodes();
        double dwellTime = 30.0;

        List<TransitRouteStop> transitRouteStops = new ArrayList<>();

        // add the first stop to the line
        TransitRouteStop trs0 = factory.createTransitRouteStop(
                schedule.getFacilities().get(Id.create(stops.get(0)+"t", TransitStopFacility.class)), 0, dwellTime
        );
        trs0.setAwaitDepartureTime(AWAIT_DEPARTURE_TIME_DEFAULT);
        transitRouteStops.add(trs0);

        double elapsedTime = dwellTime;
        TransitStopFacility lastStop = null;
        for(int i = 1; i < stops.size(); i++){
            Id<TransitStopFacility> trsiId = Id.create(stops.get(i) + "t", TransitStopFacility.class);
            //Double traveltime = calculateTravelTime(stops.get(i-1), stops.get(i));

            //TransitRouteStop trsi = factory.createTransitRouteStop(schedule.getFacilities().get(trsiId), v, dwellTime);
        }


        Id<TransitRoute> id = Id.create(line.getName() + direction, TransitRoute.class);
        TransitRoute transitRoute = factory.createTransitRoute(id, null,
                transitRouteStops, "bus");


        return transitRoute;
    }

    /**
     * Calculate the travel time between two stops
     *
     * @param lastStop Transit stop facility with a coordinate location
     * @param nextStop Transit stop facility with a coordinate location
     * @param l The cube line, defining the mode information
     * @return The time elapsed from the previous stop
     */
    private double calculateTravelTime(TransitStopFacility lastStop, TransitStopFacility nextStop, CubeLine l) {
        Double speed = l.getSpeed(); // speed as factor of free-flow speed
        Double distance;

        Node fromNode = NetworkUtils.getNearestNode(network, lastStop.getCoord());
        Node toNode = NetworkUtils.getNearestNode(network, nextStop.getCoord());
        LeastCostPathCalculator.Path path = dijkstraTree.calcLeastCostPath(fromNode, toNode, 0, null, null);
        return  Math.ceil(path.travelTime);
    }

    /**
     *  Create a MATSim Transit Route
     * @param l Cube Transit Line
     * @return A MATSim transit line with the id of the Cube transit service
     */
    private TransitLine createTransitLine(CubeLine l) {
        Id<TransitLine> id = Id.create(l.getName(), TransitLine.class);
        return this.factory.createTransitLine(id);
    }

    /**
     * Create a MATSim stop facility for each stop node.
     */
    private void createStopFacilities() {
        log.info("Creating stop facilities");

        // loop through the nodes of each transit line
        for(CubeLine l: cubeLineHashMap.values()){
            for(Integer n:l.getStopNodes()){
                Id<TransitStopFacility> id = Id.create(n + "t", TransitStopFacility.class);
                // check to make sure the schedule doesn't already have that node
                if(!schedule.getFacilities().keySet().contains(id)){
                    Node highwayNode = sc.getNetwork().getNodes().get(Id.create(n, Node.class));
                    Coord coord = highwayNode.getCoord();
                    TransitStopFacility tsf = factory.createTransitStopFacility(id, coord, BLOCKS_DEFAULT);
                    schedule.addStopFacility(tsf);

                }
            }
        }
    }

    /**
     * Read CUBE transit line files into data containers
     * @param linDirectory File path to a directory containing CUBE transit line files.
     */
    public void readLinFiles(File linDirectory) throws IOException {

        ArrayList<File> files = new ArrayList<File>(
                Arrays.asList(linDirectory.listFiles()));
        for(File f: files){
            String ext = Files.getFileExtension(f.getName());
            if(ext.equals("lin")){
                log.info("Reading transit file " + f);
                readLinFile(f);
            }
        }

    }

    /**
     * Read a single line file and store each line in a container.
     * @param linFile
     */
    private void readLinFile(File linFile) throws IOException {
        ArrayList<String> lineStringArray = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(linFile));
            String st;
            StringBuilder lineString = new StringBuilder();
            while ((st = br.readLine()) != null){
                CubeLine line = new CubeLine();
                line.setName("Empty");
                if(st.startsWith(";") | st.length() == 0){ // comment character, skip line
                } else if(st.startsWith("LINE NAME")) { // new route
                    lineStringArray.add(lineString.toString()); // add completed route to array
                    lineString = new StringBuilder(); // restart string with the new route
                    lineString.append(st);
                } else { // continues
                    lineString.append(st);
                };
            }

            lineStringArray.add(lineString.toString());

        } catch (IOException e) {
            log.error("Cannot find transit line file " + linFile);
            e.printStackTrace();
        }

        for(String linSt: lineStringArray){
            if(!linSt.equals("")){
                processLinString(linSt);
            }
        }

    }

    /**
     * Split line string into values and stash them in a container.
     * @param linSt
     */
    private void processLinString(String linSt){
        String[] values = linSt.split("\\s*,\\s*");
        CubeLine cubeLine = new CubeLine();
        ArrayList<Integer> nodes = new ArrayList<>();

        for(String value: values){
            value = value.trim();
            if(value.contains("=")){
                String[] term = value.split("=");
                switch (term[0]) {
                    case "LINE NAME":
                        cubeLine.setName(term[1].replace("\"", ""));
                        break;
                    case "MODE":
                        cubeLine.setMode(Integer.valueOf(term[1]));
                        break;
                    case "HEADWAY[1]":
                        cubeLine.setHeadway(Integer.valueOf(term[1]), 0);
                        break;
                    case "HEADWAY[2]":
                        cubeLine.setHeadway(Integer.valueOf(term[1]), 1);
                        break;
                    case "COLOR":
                        cubeLine.setColor(Integer.valueOf(term[1]));
                        break;
                    case "ONEWAY":
                        cubeLine.setOneway(Boolean.valueOf(term[1]));
                        break;
                    case "N":
                        nodes.add(Integer.valueOf(term[1]));
                        break;
                }
            } else {
                nodes.add(Integer.valueOf(value));
            }
        }

        cubeLine.setNodes(nodes);
        cubeLineHashMap.put(cubeLine.getName(), cubeLine);
    }



    public void writeFile(File scheduleFile, File vehiclesFile){
        log.info("Writing transit vehicles to file");
        new TransitScheduleWriter(sc.getTransitSchedule()).writeFile(scheduleFile.toString());
        new VehicleWriterV1(sc.getTransitVehicles()).writeFile(vehiclesFile.toString());
    }

}
