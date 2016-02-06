package ridesharing.model;

import java.time.LocalTime;

/**
 * Structure for a pickup / dropoff Node on the graph
 * @author aengusmccullough
 */
public class Node {
    
    private boolean source;  //flag to indicate whether this is a source or dest node
    private int tripId;     //the trip id this node originates from
    private int xCoord;     
    private int yCoord;
    private LocalTime earliest;     //for source node this will be given, for dest it will be computed
    private LocalTime latest;       //for dest node this will be given, for source it will be computed

    
    public Node(){
        
    }
    
    public Node(Node copy){
        this.source = copy.source;
        this.earliest = copy.earliest;
        this.tripId = copy.tripId;
        this.xCoord = copy.xCoord;
        this.yCoord = copy.yCoord;
        this.latest = copy.latest;
    }
    
    /**
     * Gives distance (km) between two points.  This also gives minutes between points
     * as average speed is given as 60km/h -> 1km/minute     
     * x,y coordinates are given in pixels, where there are 5 pixels per km
     * @param another Node 
     * @return distance double (km)
     */
    public double distanceTo(Node another){
        return Math.sqrt(Math.pow(another.getxCoord() - this.getxCoord(), 2) + Math.pow(another.getyCoord() - this.getyCoord(), 2)) / 5;
    }
    
    /**
     * is this a source or a destination node?
     * @return boolean source=true,dest=false
     */
    public boolean isSource() {
        return source;
    }

    /**
     * @param source boolean source=true,dest=false
     */
    public void setSource(boolean source) {
        this.source = source;
    }

    /**
     * @return the tripId
     */
    public int getTripId() {
        return tripId;
    }

    /**
     * @param tripId the tripId to set
     */
    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    /**
     * @return int the xCoord in pixels
     */
    public int getxCoord() {
        return xCoord;
    }

    /**
     * @param xCoord int the xCoord to set (in pixels)
     */
    public void setxCoord(int xCoord) {
        this.xCoord = xCoord;
    }

    /**
     * @return int the yCoord in pixels
     */
    public int getyCoord() {
        return yCoord;
    }

    /**
     * @param yCoord the yCoord to set (in pixels)
     */
    public void setyCoord(int yCoord) {
        this.yCoord = yCoord;
    }

    /**
     * @return LocalTime the earliest
     */
    public LocalTime getEarliest() {
        return earliest;
    }

    /**
     * @param  earliest LocalTime 
     */
    public void setEarliest(LocalTime earliest) {
        this.earliest = earliest;
    }

    /**
     * @return LocalTime the latest time at this node
     */
    public LocalTime getLatest() {
        return latest;
    }

    /**
     * @param latest LocalTime the latest to set
     */
    public void setLatest(LocalTime latest) {
        this.latest = latest;
    }
    
    
    
}
