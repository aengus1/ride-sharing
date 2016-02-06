package ridesharing.model;

import java.time.LocalTime;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Point structure is a Node as used in a route.  Extends point with service time (actual time the node is serviced)
 * and load (number of passengers immediately after visiting this node)
 * @author aengusmccullough
 */
public class Point extends Node{
    
    private LocalTime serviceTime;  //actual time of service
    private int load;   //number of passengers immediately after service
    
    public Point(Node n){
        super(n);        
    }
    
    /**
     * copy constructor
     * @param another Point
     */
    public Point(Point another){
        super(another);
        this.load = another.load;
        this.serviceTime = another.serviceTime;
    }
    
    
    /**
     * @return the serviceTime
     */
    public LocalTime getServiceTime() {
        return serviceTime;
    }

    /**
     * @param serviceTime the serviceTime to set
     */
    public void setServiceTime(LocalTime serviceTime) {
        this.serviceTime = serviceTime;
    }

    /**
     * @return the load
     */
    public int getLoad() {
        return load;
    }

    /**
     * @param load the load to set
     */
    public void setLoad(int load) {
        this.load = load;
    }

}
