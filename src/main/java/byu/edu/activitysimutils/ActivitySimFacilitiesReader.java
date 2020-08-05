//package byu.edu.activitysimutils;
//
//import com.opencsv.CSVReader;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.PopulationFactory;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.geometry.CoordUtils;
//import org.matsim.facilities.ActivityFacilitiesFactory;
//import org.matsim.facilities.ActivityFacility;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Map;
//
//public class ActivitySimFacilitiesReader {
//
//
//    private Scenario scenario;
//    File facilitiesFile;
//    PopulationFactory pf;
//    ActivityFacilitiesFactory factory;
//
///**
//     * Create an instance of ActivitySimFacilitiesReader using a new scenario
//     * @param tripsFile File path to csv file containing trips
// * */
//
//
//    public ActivitySimFacilitiesReader(File tripsFile) {
//        Config config = ConfigUtils.createConfig();
//        this.scenario = ScenarioUtils.createScenario(config);
//        this.facilitiesFile = tripsFile;
//        this.factory = scenario.getActivityFacilities().getFactory();
//    }
//
//    public void readZones() {
//        try {
//            // Start a reader and read the header row. `col` is an index between the column names and numbers
//            CSVReader reader = CSVUtils.createCSVReader(zonesFile.toString());
//            String[] header = reader.readNext();
//            Map<String, Integer> col = CSVUtils.getIndices(header,
//                    new String[]{"zone", "x", "y"}, // mandatory columns
//                    new String[]{"household_id"} // optional columns
//            );
//
//            // Read each line of the persons file
//            String[] nextLine;
//            while((nextLine = reader.readNext()) != null) {
//                // Create a MATsim Facilities object
//                Id<ActivityFacility> facilityId = Id.create(nextLine[col.get("zone")], ActivityFacility.class);
//                Coord coord = CoordUtils.createCoord(nextLine[col.get("x")], nextLine[col.get("y")]);
//                ActivityFacility facility = factory.createActivityFacility(facilityId,coord);
//
//                scenario.getActivityFacilities().addActivityFacility(facility);
//            }
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
