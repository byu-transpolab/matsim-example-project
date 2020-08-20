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
import java.util.Map;
import java.util.Random;

public class ActivitySimTripsReader {
    private static final Logger log = Logger.getLogger(ActivitySimTripsReader.class);

    Scenario scenario;
    PopulationFactory pf;
    File tripsFile;

    Random r = new Random(15);

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
     *//*
    public ActivitySimTripsReader(File tripsFile) {
        Config config = ConfigUtils.createConfig();
        this.scenario = ScenarioUtils.createScenario(config);
        this.tripsFile = tripsFile;
        this.pf = scenario.getPopulation().getFactory();
    }*/

    /**
     * Create a method to get departure times
     * Assign randomness to times
     */

//    private double getDepartureTime(activity) {
//        // get time here (then call in both places in the while loop)
//        Random r = new Random(15);
//        Double departTime = nextLine[col.get("depart")];
//        activity.setEndTime(7 * 3600 + r.nextGaussian() * 30*60); // random!
//
//    }

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
                if (prevPersonId != null && !personId.toString().equals(prevPersonId.toString())){
                    Person prevPerson = scenario.getPopulation().getPersons().get(prevPersonId);
                    Plan prevPlan = prevPerson.getPlans().get(0);
                    prevPlan.addActivity(prevActivity);
                    prevActivity = null;
                }

                prevPersonId = personId;
                Person person = scenario.getPopulation().getPersons().get(personId);

                // get the facility
                Plan plan = person.getPlans().get(0);

                // Get origin and destination id
                //is this where we can add coord
                Id<ActivityFacility> originId = Id.create(nextLine[col.get("origin")], ActivityFacility.class);
                Id<ActivityFacility> destId   = Id.create(nextLine[col.get("destination")], ActivityFacility.class);

                // get coords from facilities in scenario
                ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(originId);
                Coord originCoord = facility.getCoord();
                ActivityFacility facility2 = scenario.getActivityFacilities().getFacilities().get(destId);
                Coord destCoord = facility2.getCoord();

                // Add mode and purpose to activity
                String leg_mode = nextLine[col.get("trip_mode")];
                String purpose = nextLine[col.get("purpose")];
                // Get time of departure for each trip and convert to double
                // then add randomness
                String time = nextLine[col.get("depart")];
                Double dt = Double.parseDouble(time);

                Double depTime = dt*3600 + r.nextDouble()*3600; //adds a random number within 60 min



            // Plan for each line
            // if previous personId != current personId then write home (last activity)
            // 1 get previous activity (if none then write home)
            // 2 get end time
            // 3 get leg
            // 4


                if (plan.getPlanElements().isEmpty()){
                    Activity homeActivity = pf.createActivityFromActivityFacilityId("Home", originId);
                    homeActivity.setCoord(originCoord);
                    homeActivity.setEndTime(depTime);
                    plan.addActivity(homeActivity);
                    // add departure time
                    // departure time needs randomness (logic to check order)
                }

                if(prevActivity != null) {
                    prevActivity.setEndTime(depTime);
                    prevActivity.setCoord(originCoord);
                    plan.addActivity(prevActivity);
                }

                Leg leg = pf.createLeg(leg_mode);
                plan.addLeg(leg);

                prevActivity = pf.createActivityFromActivityFacilityId(purpose, destId);


                //Leg leg = pf.createLeg(leg_mode);
                //plan.addLeg(leg);



                // If this is the first leg, add a home activity



                // if not, add the next activity with the purpose
                // Activity activity = pf.createActivityFromActivityFacilityId(purpose, destId);
                // activity.setEndTime(actDepTime);
               // plan.addActivity(activity);

                //plan
                //home activity endtime: 11
                //leg walk
                //activity (destination): eatout time: 11

                //leg: walk
                // activity home: endtime: 12

                //leg: walk
                //activyt: othermaint endtime: 13

                //leg walk
                //  home




            }

            // last line
            Person prevPerson = scenario.getPopulation().getPersons().get(prevPersonId);
            Plan prevPlan = prevPerson.getPlans().get(0);
            prevPlan.addActivity(prevActivity);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
