package byu.edu.activitysimutils;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Period;
import java.util.Map;

public class ActivitySimTripsReader {
    private static final Logger log = Logger.getLogger(ActivitySimTripsReader.class);

    Scenario scenario;
    PopulationFactory pf;
    File tripsFile;

    /**
     * Create an instance of ActivitySimTripsReader using an existing scenario
     * @param scenario A scenario
     * @param tripsFile File path to csv file containing trips
     */
    public ActivitySimTripsReader(Scenario scenario, File tripsFile){
        this.scenario = scenario;
        this.tripsFile = tripsFile;
        this.pf = scenario.getPopulation().getFactory();
    }

    /**
     * Create an instance of ActivitySimTripsReader using a new scenario
     * @param tripsFile File path to csv file containing trips
     */
    public ActivitySimTripsReader(File tripsFile) {
        Config config = ConfigUtils.createConfig();
        this.scenario = ScenarioUtils.createScenario(config);
        this.tripsFile = tripsFile;
        this.pf = scenario.getPopulation().getFactory();
    }

    public void readTrips() {
        try {
            // Start a reader and read the header row. `col` is an index between the column names and numbers
            CSVReader reader = CSVUtils.createCSVReader(tripsFile.toString());
            String[] header = reader.readNext();
            Map<String, Integer> col = CSVUtils.getIndices(header,
                    new String[]{"person_id", "primary_purpose", "purpose", "destination", "origin", "depart", "trip_mode"}, // mandatory columns
                    new String[]{"household_id"} // optional columns
                    );

            // Read each line of the trips file
            String[] nextLine;
            while((nextLine = reader.readNext()) != null) {
                // get plan for this person
                Id<Person> personId = Id.createPersonId(nextLine[col.get("person_id")]);
                Person person = scenario.getPopulation().getPersons().get(personId);
                Plan plan = person.getPlans().get(0);

                // Get origin and destination id
                Id<ActivityFacility> originId = Id.create(nextLine[col.get("origin")], ActivityFacility.class);
                Id<ActivityFacility> destId   = Id.create(nextLine[col.get("destination")], ActivityFacility.class);

                // If this is the first leg, add a home activity
                if (plan.getPlanElements().isEmpty()){
                    Activity homeActivity = pf.createActivityFromActivityFacilityId("Home", originId);
                    plan.addActivity(homeActivity);
                }

                // if not, add the next activity with the purpose


            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
