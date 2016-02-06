/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ridesharing.algorithm;

import java.time.temporal.ChronoUnit;
import static java.time.temporal.ChronoUnit.MINUTES;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import ridesharing.model.AllPairsShortestPathMatrix;
import ridesharing.model.Node;
import ridesharing.model.Point;
import ridesharing.model.Route;
import ridesharing.model.TripRequest;
import ridesharing.model.Vehicle;

/**
 * Ride sharing solver based on the GRASP heuristic 
 * (http://www.research.att.com/export/sites/att_labs/techdocs/TD_100315.pdf).
 * Attempts to minimize the number of vehicles needed to serve the trip requests, 
 * while minimizing the cost (distance travelled) per passenger.
 * @author aengusmccullough
 */
public class Solver {

    List<TripRequest> requests;
    AllPairsShortestPathMatrix matrix;
    static final double ALPHA = 0.99;
    final Comparator<TripRequest> earliestArrivalComparator = (TripRequest o1, TripRequest o2) -> o1.getSource().getEarliest().compareTo(o2.getSource().getEarliest());
    final Comparator<TripRequest> delayFComp = (TripRequest o1, TripRequest o2) -> (Double.compare(o1.getDelay(), o2.getDelay()));
    int beta;
    int gamma;
    int capacity;
    int nIterations;
    int nSearchIterations;

    /**
     * Constructor
     *
     * @param requests List<TripRequest>
     * @param matrix AllPairsShortestPathMatrix
     * @param beta int number of candidate insertion points to consider
     * @param gamma int degree of randomness in initial trip request selection
     * @param capacity vehicle capacity
     * @param nIterations number
     * @param nSearchIterations
     */
    public Solver(List<TripRequest> requests, AllPairsShortestPathMatrix matrix, int beta, int gamma, int capacity, int nIterations, int nSearchIterations) {
        this.requests = requests;
        this.matrix = matrix;
        requests.sort(earliestArrivalComparator);
        this.beta = beta;
        this.gamma = gamma;
        this.capacity = capacity;
        this.nIterations = nIterations;
        this.nSearchIterations = nSearchIterations;
    }

    /**
     * Run the algorithm
     * @return List<Vehicle> resulting list of vehicles and their correspondig schedule
     */
    public List<Vehicle> solve() {
        //compute the initial solution
        List<Vehicle> result = calcInitialSolution();
        //calculate the objective for initial solution
        double objective = Solver.sumObjectives(result);

        int i = 0;
        while (i++ < nIterations) {
            List<Vehicle> iter = calcInitialSolution();
            if (Solver.sumObjectives(iter) > objective) {
                result = iter;
                objective = Solver.sumObjectives(iter);
            }
            int j = 0;
            while (j++ < nSearchIterations) {
                List<Vehicle> improved = localSearch(result);
                if (Solver.sumObjectives(result) > objective) {
                    objective = Solver.sumObjectives(iter);
                    result = improved;
                }
            }
        }
        return result;
    }

    /**
     * Calculate the initial solution. Note this function will return different results as it
     * is randomized
     * @return List<Vehicle> resulting list of vehicles with corresponding itinerary
     */
    protected List<Vehicle> calcInitialSolution() {
        // initialization
        List<Vehicle> result = new ArrayList<>();
        Stack<TripRequest> unserved = new Stack();
        Stack<TripRequest> cantservice = new Stack();
        Collections.sort(requests, earliestArrivalComparator);
        Collections.reverse(requests);
        unserved.addAll(requests);

        int vehicleIdx = 0;
        Vehicle vehicle = new Vehicle(vehicleIdx, matrix, capacity);
        TripRequest tr = unserved.pop();
        vehicle.getServicing().put(tr.getTripId(), tr);

        Point src = new Point(tr.getSource());
        src.setLoad(1);
        src.setServiceTime(src.getEarliest());
        vehicle.getRoute().append(src);

        Point dest = new Point(tr.getDestination());
        dest.setLoad(0);
        dest.setServiceTime(dest.getEarliest());
        vehicle.getRoute().append(dest);

        result.add(vehicle);

        try {                 
            while (!unserved.isEmpty()) {

                //compute the greedy function for each trip request
                for (TripRequest us : unserved) {
                    Solver.greedyFunction(us, result.get(vehicleIdx).getRoute());
                }
                //sort by delay time
                unserved.sort(delayFComp);

                //sample from the top gamma % of results
                double topX = (unserved.size() / 100.0) * gamma;
                double v = Math.random() * topX;
                int random = (int) Math.ceil(v);
                TripRequest curr = unserved.get(random == 0 ? 0 : random - 1);

                //attempt to insert into current vehicle's schedule
                if (Solver.attemptInitialInsertion(matrix, vehicle.getRoute(), curr, ALPHA, beta, capacity)) {
                    vehicle.getServicing().put(curr.getTripId(), curr);
                    unserved.remove(curr);
                } else {
                    cantservice.push(curr);
                    unserved.remove(curr);
                }

                if (unserved.isEmpty()) {
                    if (cantservice.isEmpty()) {
                        return result;
                    }
                    unserved.addAll(cantservice);
                    cantservice.clear();

                    vehicle = new Vehicle(++vehicleIdx, matrix, capacity);
                    tr = unserved.pop();
                    vehicle.getServicing().put(tr.getTripId(), tr);
                    src = new Point(tr.getSource());
                    src.setLoad(1);
                    src.setServiceTime(src.getEarliest());
                    vehicle.getRoute().append(src);

                    dest = new Point(tr.getDestination());
                    dest.setLoad(0);
                    dest.setServiceTime(dest.getEarliest());
                    vehicle.getRoute().append(dest);

                    result.add(vehicle);
                }
            }
        } finally {            
            result = joinVehicleRoutes(result);
        }

        return result;
    }

