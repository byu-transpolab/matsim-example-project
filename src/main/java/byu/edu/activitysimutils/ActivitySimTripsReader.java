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
import org.matsim.facilities.ActivityFacilities;
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

    // map for storing purpose to use as facility type
    // <destId, purpose> then we use the purpose from the origin (previous trip's destination)
    HashMap<String, String> purposeMap = new HashMap<>();

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

            while((nextLine = reader.readNext()) != null) {
                // get plan for this person
                Id<Person> personId = Id.createPersonId(nextLine[col.get("person_id")]);
                Id<ActivityFacility> homeId = Id.create("h" + nextLine[col.get("household_id")], ActivityFacility.class);
                Person person = scenario.getPopulation().getPersons().get(personId);
                Plan plan = person.getPlans().get(0);

                // Get time of departure for this trip and add randomness
                Double time = Double.valueOf(nextLine[col.get("depart")]);
                Double depTime = time*3600 + r.nextDouble()*3600; //adds a random number within 60 min

                // Handle next activity
                String purpose = nextLine[col.get("purpose")];
                String destId   = nextLine[col.get("destination")];
                String originId = nextLine[col.get("origin")];

                // sometimes 2296 is home and for someone else it is work...
                purposeMap.put(destId, purpose);
                String activityPurpose = purposeMap.get(originId);

                // store the random facility with the origin id here

                //Id<ActivityFacility> activityId = Id.create(getFacilityinZone(originId), ActivityFacility.class);
                Id<ActivityFacility> activityId = getFacilityinZone(originId);


                // Handle origin side
                // Is this the first trip of the day?
                if (plan.getPlanElements().isEmpty()){
                    //ActivityFacility homeBase = getFacilityinZone(originId);
                    Activity homeActivity1 = pf.createActivityFromActivityFacilityId("Home", homeId);
                    //ActivityFacility home = scenario.getActivityFacilities().getFacilities().get(homeActivity1);
                    // this coord is coming from a random facility in the same TAZ, not the households file
                    Coord homeBase2 = scenario.getActivityFacilities().getFacilities().get(homeId).getCoord();
                    homeActivity1.setCoord(homeBase2);
                    //homeActivity1.setCoord(homeBase.getCoord());
                    homeActivity1.setEndTime(depTime);
                    plan.addActivity(homeActivity1);
                } else {
                    //ActivityFacility activity = getFacilityinZone(originId);
                    //Id<ActivityFacility> activityId = activity.getId();
                    // BUG: the purpose is related to destination not origin
                    // lets write a map that stores the purpose with destination (of previous trip)
                    // and gets the purpose from the matching origin
                    Activity newActivity = pf.createActivityFromActivityFacilityId(activityPurpose, activityId);
                    Coord activityCoord = scenario.getActivityFacilities().getFacilities().get(activityId).getCoord();
                    newActivity.setCoord(activityCoord);
                    newActivity.setEndTime(depTime);
                    plan.addActivity((newActivity));
                }

                // Add leg to plan
                String leg_mode = nextLine[col.get("trip_mode")];
                Leg leg = pf.createLeg(leg_mode);
                plan.addLeg(leg);




                if(purpose.equals("Home")) {
                    //ActivityFacility homeBase2 = getFacilityinZone(destId);
                    Activity homeActivity2 = pf.createActivityFromActivityFacilityId("Home", homeId);
                    //ActivityFacility home2 = scenario.getActivityFacilities().getFacilities().get(homeActivity2);
                    Coord homeBase2 = scenario.getActivityFacilities().getFacilities().get(homeId).getCoord();
                    homeActivity2.setCoord(homeBase2);
                    plan.addActivity(homeActivity2);
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
