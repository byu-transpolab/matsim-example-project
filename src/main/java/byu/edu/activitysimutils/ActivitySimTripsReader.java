package byu.edu.activitysimutils;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.Facility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ActivitySimTripsReader {
    private static final Logger log = Logger.getLogger(ActivitySimTripsReader.class);

    Scenario scenario;
    PopulationFactory pf;
    File tripsFile;

    Random r = new Random(15);
    HashMap<String, List<Id<ActivityFacility>>> tazFacilitymap = null;

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

    public ActivitySimTripsReader(Scenario scenario, File tripsFile, HashMap<String, List<Id<ActivityFacility>>> tazFacilityMap) {
        this(scenario, tripsFile);
        this.tazFacilitymap = tazFacilityMap;
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
            Activity prevActivity = null;

            Id<Person> prevPersonId = null;
            while((nextLine = reader.readNext()) != null) {
                // get plan for this person
                Id<Person> personId = Id.createPersonId(nextLine[col.get("person_id")]);
                Id<ActivityFacility> homeId = Id.create("h" + nextLine[col.get("household_id")], ActivityFacility.class);
                Person person = scenario.getPopulation().getPersons().get(personId);
                Plan plan = person.getPlans().get(0);

                // Get time of departure for this trip and add randomness
                Double time = Double.valueOf(nextLine[col.get("depart")]);
                Double depTime = time*3600 + r.nextDouble()*3600; //adds a random number within 60 min

                // Handle origin side
                // Is this the first trip of the day?
                if (plan.getPlanElements().isEmpty()){
                    Activity homeActivity1 = pf.createActivityFromActivityFacilityId("Home", homeId);
                    homeActivity1.setEndTime(depTime);
                    plan.addActivity(homeActivity1);
                } else { // if not, then there is an existing activity that we need to find. maybe?
                    // and add a departure to it!


                }

                // Add leg to plan
                String leg_mode = nextLine[col.get("trip_mode")];
                Leg leg = pf.createLeg(leg_mode);
                plan.addLeg(leg);


                // Handle next activity
                String purpose = nextLine[col.get("purpose")];
                String destId   = nextLine[col.get("destination")];

                if(purpose.equals("Home")) {
                    Activity homeActivity2 = pf.createActivityFromActivityFacilityId("Home", homeId);
                    plan.addActivity(homeActivity2);
                } else {
                    ActivityFacility nextPlace = getFacilityinZone(destId);
                    Activity otherActivity = pf.createActivityFromActivityFacilityId(purpose, nextPlace.getId());
                    plan.addActivity(otherActivity);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * select a random facility within a TAZ.
     * @param tazId
     * @return
     */
    private ActivityFacility getFacilityinZone(String tazId) {
        List<Id<ActivityFacility>> facilityList = tazFacilitymap.get(tazId);
        Id<ActivityFacility> facilityId = facilityList.get(r.nextInt(facilityList.size()));

        return scenario.getActivityFacilities().getFacilities().get(facilityId);
    }

}
