package byu.edu.network;

import byu.edu.activitysimutils.CSVUtils;
import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt2matsim.tools.ScheduleTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/***
 * This class will build a highway network from the AGRC network information. The network information
 * is stored in two CSV files: a nodes file with ids and coordinates, and a links file with the node
 * IDs and link information. These two files are created with the `R/agrc_network_export.R` script
 * in this repository.
 */
public class AGRCNetworkReader {
    private static final Logger log = Logger.getLogger(AGRCNetworkReader.class);
    private Scenario scenario;
    private CoordinateTransformation ct;
    private Network network;
    private NetworkFactory networkFactory;
    private Boolean hasTransit = false;
    private GTFSTransitMaker gtfsTransitMaker;

    private final File outDir;
    private final File nodesFile;
    private final File linksFile;
    private static File gtfsFolder;


    /**
     * Initialize a network reader.
     * @param scenario A MATSim scenario
     * @param nodesFile A CSV file with node IDS.
     * @param linksFile A CSV file with link attributes
     * @param outDir The path to the output MATSim network *directory*. File is `<outDir>/highway_network.xml.gz`
     */
    public AGRCNetworkReader(Scenario scenario, File nodesFile, File linksFile, File outDir) {
        this.scenario = scenario;
        this.network = this.scenario.getNetwork();
        this.networkFactory = network.getFactory();
        this.ct = TransformationFactory.getCoordinateTransformation("EPSG:4326",
                this.scenario.getConfig().global().getCoordinateSystem());
        this.outDir = outDir;
        this.nodesFile = nodesFile;
        this.linksFile = linksFile;
    }

    public AGRCNetworkReader(Scenario scenario, File nodesFile, File linksFile, File outDir, File gtfsFolder) {
        this(scenario, nodesFile, linksFile, outDir);
        this.hasTransit = true;
        this.gtfsTransitMaker = new GTFSTransitMaker(this.scenario);
        this.gtfsFolder = gtfsFolder;
    }

    public void makeNetwork() throws IOException {
        readNodes(nodesFile);
        readLinks(linksFile);
        addTurnaroundLinks();
        new NetworkCleaner().run(network);
        if(hasTransit){
            gtfsTransitMaker.readGtfsFolder(gtfsFolder);
        }
    }

    /**
     * Read a Nodes CSV file into the network.
     * @param nodesFile A CSV file with the following fields:
     *                  - id
     *                  - x (lon)
     *                  - y (lat)
     */
    private void readNodes(File nodesFile) throws IOException {
        // Start a reader and read the header row. `col` is an index between the column names and numbers
        CSVReader reader = CSVUtils.createCSVReader(nodesFile.toString());
        String[] header = reader.readNext();
        Map<String, Integer> col = CSVUtils.getIndices(header,
                new String[]{"id", "x", "y"}, // mandatory columns
                new String[]{"household_id"} // optional columns
        );

        String[] nextLine;
        while((nextLine = reader.readNext()) != null) {
            Id<Node> nodeId = Id.createNodeId(nextLine[col.get("id")]);
            Double lon = Double.valueOf(nextLine[col.get("x")]);
            Double lat = Double.valueOf(nextLine[col.get("y")]);
            Coord coordLatLon = CoordUtils.createCoord(lon, lat);
            Coord coord = ct.transform(coordLatLon);
            Node node = networkFactory.createNode(nodeId, coord);
            network.addNode(node);
        }

    }


