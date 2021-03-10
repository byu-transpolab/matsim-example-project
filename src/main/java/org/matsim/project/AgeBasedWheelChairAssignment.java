package org.matsim.project;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AgeBasedWheelChairAssignment {
    private static final Logger log = Logger.getLogger(String.valueOf(AgeBasedWheelChairAssignment.class));
    private static Map<Integer, Double> wheelchairProbabilities= new HashMap<>();

    public static void main(String[] args) throws IOException {
        File probabilities = new File("scenarios/activitysim_output/probability_wc.csv");
        wheelchairProbabilities = readInProbabilities(probabilities);
        readInFile();

    }
    private static Map<Integer, Double> readInProbabilities(File probabilities) throws FileNotFoundException {
        Scanner sc = new Scanner(probabilities);
        Map<Integer, Double> wheelchairProbabilities;
        wheelchairProbabilities = new HashMap<>();
        String firstLine = sc.nextLine();
        log.log(Level.FINE, "Reading in " + firstLine);
        String nextLine;
        String[] data;
        while(sc.hasNextLine()){
            nextLine = sc.nextLine();
            data = nextLine.split(",");
            wheelchairProbabilities.put(Integer.parseInt(data[0]), Double.parseDouble(data[1]));
        }


        return wheelchairProbabilities;
    }
    private static String replace(int age, String personID){
        String newPersonID = personID;
        double random = Math.random();
        if (random <= wheelchairProbabilities.get(age)){
            newPersonID = personID.substring(0, 13) + "wc-" + personID.substring(13);
        }
        return newPersonID;
    }
    private static void readInFile(){
        BufferedReader reader;
        BufferedWriter writer;
        File XMLFile = new File("scenarios/activitysim_output/population.xml");
        try {
            FileWriter fStream = new FileWriter("probability_population.xml", false);
            writer = new BufferedWriter(fStream);
            reader = new BufferedReader(new FileReader(XMLFile));
            String nextLine = "";
            /*
            This is where the big boy stuff happens.
             */
            while (!nextLine.equals("</population>\n")){
                if (nextLine.contains("<person")){
                    StringBuilder sb = new StringBuilder();
                    String personID;
                    String attributeTag;
                    String ageAttribute;
                    while (!nextLine.equals("\t</person>\n")){
                        if (nextLine.contains("<person id=")){
                            personID = nextLine;
                            nextLine = reader.readLine() + "\n";
                            attributeTag = nextLine;
                            nextLine = reader.readLine() + "\n";
                            int age = Integer.parseInt(nextLine.replaceAll("\\D+", ""));
                            ageAttribute = nextLine;
                            personID = replace(age, personID);
                            sb.append(personID);
                            sb.append(attributeTag);
                            sb.append(ageAttribute);
                            nextLine = reader.readLine() + "\n";                        }

                        sb.append(nextLine);
                        nextLine = reader.readLine() + "\n";
                    }
                    sb.append(nextLine);
                    writer.write(sb.toString());
                }
                else {
                    writer.write(nextLine);
                    //System.out.println(nextLine);
                }
                nextLine = reader.readLine() + "\n";
            }
            writer.write(nextLine);
            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
