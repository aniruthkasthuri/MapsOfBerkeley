import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Comparator;
import java.util.Arrays;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     *
     * @param g       The graph to use.
     * @param stlon   The longitude of the start location.
     * @param stlat   The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat, double destlon, double destlat) {
        long closestStart = g.closest(stlon,stlat);
        long closestEnd = g.closest(destlon,destlat);
        Router rout = new Router();
        return rout.DijkstraSP(g,closestStart,closestEnd);
        //return edgeTo.get(closestEnd); // FIXME
    }

    private PriorityQueue<Long> pq;
    private HashMap<Long, Double> prdist;
    HashMap<Long,Double> distance;
    HashMap<Long,ArrayList<Long>> edgeTo;
    HashMap<GraphDB.Nodes,GraphDB.Edges> connex;
    public List<Long> DijkstraSP(GraphDB G, long start, long end)
    {
        Comparator<Long> comparator = new DoubleComparator();
        distance = new HashMap<>();
        int totalSize = G.adj.keySet().size();

        pq = new PriorityQueue<>(comparator);
        prdist = new HashMap<>();
        edgeTo = new HashMap<>();
        for (long x: G.idr.keySet()) {
            distance.put(x,Double.POSITIVE_INFINITY);
        }
        distance.put(start,0.0);
        ArrayList<Long> st = new ArrayList<>();
        st.add(start);
        edgeTo.put(start,st);
        pq.add(start);
        while (!pq.isEmpty()) {
            relax(G, pq.poll(), end);
            if (edgeTo.containsKey(end)){
                break;
            }
        }
        return edgeTo.get(end);
    }
    private void relax(GraphDB G, long v, long end)
    {
        for(Long y : G.adj.get(G.idr.get(v)))
        {
            if (edgeTo.containsKey(end)){
                break;
            }
            double w = G.distance(v,y);
            if (distance.get(y) > distance.get(v) + w){
                distance.put(y,distance.get(v) + w);
                prdist.put(y,distance.get(v) + w + G.distance(y,end));
                ArrayList<Long> adder = new ArrayList<>();
                for (Long x: edgeTo.get(v)){
                    adder.add(x);
                }
                adder.add(y);
                edgeTo.put(y,adder);
                if (!pq.contains(y)){
                    pq.add(y);
                }
            }
        }
    }
    public class DoubleComparator implements Comparator<Long>{
        @Override
        public int compare(Long x, Long y)
        {
            if (prdist.get(x) < prdist.get(y))
            {
                return -1;
            }
            if (prdist.get(x) > prdist.get(y))
            {
                return 1;
            }
            return 0;
        }
    }



    /**
     * Create the list of directions corresponding to a route on the graph.
     *
     * @param g     The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigatiionDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        int count = 0;
        ArrayList<NavigationDirection> direc = new ArrayList<>();
        String prevName = g.edger.get(route.get(0)*route.get(1)).name;
        NavigationDirection nd = new NavigationDirection();
        for (int x = 0; x < route.size()-1; x++){
            nd.direction = NavigationDirection.START;
            nd.distance = g.distance(route.get(x),route.get(x+1))+nd.distance;
            System.out.println(nd.distance);
              if (!g.edger.get(route.get(x)*route.get(x+1)).name.equals(prevName)){
                direc.add(count,nd);
                count++;
                prevName = g.edger.get(route.get(x)*route.get(x+1)).name;
                nd = new NavigationDirection();
            }
            nd.way = g.edger.get(route.get(x)*route.get(x+1)).name;
        }
        return direc;
    }


    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /**
         * Integer constants representing directions.
         */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /**
         * Number of directions supported.
         */
        public static final int NUM_DIRECTIONS = 8;

        /**
         * A mapping of integer values to directions.
         */
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /**
         * Default name for an unknown way.
         */
        public static final String UNKNOWN_ROAD = "unknown road";

        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /**
         * The direction a given NavigationDirection represents.
         */
        int direction;
        /**
         * The name of the way I represent.
         */
        String way;
        /**
         * The distance along this way I represent.
         */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         *
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                        && way.equals(((NavigationDirection) o).way)
                        && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}
