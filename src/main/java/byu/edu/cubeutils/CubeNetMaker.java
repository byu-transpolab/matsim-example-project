package byu.edu.cubeutils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;

public class CubeNetMaker {
    private static final Logger log = Logger.getLogger(CubeNetMaker.class);
    private Boolean hasTransit = false;

    private File highwayFile;
    private File scheduleFile;
    private File vehiclesFile;


    CubeNetworkReader networkReader;
    CubeTransitReader transitReader;

    public CubeNetMaker(Scenario scenario) {
        networkReader = new CubeNetworkReader(scenario);
        transitReader = new CubeTransitReader(scenario);
    }
    public void makeHighwayNetwork(File nodesFile, File linksFile) throws IOException {
        networkReader.readNodesFile(nodesFile);
        networkReader.readLinksFile(linksFile);
    }


    public void makeTransitNetwork(File linDirectory) throws IOException {
        hasTransit = true;
        transitReader.readLinFiles(linDirectory);
        transitReader.createTransit();
    }


    /**
     * Set the files for writing out.
     * @param highwayFile File to put the highway network
     * @param scheduleFile File to put the transit schedule file
     * @param vehiclesFile File ot put the transit vehicles file
     */
    public void setOutputFiles(File highwayFile, File scheduleFile, File vehiclesFile){
        this.highwayFile = highwayFile;
        this.scheduleFile = scheduleFile;
        this.vehiclesFile = vehiclesFile;
    }

    /**
     * If there is no transit, no need to provide output files for the transit components
     * @param highwayFile File to put the highway network
     */
    public void setOutputFiles(File highwayFile){
        this.highwayFile = highwayFile;
    }

    public void writeFiles(){
        networkReader.writeFile(highwayFile);
        if(hasTransit){
            transitReader.writeFile(scheduleFile, vehiclesFile);
        }
    }

    public static void main(String[] args) throws IOException {
        String crs = args[0];
        String directory = args[1];
        String outDirectory = args[2];

        File nodesFile = new File(directory, "nodes.csv");
        File linksFile = new File(directory, "links.csv");
        File hwyFile = new File(outDirectory, "highway_network.xml.gz");


        File scheduleFile = new File(outDirectory, "transit_schedule.xml.gz");
        File vehiclesFile = new File(outDirectory, "transit_vehicles.xml.gz");


        log.info("======= Converting CUBE Highway Network DBF to MATSim Network ==========");
        Config config = ConfigUtils.createConfig();
        Scenario sc = ScenarioUtils.createScenario(config);
        sc.getConfig().global().setCoordinateSystem(crs);

        CubeNetMaker cnm = new CubeNetMaker(sc);
        cnm.makeHighwayNetwork(nodesFile, linksFile);
        cnm.makeTransitNetwork(new File(directory));
        cnm.setOutputFiles(hwyFile, scheduleFile, vehiclesFile);
        cnm.writeFiles();
        log.info("======= Finished ==============");
    }

}