    /**
     * Read the network links information
     * @param linksFile A csv file containing the following fields:
     *                  - link_id,
     *                  - Oneway,
     *                  - DriveTime (minutes)
     *                  - Length (miles)
     *                  - RoadClass
     *                  - AADT (count)
     *                  - start_node
     *                  - end_node
     *                  - ft
     *                  - lanes
     *                  - sl (miles per hour)
     *                  - med median treatment
     *                  - terrain
     *                  - capacity (vehicles / hr)
     */
    private void readLinks(File linksFile) throws IOException {
        CSVReader reader = CSVUtils.createCSVReader(linksFile.toString());
        String[] header = reader.readNext();
        Map<String, Integer> col = CSVUtils.getIndices(header,
                new String[]{"link_id", "Oneway", "start_node", "end_node", "Length_Miles",
                        "capacity", "DriveTime", "lanes"}, // mandatory columns
                new String[]{"Speed", "AADT", "RoadClass"} // optional columns
        );

        String[] nextLine;
        while ((nextLine = reader.readNext()) != null){
            // set up link ID with from and to nodes
            Id<Node> fromNodeId = Id.createNodeId(nextLine[col.get("start_node")]);
            Id<Node> toNodeId   = Id.createNodeId(nextLine[col.get("end_node")]);
            Node fromNode = network.getNodes().get(fromNodeId);
            Node toNode   = network.getNodes().get(toNodeId);
            Id<Link> linkId = Id.createLinkId(nextLine[col.get("link_id")]);
            Link l = networkFactory.createLink(linkId, fromNode, toNode);

            // get link attributes from csv
            Double driveTime = Double.valueOf(nextLine[col.get("DriveTime")]);
            Double lengthMiles = Double.valueOf(nextLine[col.get("Length_Miles")]);
            Double capacity = Double.valueOf(nextLine[col.get("capacity")]);
            Integer lanes = Integer.valueOf(nextLine[col.get("lanes")]);
            Integer oneWay = Integer.valueOf(nextLine[col.get("Oneway")]);

            Double length = lengthMiles * 1609.34; // convert miles to meters
            Double freeSpeed = length / (driveTime * 60); // convert meters per minute to meters per second

            // put link attributes on link
            l.setLength(length);
            l.setFreespeed(freeSpeed);
            l.setNumberOfLanes(lanes);
            l.setCapacity(capacity);
            network.addLink(l);

            // create reverse direction link if it exists
            if(oneWay != 1) {
                Id<Link> rLinkId = Id.createLinkId(nextLine[col.get("link_id")] + "r");
                Link rl = networkFactory.createLink(rLinkId, toNode, fromNode);

                rl.setLength(length);
                rl.setFreespeed(freeSpeed);
                rl.setNumberOfLanes(lanes);
                rl.setCapacity(capacity);
                network.addLink(rl);
            }

        }

    }


    /**
     * Interstates at the edge of the boundary need to have additional turnaround links added or paths
     * going in that direction end up breaking.
     */
    private void addTurnaroundLinks() {

        // create map of nodes that only have one link entering or exiting.
        ArrayList<Node> inOnlyNodes = new ArrayList<>();
        ArrayList<Node> outOnlyNodes = new ArrayList<>();

        // Loop through all nodes in the network, and populate the lists we just created
        Iterator<? extends Node> iter = network.getNodes().values().iterator();
        while (iter.hasNext()) {
            Node myNode = iter.next();
            if(myNode.getOutLinks().isEmpty()) inOnlyNodes.add(myNode); // no outbound links
            if(myNode.getInLinks().isEmpty()) outOnlyNodes.add(myNode); // no inbound links
        }

        // loop through all the outOnlyNodes
        for(Node outNode : outOnlyNodes){
            // Loop through the inOnlyNodes and see if there are any outOnlyNodes within 200m. Finds the nearest outOnlyNode
            Node matchInNode = null;
            Coord outCoord = outNode.getCoord();
            Double inDistance = Double.POSITIVE_INFINITY; // starting distance is infinite
            for (Node inNode : inOnlyNodes) {
                Coord inCoord = inNode.getCoord();
                Double thisDistance = NetworkUtils.getEuclideanDistance(outCoord, inCoord);
                if(thisDistance < inDistance & thisDistance < 200){
                    matchInNode = inNode; // update the selected companion node
                    inDistance = thisDistance; // update the comparison distance
                }
            }

            // if there is a matched inOnlyNode, we will build a new link with default stupid attributes
            if(matchInNode != null){
                Id<Link> lid = Id.createLinkId(outNode.getId() + "_" + matchInNode.getId());
                Link l =  networkFactory.createLink(lid, matchInNode, outNode);
                l.setCapacity(2000);
                l.setNumberOfLanes(1);
                l.setFreespeed(20);
                network.addLink(l);
                log.info("Added turnaround link " + lid);
            }
        }


    }



    private void writeNetwork(){
        log.info("Writing network to " + outDir);
        log.info("--- Links: " + network.getLinks().values().size());
        log.info("--- Nodes: " + network.getNodes().values().size());
        new NetworkWriter(network).write(outDir.toString() + "/highway_network.xml.gz");
        if (hasTransit) {
            gtfsTransitMaker.writeTransitOutputFiles(outDir);
        }
    }



    public static void main(String[] args) {
        Boolean hasTransit = false;
        File nodesFile = new File(args[0]);
        File linksFile = new File(args[1]);
        File outDir = new File(args[2]);
        String crs = args[3];
        File gtfsFolder = null;

        // If there are five command-line arguments given, then there is a GTFS folder in the
        // scenario and therefore transit is included.
        if(args.length == 5){
            hasTransit = true;
            gtfsFolder = new File(args[4]);
        }

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().global().setCoordinateSystem(crs);
        AGRCNetworkReader reader = null;
        if (hasTransit) {
            log.info("GTFS file available, constructing transit");
            reader = new AGRCNetworkReader(scenario, nodesFile, linksFile, outDir, gtfsFolder);
        } else {
            reader = new AGRCNetworkReader(scenario, nodesFile, linksFile, outDir);
        }


        try {
            reader.makeNetwork();
            reader.writeNetwork();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
