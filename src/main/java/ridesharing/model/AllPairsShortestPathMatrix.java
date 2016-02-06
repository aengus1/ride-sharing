package ridesharing.model;

import java.util.Collections;
import java.util.List;

/**
 * Structure to store travel times between all nodes
 * @author aengusmccullough
 */
public class AllPairsShortestPathMatrix {

    private final double[][] matrix;
    public static final int ORIGIN = 0;
    public static final int DEST = 1;

    /**
     * Construct from list of trip requests
     * @param tripRequests List<TripRequest> input data
     */
    public AllPairsShortestPathMatrix(List<TripRequest> tripRequests) {

        //sort the trip requests by id, because we will use this to lookup 
        Collections.sort(tripRequests);

        //size the matrix to contain a source and destination point for each trip
        //e.g.          |trip1src|trip2src|trip1dest|trip2dest
        // trip1src     |       0
        // trip2src     |               0   
        // trip1dest    |                       0
        // trip2dest    |                               0
        matrix = new double[tripRequests.size() * 2][tripRequests.size() * 2];
        
        int row = 0;
        int col = 0;
        for (row = 0; row < matrix.length / 2; row++) {
            //1st quadrant -> origin/origin
            for (col = 0; col < matrix[row].length / 2; col++) {
                matrix[row][col] = tripRequests.get(row).getSource().distanceTo(tripRequests.get(col).getSource());
            }
            //2nd quadrant -> origin/dest            
            for (; col < matrix[row].length; col++) {
                matrix[row][col] = tripRequests.get(row).getSource().distanceTo(tripRequests.get(col - (matrix.length / 2)).getDestination());
            }
        }

        for (; row < matrix.length; row++) {
            //3rd quadrant dest/origin
            for (col = 0; col < matrix[row].length / 2; col++) {
                matrix[row][col] = tripRequests.get(row - (matrix.length / 2)).getDestination().distanceTo(tripRequests.get(col).getSource());
            }
            //4th quadrant  dest/dest
            for (; col < matrix[row].length; col++) {
                matrix[row][col] = tripRequests.get(row - (matrix.length / 2)).getDestination().distanceTo(tripRequests.get(col - (matrix.length / 2)).getDestination());
            }
        }
    }
    
    
    /**
     * Returns the distance between two nodes in km (this is also the travel time in minutes assuming average speed of 60km/h)
     * @param a Node
     * @param b Node
     * @return double km or mins to destination
     */
    public double getTravelTime(Node a, Node b){
        return getTravelTime(a.getTripId(), a.isSource()?ORIGIN:DEST, b.getTripId(), b.isSource()?ORIGIN:DEST);
    }
    
    
    /**
     * Private method to retrieve the trip request
     * @param idTripA int tripid
     * @param originDestTripA int origin/dest
     * @param idTripB int tripid
     * @param originDestTripB int origin/dest
     * @return  double traveltime (mins) or distance (km)
     */
    private double getTravelTime(int idTripA,int originDestTripA,int idTripB,int originDestTripB) {
        //zero based array
        idTripA--;
        idTripB--;  
        
        if(originDestTripA==ORIGIN && originDestTripB==ORIGIN){            
            return matrix[idTripA][idTripB];
        }
        if(originDestTripA==ORIGIN && originDestTripB==DEST){                        
            return matrix[idTripA][(matrix.length/2)+idTripB];
            
        }
        if(originDestTripA==DEST && originDestTripB==ORIGIN){            
            return matrix[(matrix.length/2)+idTripA][idTripB];
        }
        if(originDestTripA==DEST && originDestTripB==DEST){            
            return matrix[(matrix.length/2)+idTripA][(matrix.length/2)+idTripB];                    
        }
        return 0;
    }

}
