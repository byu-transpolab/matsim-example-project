package byu.edu.activitysimutils;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class ActivitySimPersonsReader {
    private static final Logger log = Logger.getLogger(ActivitySimPersonsReader.class);

    Scenario scenario;
    PopulationFactory pf;
    File personsFile;

    /**
     * Create an instance of ActivitySimPersonsReader using an existing scenario
     * @param scenario A scenario
     * @param personsFile File path to csv file containing persons
     */
    public ActivitySimPersonsReader(Scenario scenario, File personsFile){
        this.scenario = scenario;
        this.personsFile = personsFile;
        this.pf = scenario.getPopulation().getFactory();
    }

    /**
     * Create an instance of ActivitySimPersonsReader using a new scenario
     * @param personsFile File path to csv file containing persons
     */
    public ActivitySimPersonsReader(File personsFile) {
        Config config = ConfigUtils.createConfig();
        this.scenario = ScenarioUtils.createScenario(config);
        this.personsFile = personsFile;
        this.pf = scenario.getPopulation().getFactory();
    }

    public void readPersons() {
        try {
            // Start a reader and read the header row. `col` is an index between the column names and numbers
            CSVReader reader = CSVUtils.createCSVReader(personsFile.toString());
            String[] header = reader.readNext();
            Map<String, Integer> col = CSVUtils.getIndices(header,
                    new String[]{"person_id", "age", "sex",  }, // mandatory columns
                    new String[]{"household_id", "wc_var"} // optional columns
            );

            // Read each line of the persons file
            String[] nextLine;
            while((nextLine = reader.readNext()) != null) {
                // Create this person
                Id<Person> personId = Id.createPersonId(nextLine[col.get("person_id")]);

                int age = Integer.parseInt(nextLine[col.get("age")]);
                String sex = nextLine[col.get("sex")];
                //String wc_var = nextLine[col.get("wc_var")];
                String household_id = nextLine[col.get("household_id")];

                //if (wc_var.equals("TRUE")) {
                //    personId = Id.createPersonId("wc-" + personId);
                //}
                Person person = pf.createPerson(personId);
                person.getAttributes().putAttribute("age", age);
                person.getAttributes().putAttribute("sex", sex);
                //person.getAttributes().putAttribute("wc_var", wc_var);
                person.getAttributes().putAttribute("household_id", household_id);

                // create an empty plan
                person.addPlan(pf.createPlan());
                scenario.getPopulation().addPerson(person);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This reads in the Persons file again, parses through each ID and if that person in the scenario has no plans, tacks on home coordinates in their plans
     */
    public void readPlans() {
        try {
            CSVReader reader = CSVUtils.createCSVReader(personsFile.toString());
            String[] header = new String[0];

            header = reader.readNext();

            Map<String, Integer> col = CSVUtils.getIndices(header,
                    new String[]{"person_id"}, // mandatory columns
                    new String[]{"household_id"} // optional columns
            );

            // Read each line of the trips file
            String[] nextLine;
            Activity prevActivity = null;

            Id<Person> prevPersonId = null;
            while ((nextLine = reader.readNext()) != null) {
                // get plan for this person
                Id<Person> personId = Id.createPersonId(nextLine[col.get("person_id")]);

                Id<ActivityFacility> homeId = Id.create("h" + nextLine[col.get("household_id")], ActivityFacility.class);

                Person person = scenario.getPopulation().getPersons().get(personId);
                if (person == null) {
                    personId = Id.createPersonId("wc-" + personId);
                    person = scenario.getPopulation().getPersons().get(personId);
                }
                Plan plan = person.getPlans().get(0);
                if (plan.getPlanElements().isEmpty()){
                    Activity homeActivity1 = pf.createActivityFromActivityFacilityId("Home", homeId);
                    Coord home = scenario.getActivityFacilities().getFacilities().get(homeId).getCoord();
                    homeActivity1.setCoord(home);
                    plan.addActivity(homeActivity1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
