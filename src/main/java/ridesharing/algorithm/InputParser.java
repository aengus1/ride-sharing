/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ridesharing.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import ridesharing.model.Node;
import ridesharing.model.TripRequest;

/**
 *
 * @author aengusmccullough
 */
public class InputParser {
    
     /**
     * Reads the input file and turns it into a list of trip requests Input file
     * format: header (line 1) : number of rows subsequent lines (tab
     * separated): requester | tripid | depart after | arrive before | x1 | y1 |
     * x2 | y2
     *
     * @param f File input file
     * @return List<TripRequest> trip requests
     * @throws IOException
     */
    public static List<TripRequest> parseInputFile(File f) throws IOException{
    
        DateTimeFormatter df = DateTimeFormatter.ofPattern("H:mm");        
        List<TripRequest> tripRequests = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(f))) {
            String line = "";

            //1.  read first line to get number of records
            int nRecords = 0;
            if ((line = bufferedReader.readLine()) != null) {
                try {
                    nRecords = Integer.parseInt(line);
                } catch (NumberFormatException ex) {
                    throw new IOException("Error parsing file.  Expected number of records on first line");
                }
            } else {
                throw new IOException("Error parsing file.  First line is empty");
            }
            
            //read each line...
            int i = 0;
            while ((line = bufferedReader.readLine()) != null) {
                i++;
                String[] values = line.split("\t");
                if (values.length != 8) {
                    throw new IOException("Error parsing file.  Expected 8 values on line " + i);
                }
                TripRequest tr = new TripRequest();
                Node source = new Node();
                source.setSource(true);
                Node dest = new Node();
                dest.setSource(false);
                
                tr.setRequester(values[0]);
                try {
                    tr.setTripId(Integer.parseInt(values[1]));
                    source.setTripId(tr.getTripId());                
                    dest.setTripId(tr.getTripId());
                } catch (NumberFormatException ex) {
                    throw new IOException("Error parsing file.  Expected numeric trip identifier on line " + i);
                }
                
                try {
                    LocalTime departAfter = LocalTime.parse(values[2], df);
                    source.setEarliest(departAfter);
                    
                } catch (DateTimeParseException ex) {                    
                        throw new IOException("Error parsing file.  Depart after time must be in HH:mm format on line " + i);
                    }
                
                try {
                    LocalTime arriveBefore = LocalTime.parse(values[3], df);
                    dest.setLatest(arriveBefore);
                } catch (DateTimeParseException ex) {                    
                        throw new IOException("Error parsing file.  Arrive before time must be in HH:mm format on line " + i);
                    }

                
                try {
                    int originX = Integer.parseInt(values[4]);
                    int originY = Integer.parseInt(values[5]);
                    source.setxCoord(originX);
                    source.setyCoord(originY);                    
                } catch (NumberFormatException ex) {
                    throw new IOException("Error parsing file. Origin X Y Coordinates must be integers on line " + i);
                }

                try {
                    int destX = Integer.parseInt(values[6]);
                    int destY = Integer.parseInt(values[7]);
                    dest.setxCoord(destX);
                    dest.setyCoord(destY);                    
                } catch (NumberFormatException ex) {
                    throw new IOException("Error parsing file. Destination X Y Coordinates must be integers on line " + i);
                }
                //add nodes to trip request
                tr.setSource(source);
                tr.setDestination(dest);
                //add trip request to list
                tripRequests.add(tr);
                
                if (i >= nRecords) {
                    break;
                }
            }
        }
        return tripRequests;
    }
    
    
}
