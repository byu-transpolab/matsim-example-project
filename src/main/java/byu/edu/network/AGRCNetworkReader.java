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
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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


    /**
     * Initialize a network reader.
     * @param scenario A MATSim scenario
     * @param nodesFile A CSV file with node IDS.
     * @param linksFile A CSV file with link attributes
     * @param outDir The path to the output MATSim network *directory*. File is `<outDir>/highway_network.xml.gz`
     */
    public AGRCNetworkReader(Scenario scenario, File nodesFile, File linksFile, File outDir) throws IOException {
        this.scenario = scenario;
        this.network = scenario.getNetwork();
        this.networkFactory = network.getFactory();
        this.ct = TransformationFactory.getCoordinateTransformation("EPSG:4326",
                scenario.getConfig().global().getCoordinateSystem());

        readNodes(nodesFile);
        readLinks(linksFile);
        writeNetwork(outDir);
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

    private void writeNetwork(File outDir){
        log.info("Writing network to " + outDir);
        log.info("--- Links: " + network.getLinks().values().size());
        log.info("--- Nodes: " + network.getNodes().values().size());
        new NetworkWriter(network).write(outDir.toString() + "/highway_network.xml.gz");
    }

    public static void main(String[] args) {
        File nodesFile = new File(args[0]);
        File linksFile = new File(args[1]);
        File outDir = new File(args[2]);
        String crs = args[3];

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().global().setCoordinateSystem(crs);
        try {
            AGRCNetworkReader reader = new AGRCNetworkReader(scenario, nodesFile, linksFile, outDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
