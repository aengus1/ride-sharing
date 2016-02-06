/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ridesharing.algorithm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import ridesharing.model.AllPairsShortestPathMatrix;
import ridesharing.model.TripRequest;
import ridesharing.model.Vehicle;

/**
 *
 * @author aengusmccullough
 */
public class Main {

    /**
     * @param args the command line arguments 
     */
    public static void main(String[] args) {

        //1. parse and validate args
        final Map<String, String> clArgs = parseCommandLineArgs(args);
        validateArgs(clArgs);
        
        

        //2. Parse the input file
        File f = new File(clArgs.get("input"));
        List<TripRequest> tripRequests = null;
        try {
            tripRequests = InputParser.parseInputFile(f);
        } catch (IOException ex) {
            exitWithException("Error parsing input file");
        }

        //3. calculate the all pairs shortest path matrix
        AllPairsShortestPathMatrix matrix = new AllPairsShortestPathMatrix(tripRequests);

        //4. update the trip requests with earliest / latest arrival departure information
        for (TripRequest tr : tripRequests) {
            tr.getSource().setLatest(tr.getDestination().getLatest().minusMinutes((long) Math.ceil(tr.travelTime(matrix))));
            tr.getDestination().setEarliest(tr.getSource().getEarliest().plusMinutes((long) Math.ceil(tr.travelTime(matrix))));
        }

        //5. Set parameters
        
        int beta = 5;      //controls randomization of source/dest point insertion  
        if(clArgs.containsKey("beta")){
            beta = Integer.parseInt(clArgs.get("beta"));
        }
        int gamma = 25;     //controls randomization of trip request selection (higher = more random, lower = favour greedy solution)
        if(clArgs.containsKey("gamma")){
            gamma = Integer.parseInt(clArgs.get("gamma"));
        }
        int capacity = 3;
        if(clArgs.containsKey("capacity")){
            capacity = Integer.parseInt(clArgs.get("capacity"));
        }        
        int nIterations = 100;
        if(clArgs.containsKey("i")){
            nIterations = Integer.parseInt(clArgs.get("i"));
        }
        int nSearchIterations = 50;
        if(clArgs.containsKey("s")){
            nSearchIterations = Integer.parseInt(clArgs.get("s"));
        }
        
        //6. run algorithm
        Solver solver = new Solver(tripRequests, matrix, beta, gamma, capacity, nIterations, nSearchIterations);
        List<Vehicle> result = solver.solve();

        //7. print output
        for (Vehicle v : result) {
            System.out.println("Itinerary for vehicle " + v.getVehicleId() + ":");
            System.out.println(v.getItinerary());
        }
        
        //8. write output
        if(clArgs.containsKey("output")){
            
            String output = clArgs.get("output");
            System.out.println("writing output: " + output);
            File fo = new File(output);
            FileWriter fw = null;
            try {
                 fw = new FileWriter(fo);
                for (Vehicle r : result) {
                    fw.write("Itinerary for vehicle " + r.getVehicleId() + ":\n");
                    fw.write(r.getItinerary());
                }
                fw.write("\n");
            } catch (IOException ex) {
                exitWithException("An error occurred attempting to write output file to: " + output);                
            }finally{
                try {
                    fw.flush();
                    fw.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                
            }
            
        }
    }

    /**
     * print usage
     */
    static void printUsage() {
        final String usage = "Usage: <path_to_input_file> \n"
                + "options:[ \n --output <path_to_output_file> \n"
                + " --capacity <vehicle capacity> \n"
                + " --beta <no. of candidate insertion points to consider>  (default 5) \n"
                + " --gamma <level of randomization> (default 20)\n"
                + " --i <no of iterations> (default 20)\n"
                + " --s <no of search iterations> (default 50) \n ] \n"
                + " --help";
        System.out.println(usage);
        System.exit(0);
    }

    /**
     * parse command line args
     * @param args
     * @return Map<String,String> key/value
     */
    static Map<String, String> parseCommandLineArgs(String[] args) {
        Map<String, String> result = new HashMap<>();        
        if (args.length > 0 && !args[0].startsWith("--")) {
            result.put("input", args[0]);
        }
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                arg = arg.replaceAll("--", "");
                if (args.length >= i + 1 && !args[i + 1].startsWith("--")) {
                    result.put(arg, args[i + 1]);
                }
            }
        }
        return result;
    }

    /**
     * print message to stderr and exit
     * @param message 
     */
    static void exitWithException(String message) {
        System.err.println(message);
        System.exit(0);
    }

    /**
     * Validate command line args
     * @param args 
     */
    static void validateArgs(Map<String, String> args) {
        Iterator<String> it = args.keySet().iterator();
        boolean hasInput = false;
        while (it.hasNext()) {
            String arg = it.next();
            switch (arg) {
                case "input":
                    hasInput = true;
                    break;
                case "output":
                    break;
                case "help":
                    printUsage();
                    break;
                default:
                    try {
                        int val = Integer.parseInt(args.get(arg));
                        if (val < 1) {
                            exitWithException(arg + " must be greater than zero");
                        }
                    } catch (NumberFormatException ex) {
                        exitWithException(arg + " must be an integer");
                    }
                    break;
            }
        }
        if (!hasInput) {
            printUsage();
            exitWithException("No input file provided");            
        }
    }
}