    /**
     * Local search randomly selects two vehicles and attempts to swap a trip 
     * request from one to the other.  All feasible candidate insertion points are
     * considered and the one with the best improvement in objective is chosen
     * @param vehicles List<Vehicle>
     * @return  List<Vehicle>
     */
    protected List<Vehicle> localSearch(List<Vehicle> vehicles) {

        //randomly select two vehicles from the list
        int nVehicles = vehicles.size();
        Vehicle a = null;
        Vehicle b = null;
        if (nVehicles < 2) {
            return vehicles;
        } else if (nVehicles == 2) {
            a = vehicles.get(0);
            b = vehicles.get(1);
        } else {
            nVehicles--;
            int ax = 0, bx = 0;
            do {
                ax = (int) Math.ceil(nVehicles * Math.random());
                bx = (int) Math.ceil(nVehicles * Math.random());
            } while (ax != bx);
            a = vehicles.get(ax);
            b = vehicles.get(bx);
        }

        //randomly select  a trip request from each vehicle
        int randomA = (int) Math.ceil(Math.random() * a.getRoute().getSchedule().size() - 1);
        int randomB = (int) Math.ceil(Math.random() * b.getRoute().getSchedule().size() - 1);

        int tripRequestA = a.getRoute().getSchedule().get(randomA).getTripId();
        int tripRequestB = b.getRoute().getSchedule().get(randomB).getTripId();

        //clone routes before modifying
        Route routeA = new Route(a.getRoute());
        Route routeB = new Route(b.getRoute());

        //remove trip requests
        Solver.removeTripRequest(tripRequestA, routeA);
        Solver.removeTripRequest(tripRequestB, routeB);

        //attempt insertion of tripRequestA into routeB
        TripRequest trA = this.findTripRequest(tripRequestA);
        Route newRouteB = Solver.attemptSearchInsertion(matrix, routeB, trA, ALPHA, capacity);

        //attempt insertion of tripRequestB into routeA
        TripRequest trB = findTripRequest(tripRequestB);
        Route newRouteA = Solver.attemptSearchInsertion(matrix, routeA, trB, ALPHA, capacity);

        //insertion success 
        if (newRouteA != null && newRouteB != null) {
            //update routes
            a.setRoute(newRouteA);
            b.setRoute(newRouteB);
            //update vehicle
            a.getServicing().remove(tripRequestA);
            a.getServicing().put(tripRequestB, trB);
            b.getServicing().remove(tripRequestB);
            b.getServicing().put(tripRequestA, trA);
        }

        return joinVehicleRoutes(vehicles);
    }

    
    

