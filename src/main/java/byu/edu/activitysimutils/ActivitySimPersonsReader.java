package byu.edu.activitysimutils;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
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
     * @param tripsFile File path to csv file containing trips
     */
    public ActivitySimPersonsReader(Scenario scenario, File tripsFile){
        this.scenario = scenario;
        this.personsFile = tripsFile;
        this.pf = scenario.getPopulation().getFactory();
    }

    /**
     * Create an instance of ActivitySimPersonsReader using a new scenario
     * @param tripsFile File path to csv file containing trips
     */
    public ActivitySimPersonsReader(File tripsFile) {
        Config config = ConfigUtils.createConfig();
        this.scenario = ScenarioUtils.createScenario(config);
        this.personsFile = tripsFile;
        this.pf = scenario.getPopulation().getFactory();
    }

    public void readPersons() {
        try {
            // Start a reader and read the header row. `col` is an index between the column names and numbers
            CSVReader reader = CSVUtils.createCSVReader(personsFile.toString());
            String[] header = reader.readNext();
            Map<String, Integer> col = CSVUtils.getIndices(header,
                    new String[]{"person_id", "age", "sex", }, // mandatory columns
                    new String[]{"household_id"} // optional columns
            );

            // Read each line of the persons file
            String[] nextLine;
            while((nextLine = reader.readNext()) != null) {
                // Create this person
                Id<Person> personId = Id.createPersonId(nextLine[col.get("person_id")]);
                Person person = pf.createPerson(personId);

                person.getAttributes().putAttribute("age", nextLine[col.get("age")]);
                person.getAttributes().putAttribute("sex", nextLine[col.get("sex")]);

                if (col.keySet().contains("household_id")){
                    String hhId = nextLine[col.get("household_id")];
                }

                // create an empty plan
                person.addPlan(pf.createPlan());
                scenario.getPopulation().addPerson(person);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
