package byu.edu.activitysimutils;

import com.opencsv.CSVReader;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.Double.parseDouble;

public class ActivitySimFacilitiesReader {

    private Scenario scenario;
    File facilitiesFile;
    File householdsFile;
    File coordinateFile;
    PopulationFactory pf;
    ActivityFacilitiesFactory factory;
    CoordinateTransformation ct;

    /*
    TAZID: [facility1, facility2, facility3]
     */
    public HashMap<String, List<Id<ActivityFacility>>> tazFacilityMap = new HashMap<>();
    public HashMap<String, List<Double>> householdMap = new HashMap<>();

    /**
         * Create an instance of ActivitySimFacilitiesReader using an existing scenario
         * @param facilitiesFile File path to csv file containing facility coordinates
     * */
    public ActivitySimFacilitiesReader(Scenario scenario, File facilitiesFile, File householdsFile, File coordinateFile) {

        this.scenario = scenario;
        this.facilitiesFile = facilitiesFile;
        this.householdsFile = householdsFile;
        this.coordinateFile = coordinateFile;
        this.factory = scenario.getActivityFacilities().getFactory();
        this.ct = TransformationFactory.getCoordinateTransformation("EPSG:4326", scenario.getConfig().global().getCoordinateSystem());
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
                String tazId = nextLine[col.get("TAZ")];

                Coord coord = CoordUtils.createCoord(x, y);
                coord = ct.transform(coord);
                ActivityFacility facility = factory.createActivityFacility(facilityId, coord);

                scenario.getActivityFacilities().addActivityFacility(facility);

                if(tazFacilityMap.containsKey(tazId)){
                    List tazidList = tazFacilityMap.get(tazId);
                    tazidList.add(facilityId);
                    tazFacilityMap.put(tazId, tazidList);
                } else {
                    List<Id<ActivityFacility>> tazidList = new ArrayList();
                    tazidList.add(facilityId);
                    tazFacilityMap.put(tazId, tazidList);
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void readHouseholds(){
        try {
            // Start a reader and read the header row. `col` is an index between the column names and numbers
            CSVReader reader = CSVUtils.createCSVReader(coordinateFile.toString());
            String[] header = reader.readNext();
            Map<String, Integer> col = CSVUtils.getIndices(header,
                    new String[]{"household_id", "longitude", "latitude"}, // mandatory columns
                    new String[]{"income"} // optional columns
            );

            // Read each line of the households file
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // Create a MATsim Facilities object
                Id<ActivityFacility> houseId = Id.create("h" + nextLine[col.get("household_id")], ActivityFacility.class);
                Double x = Double.valueOf(nextLine[col.get("longitude")]);
                Double y = Double.valueOf(nextLine[col.get("latitude")]);

                Coord coord = CoordUtils.createCoord(x, y);
                coord = ct.transform(coord);
                ActivityFacility facility = factory.createActivityFacility(houseId, coord);

                scenario.getActivityFacilities().addActivityFacility(facility);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHouseholdAttributes(){
        try {
            // Start a reader and read the header row. `col` is an index between the column names and numbers
            CSVReader reader = CSVUtils.createCSVReader(householdsFile.toString());
            String[] header = reader.readNext();
            Map<String, Integer> col = CSVUtils.getIndices(header,
                    new String[]{"household_id", "income", "auto_ownership", "num_workers","hhsize"}, // mandatory columns
                    new String[]{"income_segment", "num_non_workers"} // optional columns
            );

            // Read each line of the households file
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                // Read in household attribute values
                String household_id = nextLine[col.get("household_id")];
                Double income = parseDouble(nextLine[col.get("income")]);
                Double auto_ownership = parseDouble(nextLine[col.get("auto_ownership")]);
                Double num_workers = parseDouble(nextLine[col.get("num_workers")]);
                Double hhSize = parseDouble(nextLine[col.get("hhsize")]);

                // Add household attributes to household map
                List<Double> hhAttributes = Arrays.asList(income, auto_ownership, num_workers, hhSize);
                householdMap.put(household_id, hhAttributes);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /*
    private void readCoordinates(){
        try {
            CSVReader reader = CSVUtils.createCSVReader(coordinateFile.toString());
            String[] header = reader.readNext();
            Map<String, Integer> col = CSVUtils.getIndices(header,
                    new String[]{"household_id", "longitude", "latitude"}, // mandatory columns

            );

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/
    public HashMap<String, List<Id<ActivityFacility>>> getTazFacilityMap() {
        return tazFacilityMap;
    }
    public HashMap<String, List<Double>> getHouseholdMap(){return householdMap;}




}