    /**
     * Attempt to insert a trip request into a route (initial)
     * considers only beta candidate insertion points
     * @param matrix  AllPairsShortestPathMatrix
     * @param route Route
     * @param tr TripRequest
     * @param alpha constant
     * @param beta parameter (how many insertion points to try)
     * @param capacity vehicle capacity
     * @return boolean success/ failure
     */
    public static boolean attemptInitialInsertion(AllPairsShortestPathMatrix matrix, Route route, TripRequest tr, double alpha, int beta, int capacity) {

        Set<Solver.PtPair> feasiblePairs;

        //1. source points
        int[] s = Solver.getCandidateInsertionPoints(route, tr, beta, true);
        List<Solver.PtPair> srcPairs = getSrcPtPairs(route.getSchedule().size(), s);
        feasiblePairs = getFeasiblePairs(matrix, route, srcPairs, tr, alpha, capacity);

        //2. dest points
        s = Solver.getCandidateInsertionPoints(route, tr, beta, false);
        List<Solver.PtPair> destPairs = getDestPtPairs(route.getSchedule().size(), s);
        feasiblePairs.addAll(getFeasiblePairs(matrix, route, destPairs, tr, alpha, capacity));

        //randomly select a pair with probability proportional to value of objective function
        List<Solver.PtPair> fpairs = new ArrayList<>();
        fpairs.addAll(feasiblePairs);
        Collections.sort(fpairs);
        double topX = (fpairs.size() / 100.0) * beta;
        int random = (int) Math.ceil(topX * Math.random());
        if (!feasiblePairs.isEmpty()) {
            Solver.PtPair pair = fpairs.get(random == 0 ? 0 : random - 1);

            //attempt insertion, (updating serviceTime and load for all nodes after source insertion point)
            if (route.insert(pair.getA(), pair.getB(), tr)) {
                route.calculateObjective(matrix, alpha);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    /**
     * Attempt to insert a trip request into a route (search)
     * considers all feasible insertion points
     * @param matrix
     * @param route
     * @param tr
     * @param alpha
     * @param capacity
     * @return
     */
    public static Route attemptSearchInsertion(AllPairsShortestPathMatrix matrix, Route route, TripRequest tr, double alpha, int capacity) {

        Set<Solver.PtPair> feasiblePairs;

        //1. source points 
        int[] s = Solver.getCandidateInsertionPoints(route, tr, route.getSchedule().size(), true);
        List<Solver.PtPair> srcPairs = getSrcPtPairs(route.getSchedule().size(), s);
        feasiblePairs = getFeasiblePairs(matrix, route, srcPairs, tr, alpha, capacity);

        //2. dest points
        s = Solver.getCandidateInsertionPoints(route, tr, route.getSchedule().size(), false);
        List<Solver.PtPair> destPairs = getDestPtPairs(route.getSchedule().size(), s);
        feasiblePairs.addAll(getFeasiblePairs(matrix, route, destPairs, tr, alpha, capacity));

        
        // consider all feasible insertion points
        double bestObjective = route.getObjective();
        Route bestRoute = null;
        double objective = 0;
        for (PtPair pair : feasiblePairs) {
            Route clone = new Route(route);
            if (clone.insert(pair.getA(), pair.getB(), tr)) {
                objective = clone.calculateObjective(matrix, alpha);
                if (objective > bestObjective) {
                    bestObjective = objective;
                    bestRoute = clone;
                }
            }
        }
        if (bestRoute != null) {
            bestRoute.update(matrix);
        }

        return bestRoute;
    }

    
    /**
     * Checks if resulting routes can be linked together to reduce number of
     * vehicles
     *
     * @param input List<Vehicle> initial list
     * @return List<Vehicle> reduced list
     */
    private List<Vehicle> joinVehicleRoutes(List<Vehicle> input) {
        List<Integer> vehiclesforRemoval = new ArrayList<>();
        for (Vehicle v : input) {
            if (vehiclesforRemoval.contains(v.getVehicleId())) {
                continue;
            }
            Point vLast = v.getRoute().getSchedule().get(v.getRoute().getSchedule().size() - 1);
            Point vFirst = v.getRoute().getSchedule().get(0);
            for (int i = 0; i < input.size(); i++) {
                if (v.getVehicleId() == input.get(i).getVehicleId()) {
                    continue;
                }
                if (vehiclesforRemoval.contains(input.get(i).getVehicleId())) {
                    continue;
                }
                Point rLast = input.get(i).getRoute().getSchedule().get(input.get(i).getRoute().getSchedule().size() - 1);
                Point rFirst = input.get(i).getRoute().getSchedule().get(0);

                if (vLast.getServiceTime().isBefore(rFirst.getServiceTime())
                        && (matrix.getTravelTime(vLast, rFirst) <= (vLast.getLatest().until(rFirst.getServiceTime(), ChronoUnit.MINUTES)))) {
                    v.getRoute().appendRoute(input.get(i).getRoute(), matrix);
                    v.getServicing().putAll(input.get(i).getServicing());
                    vehiclesforRemoval.add(input.get(i).getVehicleId());
                    break;
                }
                if (rLast.getServiceTime().isBefore(vFirst.getServiceTime())
                        && (matrix.getTravelTime(rLast, vFirst) <= rLast.getLatest().until(vFirst.getServiceTime(), ChronoUnit.MINUTES))) {
                    input.get(i).getRoute().appendRoute(v.getRoute(), matrix);
                    input.get(i).getServicing().putAll(v.getServicing());
                    vehiclesforRemoval.add(v.getVehicleId());
                    break;
                }
            }
        }

        int idx = 0;
        for (Iterator<Vehicle> iter = input.listIterator(); iter.hasNext();) {
            Vehicle a = iter.next();
            if (vehiclesforRemoval.contains(a.getVehicleId())) {
                iter.remove();
            } else {
                a.setVehicleId(idx++);
                a.getRoute().calculateObjective(matrix, ALPHA);
            }
        }
        return input;
    }
    
    /**
     * Utility method to sum objectives for a list of vehicles
     * @param vehicles
     * @return 
     */
    
    private static double sumObjectives(List<Vehicle> vehicles) {
        double obj = 0;
        for (Vehicle vehicle : vehicles) {
            obj += vehicle.getRoute().getObjective();
        }
        return obj;
    }

    /**
     * Determines the 'best' positions to insert the source or destination point
     * of a triprequest into a route
     *
     * @param route Route
     * @param tr TripRequest
     * @param beta int how many candidates to return (higher=more optimal
     * solution, lower=quicker)
     * @param source boolean (source=true,destination =false)
     * @return int[] candidate insertion points
     */
    private static int[] getCandidateInsertionPoints(Route route, TripRequest tr, int beta, boolean source) {
        double[][] rDelay = new double[route.getSchedule().size()][2];  //[0] -> index in schedule [1]-> rDelay
        int i = -1;
        for (Point pt : route.getSchedule()) {
            rDelay[++i][0] = i;
            if (source) {
                rDelay[i][1] = Solver.calcSourcePointDelay(pt, tr.getSource());
            } else {
                rDelay[i][1] = Solver.calcDestPointDelay(pt, tr.getDestination());
            }
        }
        //sort on minimum delay
        java.util.Arrays.sort(rDelay, (double[] a, double[] b1) -> Double.compare(a[0], b1[0]));

        //s is a subset of r, containing (min of beta, schedule.size) candidate source insertion points
        int size = Math.min(route.getSchedule().size(), beta);
        int[] s = new int[size];
        for (int j = 0; j < size; j++) {
            s[j] = (int) rDelay[j][0];
        }
        return s;
    }

    /**
     * Greedy function for assessing the viability of inserting a trip
     * request into the route
     *
     * @param request
     * @param route
     * @return
     */
    private static double greedyFunction(TripRequest request, Route route) {

        double minSrc = Double.POSITIVE_INFINITY;
        double minDest = Double.POSITIVE_INFINITY;
        double src;
        double dest;
        for (Point schedule : route.getSchedule()) {
            src = Solver.calcSourcePointDelay(schedule, request.getSource());
            dest = Solver.calcDestPointDelay(schedule, request.getDestination());
            if (src < minSrc) {
                minSrc = src;
            }
            if (dest < minDest) {
                minDest = dest;
            }
        }
        request.setDelay(minSrc + minDest);
        return minSrc + minDest;
    }

    /**
     * Checks the feasibility of inserting trip request at each set of locations
     *
     * @param pairs List<PtPair> list of indexes to insert source and
     * destination points
     * @param tr TripRequest the trip request
     * @param alpha int alpha param
     * @return List feasible pairs
     */
    private static Set<Solver.PtPair> getFeasiblePairs(AllPairsShortestPathMatrix matrix, Route route, List<Solver.PtPair> pairs, TripRequest tr, double alpha, int capacity) {
        Set<Solver.PtPair> feasiblePairs = new HashSet<>();
        for (Solver.PtPair sp : pairs) {
            List<Point> tempSchedule = new ArrayList<>();
            tempSchedule.addAll(route.getSchedule());
            tempSchedule.add(sp.getB(), new Point(tr.getDestination()));
            tempSchedule.add(sp.getA(), new Point(tr.getSource()));
            if (Route.canTraverse(tempSchedule, matrix, capacity)) {
                sp.calculateObjectiveFunction(tempSchedule, alpha);
                feasiblePairs.add(sp);
            }
        }
        return feasiblePairs;
    }

    /**
     * Function used in heuristic to determine the delay
     *
     * @param pt
     * @param ptI
     * @return
     */
    public static double calcSourcePointDelay(Point pt, Node ptI) {
        double delay = Math.max(ptI.distanceTo(pt), pt.getServiceTime().until(ptI.getEarliest(), MINUTES));
        return delay;
    }

    /**
     * Function used in heuristic to determine the delay
     * @param pt
     * @param ptI
     * @return 
     */
    public static double calcDestPointDelay(Point pt, Node ptI) {
        if (pt.getServiceTime().plusMinutes((long) Math.ceil(ptI.distanceTo(pt))).isBefore(ptI.getLatest())) {
            return ptI.distanceTo(pt);
        } else {
            return Double.POSITIVE_INFINITY;
        }

    }

    /**
     * Get each pair of points (a,b) where a comes before b in r
     *
     * @param s int[] Subset of R (a)
     * @param rsize size of R (b)
     * @return
     */
    private static List<Solver.PtPair> getSrcPtPairs(int rsize, int[] s) {
        List<Solver.PtPair> pairs = new ArrayList<>();
        for (int i = 0; i < s.length; i++) {
            for (int j = 0; j < rsize; j++) {
                if (s[i] <= j) {
                    pairs.add(new Solver.PtPair(s[i], j));
                }

            }
        }
        return pairs;
    }

    /**
     * Get each pair of points (a,b) where a comes before b in r
     *
     * @param rsize R (a)
     * @param s int[] Subset of R (b)
     * @return
     */
    private static List<Solver.PtPair> getDestPtPairs(int rsize, int[] s) {
        List<Solver.PtPair> pairs = new ArrayList<>();
        for (int i = 0; i < s.length; i++) {
            for (int j = 0; j < rsize; j++) {
                if (j <= s[i]) {
                    pairs.add(new Solver.PtPair(j, s[i]));
                }
            }
        }
        return pairs;

    }
    
    
    /**
     * Utility method to find a trip request by id
     * @param tripId
     * @return 
     */
    
    private TripRequest findTripRequest(int tripId) {
        for (TripRequest request : requests) {
            if (request.getTripId() == tripId) {
                return request;
            }
        }
        return null;
    }

    /**
     * Utility method to remove a trip request from a route
     * @param tripId
     * @param route 
     */
    private static void removeTripRequest(int tripId, Route route) {
        int src = -1, dest = -1;
        for (int i = 0; i < route.getSchedule().size(); i++) {
            if (tripId == route.getSchedule().get(i).getTripId()) {
                if (route.getSchedule().get(i).isSource()) {
                    src = i;
                } else {
                    dest = i;
                }
                if (src > -1 && dest > -1) {
                    break;
                }
            }
        }
        if (src != -1 && dest != -1) {
            route.remove(src, dest);
        }
    }
    

    /**
     * Static class used to store insertion points and objective associated with
     */
    private static class PtPair implements Comparable<PtPair> {

        private int a;
        private int b;
        private double objective;

        PtPair(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object another) {
            if (another instanceof PtPair) {
                return this.getA() == ((PtPair) another).getA() && this.getB() == ((PtPair) another).getB();
            }
            return false;

        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + this.a;
            hash = 67 * hash + this.b;
            return hash;
        }

        /**
         * @return the a
         */
        public int getA() {
            return a;
        }

        /**
         * @param a the a to set
         */
        public void setA(int a) {
            this.a = a;
        }

        /**
         * @return the b
         */
        public int getB() {
            return b;
        }

        /**
         * @param b the b to set
         */
        public void setB(int b) {
            this.b = b;
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

        /**
         * Determine objective of inserting this pair into schedule represented by routes
         * @param pts
         * @param alpha
         * @return 
         */
        public double calculateObjectiveFunction(List<Point> pts, double alpha) {
            int load = 0;
            //update load (n passengers) at each point
            for (Point pt : pts) {
                if (pt.isSource()) {
                    pt.setLoad(++load);
                }
                if (!pt.isSource()) {
                    pt.setLoad(--load);
                }
            }
            //
            double cost = 0;
            for (int j = this.getA(); j < this.getB() - 1; j++) {
                cost += (pts.get(j).distanceTo(pts.get(j + 1)) / (pts.get(j).getLoad()));
            }

            double obj = 1 - alpha * (cost / pts.get(this.getA()).distanceTo(pts.get(this.getB())));
            this.setObjective(obj);
            return obj;
        }

        @Override
        public int compareTo(PtPair o) {
            return Double.compare(this.objective, o.objective);
        }

    }
}
