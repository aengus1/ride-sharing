# ride-sharing
Routing / Scheduling algorithm for ridesharing problem

# 1.0	Introduction
Given a set of people that need to travel within a geographic region on the same day, this algorithm attempts to satisfy each trip request using as few vehicles as possible while minimizing the cost per trip for each passenger, based on the distance travelled.
# 2.0	Detail
Provide an input file (example provided in src/main/resources) that details each trip request in the following format:
Requester | Trip ID | Depart After | Arrive Before | x1 | y1 | x2 | y2
Input coordinates are provided in pixels, assuming 5 pixels per kilometer and an average travel speed of 60 km/h.
A GRASP heuristic (http://www.research.att.com/export/sites/att_labs/techdocs/TD_100315.pdf) is used to search for feasible solutions. 


# 3.0	Installation
Requires JDK 8.0 and maven to build.

mvn install

java –jar target/ride-sharing.jar <path_to_input_file>

Options:
--outputfile path_to_output_file
--capacity passenger_capacity_of_vehicle
--i  number_of_iterations
--s number_of_search_iterations
--beta number_of_candidate_insertion_points_to_consider
--gamma level_of_randomization_in_trip_request_selection
--help print_usage

