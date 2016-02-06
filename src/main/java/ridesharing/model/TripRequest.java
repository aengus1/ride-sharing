/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ridesharing.model;

/**
 *
 * @author aengusmccullough
 */
public class TripRequest implements Comparable<TripRequest>{
    
    private Node source;    //source node
    private Node destination;   //destination node
    private String requester;   //name of requester
    private int tripId;         //unique identifier for this trip
    private double delay;       //variable used in heuristic
    
    public double travelTime(AllPairsShortestPathMatrix matrix){
        return matrix.getTravelTime(source, destination);
    }
    /**
     * @return the source
     */
    public Node getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(Node source) {
        this.source = source;
    }

    /**
     * @return the destination
     */
    public Node getDestination() {
        return destination;
    }

    /**
     * @param destination the destination to set
     */
    public void setDestination(Node destination) {
        this.destination = destination;
    }

    /**
     * @return the requester
     */
    public String getRequester() {
        return requester;
    }

    /**
     * @param requester the requester to set
     */
    public void setRequester(String requester) {
        this.requester = requester;
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

    @Override
    public int compareTo(TripRequest o) {
        return new Integer(this.getTripId()).compareTo(o.getTripId());
    }
    
   

    /**
     * @return the delay
     */
    public double getDelay() {
        return delay;
    }

    /**
     * @param delay the delay to set
     */
    public void setDelay(double delay) {
        this.delay = delay;
    }
}
