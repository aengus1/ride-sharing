/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ridesharing.model;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Set;


/**
 *
 * @author aengusmccullough
 */
public class Vehicle {
    private int vehicleId;
    
    private final HashMap<Integer,TripRequest> servicing;
    private Route route;
    int capacity;

    public Vehicle(int vehicleId,AllPairsShortestPathMatrix matrix, int capacity){
        this.vehicleId = vehicleId;
        this.route = new Route(capacity);
        this.servicing = new HashMap<>();
        this.capacity = capacity;
    }
    /**
     * @return the vehicleId
     */
    public int getVehicleId() {
        return vehicleId;
    }

    /**
     * @param vehicleId the vehicleId to set
     */
    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    
    /**
     * @return the servicing
     */
    public HashMap<Integer,TripRequest> getServicing() {
        return servicing;
    }

    /**
     * @param trs
     */
    public void setServicing(Set<TripRequest> trs) {
        for (TripRequest svc : trs) {
            servicing.put(svc.getTripId(),svc);
        }
        
    }

    /**
     * @return the route
     */
    public Route getRoute() {
        return route;
    }

    /**
     * @param schedule the route to set
     */
    public void setRoute(Route schedule) {
        this.route = schedule;
    }
    
    
    public String getItinerary(){
        DateTimeFormatter df = DateTimeFormatter.ofPattern("H:mm");       
        StringBuilder sb = new StringBuilder();
        sb.append("Objective: ").append(getRoute().getObjective()).append("\n");
        for (Point pt : this.getRoute().getSchedule()) {
            sb.append(df.format(pt.getServiceTime())).append(" ")
                    .append((pt.isSource()?"Pickup ": "Dropoff "))
                    .append(servicing.get(pt.getTripId()).getRequester())
                    .append((pt.isSource()?" from ": " at "))
                    .append(pt.getTripId()).append("_").append(pt.isSource()?"S":"D")
                    .append(pt.isSource()?"(Earliest pickup "+df.format(pt.getEarliest()):
                            "(Latest arrival "+df.format(pt.getLatest())).append(")")
                    .append("\n");            
        }
        //System.out.println(sb.toString());
        return sb.toString();
    }
    
}
