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

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ActivitySimFacilitiesReader {


    private Scenario scenario;
    File facilitiesFile;
    PopulationFactory pf;
    ActivityFacilitiesFactory factory;

    /**
         * Create an instance of ActivitySimFacilitiesReader using a new scenario
         * @param facilitiesFile File path to csv file containing facility coordinates
     * */
    public ActivitySimFacilitiesReader(File facilitiesFile) {
        Config config = ConfigUtils.createConfig();
        this.scenario = ScenarioUtils.createScenario(config);
        this.facilitiesFile = facilitiesFile;
        this.factory = scenario.getActivityFacilities().getFactory();
    }

    public void readFacilities() {
        try {
            // Start a reader and read the header row. `col` is an index between the column names and numbers
            CSVReader reader = CSVUtils.createCSVReader(facilitiesFile.toString());
            String[] header = reader.readNext();
            Map<String, Integer> col = CSVUtils.getIndices(header,
                    new String[]{"id", "zone", "x", "y"}, // mandatory columns
                    new String[]{"household_id"} // optional columns
            );

            // Read each line of the persons file
            String[] nextLine;
            while((nextLine = reader.readNext()) != null) {
                // Create a MATsim Facilities object
                Id<ActivityFacility> facilityId = Id.create(nextLine[col.get("id")], ActivityFacility.class);
                Double x = Double.valueOf(nextLine[col.get("x")]);
                Double y = Double.valueOf(nextLine[col.get("y")]);

                Coord coord = CoordUtils.createCoord(x, y);
                ActivityFacility facility = factory.createActivityFacility(facilityId,coord);

                scenario.getActivityFacilities().addActivityFacility(facility);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {



    }
}
