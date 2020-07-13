package byu.edu.cubeutils;

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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CubeNetworkReader {
    private static final Logger log = Logger.getLogger(CubeNetworkReader.class);
    private Scenario sc;
    private Network network;
    private NetworkFactory nf;
    private CoordinateTransformation ct;

    /**
     * Create the class, starting up factories and containers
     */
    public CubeNetworkReader(String fromCRS, String toCRS) {
        Config config = ConfigUtils.createConfig();
        sc = ScenarioUtils.createScenario(config);
        network = sc.getNetwork();
        nf = network.getFactory();
        ct = TransformationFactory.getCoordinateTransformation(fromCRS, toCRS);
    }

    // When you have an existing scenario
    public CubeNetworkReader(Scenario sc){
        this.sc = sc;
        network = sc.getNetwork();
        nf = network.getFactory();
        ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
                sc.getConfig().global().getCoordinateSystem());
    }

    /**
     * Read the nodes CSV file. For every node, create a corresponding node in the MATSim network
     * in the network
     * @param nodesFile Path to CSV file as a file
     */
    public void readNodesFile(File nodesFile) throws IOException {
        log.info("Reading Nodes file from " + nodesFile);
        CSVReader reader = CSVUtils.createCSVReader(nodesFile.toString());
        String[] header = reader.readNext();
        Map<String, Integer> col = CSVUtils.getIndices(header, new String[]{"N", "X", "Y"}, new String[]{});

        String[] nextLine;
        while((nextLine = reader.readNext()) != null) {
            // here we can change string encoding if it is needed
            String id = nextLine[col.get("N")];
            Double lon = Double.valueOf(nextLine[col.get("X")]);
            Double lat = Double.valueOf(nextLine[col.get("Y")]);
            Id nodeId = Id.createNodeId(id);
            Coord coord = new Coord(lon, lat);
            ct.transform(coord);

            Node n = nf.createNode(nodeId, coord);
            network.addNode(n);
        }

    }

    /**
     * Read the links CSV file. For every link, create a MATSim network
     * @param linksFile Path to CSV links file, as file
     */
    public void readLinksFile(File linksFile) throws IOException {
        log.info("Reading Links file from " + linksFile);
        CSVReader reader = CSVUtils.createCSVReader(linksFile.toString());
        String[] header = reader.readNext();
        Map<String, Integer> col = CSVUtils.getIndices(header,
                new String[]{"A", "B", "LINKID", "LANES", "DISTANCE", "CAP1HR1LN", "FF_SPD", "ONEWAY"},
                new String[]{});
        String[] nextLine;
        while((nextLine = reader.readNext()) != null) {
            // Get strings of nodes and turn them into IDs
            Id<Node> aNodeId = Id.createNodeId(nextLine[col.get("A")]);
            Id<Node> bNodeId = Id.createNodeId(nextLine[col.get("B")]);
            Id<Link> linkId = Id.createLinkId(nextLine[col.get("LINKID")]);

            Node aNode = network.getNodes().get(aNodeId);
            Node bNode = network.getNodes().get(bNodeId);

            // Get other information from the link table
            Double capacity = Double.valueOf(nextLine[col.get("CAP1HR1LN")]);
            Double length = Double.valueOf(nextLine[col.get("DISTANCE")]) * 1609.34; // convert miles to meters
            Double freeSpeed = Double.valueOf(nextLine[col.get("FF_SPD")]) * 0.44707; // convert mph to meters per second
            Integer lanes = Integer.valueOf(nextLine[col.get("LANES")]);
            Integer oneWay = Integer.valueOf(nextLine[col.get("ONEWAY")]);

            if(lanes > 0 & lanes < 7) { // zero lanes means the road does not exist in the network year; 7 are centroid connectors

                // Create link in forward direction
                Link link = nf.createLink(linkId, aNode, bNode);
                link.setCapacity(capacity * lanes);
                link.setLength(length);
                link.setFreespeed(freeSpeed);
                network.addLink(link);

                // create link in reverse direction if not one way
                if(oneWay == 2){
                    Id<Link> rLinkId = Id.createLinkId(nextLine[col.get("LINKID")]+ "r");
                    Link rLink = nf.createLink(rLinkId, bNode, aNode);
                    rLink.setCapacity(capacity * lanes);
                    rLink.setLength(length);
                    rLink.setFreespeed(freeSpeed);
                    network.addLink(rLink);
                }

            }

        }

    }

    /**
     * Write network to xml output file
     * @param outFile Path to output xml file, as file.
     */
    public void writeFile(File outFile){
        log.info("Writing network to " + outFile);
        log.info("--- Links: " + network.getLinks().values().size());
        log.info("--- Nodes: " + network.getNodes().values().size());
        new NetworkWriter(network).write(outFile.toString());
    }


    /**
     * Main method for standalone operation
     * @param args:
     *            0: nodes DBF file
     *            1: links DBF file
     *            2: output XML file
     */
    public static void main(String[] args) throws IOException {
        log.info("======= Converting CUBE Highway Network DBF to MATSim Network ==========");
        CubeNetworkReader cubeNetworkReader = new CubeNetworkReader(args[3], args[4]);
        cubeNetworkReader.readNodesFile(new File(args[0]));
        cubeNetworkReader.readLinksFile(new File(args[1]));
        cubeNetworkReader.writeFile(new File(args[2]));
        log.info("======= Finished ==============");

    }


}
