/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ridesharing.model;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Structure representing the route taken by a vehicle
 * @author aengusmccullough
 */
public class Route {

    private List<Point> schedule;       
    private int capacity;    
    private double objective;
    
    public Route(int capacity) {
        this.schedule = new ArrayList<>();        
        this.capacity = capacity;
    }

    /**
     * copy constructor
     * @param another 
     */
    public Route(Route another){
        this.capacity = another.capacity;
        this.objective = another.objective;
        this.schedule = new ArrayList<>();
        for (Point p : schedule) {
            Point pn = new Point(p);
            this.schedule.add(pn);
        }
    }
    /**
     * Append a point to the end of the route without updating load/service time or performing any checks
     * @param pt Point
     */     
    public void append(Point pt) {
        this.getSchedule().add(pt);
    }

    /**
     * Append a point at the end of this route
     * @param another
     * @param matrix 
     */
    public void appendRoute(Route another,AllPairsShortestPathMatrix matrix){
        for (Point pt : another.getSchedule()) {
            append(pt);
        }
        update(matrix);
    }
    /**
     * Remove a trip request at given indices
     * @param srcIdx  int index of source point
     * @param destIdx int index of dest point
     */
    public void remove(int srcIdx, int destIdx){
        this.getSchedule().remove(srcIdx);
        this.getSchedule().remove(destIdx);
        //recalculate service time and load for all points after insertion
        for (int i = srcIdx; i < getSchedule().size(); i++) {
            if(i==0){
                getSchedule().get(i).setServiceTime(getSchedule().get(i).getEarliest());
                getSchedule().get(i).setLoad(1);
            }else{                
                LocalTime timeToI = getSchedule().get(i-1).getServiceTime().plusMinutes((long)Math.ceil(getSchedule().get(i).distanceTo(getSchedule().get(i))));
                if(timeToI.isBefore(getSchedule().get(i).getEarliest())){
                    getSchedule().get(i).setServiceTime(getSchedule().get(i).getEarliest());
                }else{
                    getSchedule().get(i).setServiceTime(timeToI);
                }                
                getSchedule().get(i).setLoad(getSchedule().get(i).isSource()?getSchedule().get(i-1).getLoad()+1:getSchedule().get(i-1).getLoad()-1);                
                                                
            }                        
        }
    }
    
    
    
    /**
     * Insert a trip request at given indices
     * @param aIdx
     * @param bIdx
     * @param tr
     * @return 
     */
    public boolean insert(int aIdx,int bIdx,TripRequest tr){
        //insert the trip into schedule        
        this.getSchedule().add(aIdx, new Point(tr.getSource()));
        this.getSchedule().add(bIdx+1,new Point(tr.getDestination()));
        //recalculate service time and load for all points after insertion
        for (int i = aIdx; i < getSchedule().size(); i++) {
            if(i==0){
                getSchedule().get(i).setServiceTime(getSchedule().get(i).getEarliest());
                getSchedule().get(i).setLoad(1);
            }else{                
                LocalTime timeToI = getSchedule().get(i-1).getServiceTime().plusMinutes((long)Math.ceil(getSchedule().get(i-1).distanceTo(getSchedule().get(i))));
                if(timeToI.isBefore(getSchedule().get(i).getEarliest())){
                    getSchedule().get(i).setServiceTime(getSchedule().get(i).getEarliest());
                }else{
                    getSchedule().get(i).setServiceTime(timeToI);
                }
                if(getSchedule().get(i).getServiceTime().isAfter(getSchedule().get(i).getLatest())){
                    remove(aIdx,bIdx);
                    return false;
                }
                getSchedule().get(i).setLoad(getSchedule().get(i).isSource()?getSchedule().get(i-1).getLoad()+1:getSchedule().get(i-1).getLoad()-1);                
                if(getSchedule().get(i).getLoad()>getCapacity()){
                    remove(aIdx,bIdx);
                    return false;
                }                                
            }                        
        }
        return true;           
    }


