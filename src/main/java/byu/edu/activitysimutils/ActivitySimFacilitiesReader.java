package byu.edu.activitysimutils;

import com.opencsv.CSVReader;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ActivitySimFacilitiesReader {

    //here are my questions
    // I read the zones file, then match them with the facility in the plans?
    // or do they save to the facilities in the scenario? what does that mean?
    // I don't know why i would need trips file here.

    private Scenario scenario;
    File facilitiesFile;
    PopulationFactory pf;
    ActivityFacilitiesFactory factory;

    /**
         * Create an instance of ActivitySimFacilitiesReader using a new scenario
         * @param facilitiesFile File path to csv file containing facility coordinates
     * */
    public ActivitySimFacilitiesReader(Scenario scenario, File facilitiesFile) {
        //Config config = ConfigUtils.createConfig();
        //this.scenario = ScenarioUtils.createScenario(config);
        this.scenario = scenario;
        this.facilitiesFile = facilitiesFile;
        this.factory = scenario.getActivityFacilities().getFactory();
    }

    public void readFacilities() {
        try {
            // Start a reader and read the header row. `col` is an index between the column names and numbers
            CSVReader reader = CSVUtils.createCSVReader(facilitiesFile.toString());
            String[] header = reader.readNext();
            Map<String, Integer> col = CSVUtils.getIndices(header,
                    new String[]{"facility_id", "TAZ", "lon", "lat"}, // mandatory columns
                    new String[]{"business_id"} // optional columns
            );

            // Read each line of the persons file
            String[] nextLine;
            while((nextLine = reader.readNext()) != null) {
                // Create a MATsim Facilities object
                Id<ActivityFacility> facilityId = Id.create(nextLine[col.get("facility_id")], ActivityFacility.class);
                Double x = Double.valueOf(nextLine[col.get("lon")]);
                Double y = Double.valueOf(nextLine[col.get("lat")]);

                Coord coord = CoordUtils.createCoord(x, y);
                ActivityFacility facility = factory.createActivityFacility(facilityId,coord);

                scenario.getActivityFacilities().addActivityFacility(facility);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void writeFiles(File outFile) {
         new FacilitiesWriter(scenario.getActivityFacilities()).write(outFile.toString());
    }

    /*public static void main(String[] args) throws IOException{
        String directory = args[0];
        String outDirectory = args[1];

        File facilitiesFile = new File(directory, "facility_ids.csv");
        File outFile = new File(outDirectory, "facility_list.xml.gz");

        ActivitySimFacilitiesReader afr = new ActivitySimFacilitiesReader(scenario, facilitiesFile);
        afr.readFacilities();
        afr.writeFiles(outFile);

    }*/


}
