package byu.edu.activitysimutils;

import com.opencsv.CSVReader;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CSVUtils {
    private static final Logger log = Logger.getLogger(CSVUtils.class);

    /**
     * Creates a reader for CSV files
     *
     * @throws FileNotFoundException if the input file cannot be found
     */
    public static CSVReader createCSVReader(String path) throws FileNotFoundException {
        InputStream stream = new FileInputStream(path);
        return new CSVReader(new InputStreamReader(stream));
    }

    /**
     * Get indices from CSV Header
     * @param header an array of strings giving the indices for the
     * @param requiredColumns array of attributes you need the indices of
     * @param optionalColumns array of attributes that are handy to have around
     * @return the index for each attribute given in columnNames
     */
    public static Map<String, Integer> getIndices(String[] header, String[] requiredColumns, String[] optionalColumns) {
        Map<String, Integer> indices = new HashMap<>();
        Set<String> requiredNotFound = new HashSet<>();

        for(String columnName : requiredColumns) {
            boolean found = false;
            for(int i = 0; i < header.length; i++) {
                if(header[i].equals(columnName)) {
                    indices.put(columnName, i);
                    found = true;
                    break;
                }
            }
            if(!found) {
                requiredNotFound.add(columnName);
            }
        }

        if(requiredNotFound.size() > 0) {
            throw new IllegalArgumentException("Required column(s) " + requiredNotFound + " not found in csv. Might be some additional characters in the header or the encoding not being UTF-8.");
        }

        for(String columnName : optionalColumns) {
            for(int i = 0; i < header.length; i++) {
                if(header[i].equals(columnName)) {
                    indices.put(columnName, i);
                    break;
                }
            }
        }
        return indices;
    }
}