    /**
     * Check if route can be traversed while meeting constraints
     *
     * @param nodes
     * @param matrix
     * @param capacity
     * @return
     */
    public static boolean canTraverse(List<Point> nodes, AllPairsShortestPathMatrix matrix, int capacity) {
        boolean res = false;
        if (nodes.isEmpty()) {
            return res;
        }

        LocalTime serviceTime = nodes.get(0).getEarliest();
        int load = 1;
        for (int i = 1; i < nodes.size(); i++) {
            load = nodes.get(i).isSource() ? load + 1 : load - 1;
            if (load > capacity) {
                return false;
            }
            serviceTime = serviceTime.plusMinutes((int) Math.ceil(matrix.getTravelTime(nodes.get(i - 1), nodes.get(i))));

            if (serviceTime.isBefore(nodes.get(i).getEarliest())) {
                //wait                
                serviceTime = nodes.get(i).getEarliest();
            }
            if (serviceTime.isAfter(nodes.get(i).getLatest())) {
                return false;
            }            
        }
        return true;
    }
    
    /**
     * Calculates the objective for this route.     
     * @param matrix AllPairsShortestPathMatrix
     * @param alpha  double constant
     * @return objective double
     */
    public double calculateObjective(AllPairsShortestPathMatrix matrix,double alpha){  
        if(!Route.canTraverse(this.getSchedule(), matrix, capacity)){
            return 0;
        }else{
            this.update(matrix);
        }
       double totalCost =0;
       double sharedCost = 0;
       double privateCost = 0;
       Point src = null;
       Point dest = null;
        for (int i = 0; i < getSchedule().size(); i++) {        //for every trip request served by this route
            if(getSchedule().get(i).isSource()){
                src = getSchedule().get(i);
                int j = i;
                int tripId = getSchedule().get(i).getTripId();
                do{     //sum shared cost for each edge until this trip's destination is reached
                    i++;
                    sharedCost += (getSchedule().get(i-1).distanceTo(getSchedule().get(i)) / (getSchedule().get(i-1).getLoad()));                    
                    
                }while(getSchedule().get(i).getTripId()!=tripId);
                dest = getSchedule().get(i);
                privateCost = matrix.getTravelTime(src, dest);
                totalCost+= 1 - alpha*(sharedCost/privateCost);
                sharedCost = privateCost = 0;                                      
                i = j;
            }                        
        }
        setObjective(totalCost);
        return totalCost;
    }
    
    /**
     * Traverses route and updates load and service time
     *     
     * @param matrix AllPairsShortestPathMatrix         
     */
    public  void update(AllPairsShortestPathMatrix matrix) {
        if(!Route.canTraverse(getSchedule(),matrix,capacity)){
            return;
        }        
        if (getSchedule().isEmpty()) {
            return;
        }

        LocalTime serviceTime = getSchedule().get(0).getEarliest();
        int load = 1;
        for (int i = 1; i < getSchedule().size(); i++) {
            load = getSchedule().get(i).isSource() ? load + 1 : load - 1;
            if (load > capacity) {
                return;
            }
            serviceTime = serviceTime.plusMinutes((int) Math.ceil(matrix.getTravelTime(getSchedule().get(i - 1), getSchedule().get(i))));

            if (serviceTime.isBefore(getSchedule().get(i).getEarliest())) {
                //wait                
                serviceTime = getSchedule().get(i).getEarliest();
            }
            if (serviceTime.isAfter(getSchedule().get(i).getLatest())) {
                return;
            }
            getSchedule().get(i).setServiceTime(serviceTime);
            getSchedule().get(i).setLoad(load);
        }        
    }


    /**
     * @return the schedule
     */
    public List<Point> getSchedule() {
        return schedule;
    }

    /**
     * @param schedule the schedule to set
     */
    public void setSchedule(List<Point> schedule) {
        this.schedule = schedule;
    }

    /**
     * @return the capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * @param capacity the capacity to set
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * @return the objective
     */
    public double getObjective() {
        return objective;
    }

    /**
     * @param objective the objective to set
     */
    public void setObjective(double objective) {
        this.objective = objective;
    }

}
