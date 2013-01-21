//package common;
//
//import ch.maxant.dci.util.Context;
//import ch.maxant.dci.util.Data;
//import ch.maxant.dci.util.Role;
//
//import java.util.*;
//
///**
// * here, I have removed need to use a type declaration for a merged interface.
// * there is no merged interface.  I only ever refer to objects by their actual data
// * type.
// *
// * Oh, and I have solved the object schizophrenia problem too.
// */
//public class Runner
//{
//    static class Pair
//    {
//        private Object a;
//        private Object b;
//
//        public Pair( Object a, Object b )
//        {
//            this.a = a;
//            this.b = b;
//        }
//
//        @Override
//        public int hashCode()
//        {
//            final int prime = 31;
//            int result = 1;
//            result = prime * result + ( ( a == null ) ? 0 : a.hashCode() );
//            result = prime * result + ( ( b == null ) ? 0 : b.hashCode() );
//            return result;
//        }
//
//        @Override
//        public boolean equals( Object obj )
//        {
//            if (this == obj)
//                return true;
//            if (obj == null)
//                return false;
//            if (getClass() != obj.getClass())
//                return false;
//            Pair other = (Pair) obj;
//            if (a == null)
//            {
//                if (other.a != null)
//                    return false;
//            }
//            else if (!a.equals( other.a ))
//                return false;
//            if (b == null)
//            {
//                if (other.b != null)
//                    return false;
//            }
//            else if (!b.equals( other.b ))
//                return false;
//            return true;
//        }
//
//        @Override
//        public String toString()
//        {
//            return "Pair [" + a + ", " + b + "]";
//        }
//
//    }
//
//    ////////////////////////////////////////////////////////////////
//
//    /**
//     * use less than infinity, otherwise some of the calcs go wrong,
//     * when we add tentative distances to actual distances and we end
//     * up with negative numbers, because weve gone over max INT.
//     */
//    static final Integer INFINITY = Integer.MAX_VALUE - 10000;
//
//    ////////////////////////////////////////////////////////////////
//
//    /**
//     * "Map" as in cartography rather than Computer Science...
//     *
//     * Map is technically a role from the DCI perspective. The role
//     * in this example is played by an object representing a particular
//     * Manhattan geometry
//     */
//    static class CartographyMap extends Role<Geometry, Object>
//    {
//
//        Integer distance_between( Node a, Node b )
//        {
//            return self.getDistances().get( new Pair( a, b ) );
//        }
//
//        // These two functions presume always travelling
//        // in a southern or easterly direction
//
//        Node next_down_the_street_from( Node node )
//        {
//            return self.east_neighbor_of( node );
//        }
//
//        Node next_along_the_avenue_from( Node node )
//        {
//            return self.south_neighborOf( node );
//        }
//
//    }
//
//    ////////////////////////////////////////////////////////////////
//
//    /**
//     * There are four roles in the algorithm: CurrentIntersection (@current)
//     * EastNeighbor, which lies DIRECTLY to the east of CurrentIntersection
//     * (@east_neighbor) SouthernNeighbor, which is DIRECTLy to its south
//     * (@south_neighbor) Destination, the target node (@destination)
//     *
//     * We also add a role of Map (@map) as the oracle for the geometry
//     *
//     * The algorithm is straight from Wikipedia:
//     *
//     * http://en.wikipedia.org/wiki/Dijkstra's_algorithm
//     *
//     * and reads directly from the distance method, below
//     *
//     * (use context type "Object" because this role is found in several contexts -
//     * we still need to think how to handle duplicate code in a better way...)
//     */
//    static class Distance_labeled_graph_node extends Role<Node, Object>
//    {
//
//        /*
//        * NOTE: This role creates a new data member in the node into
//        *        which it is injected. An alernative implementation would
//        *        be to use a separate associative array
//        */
//
//        void set_tentative_distance_to( Integer x )
//        {
//            self.setData( "tentative_distance", x );
//        }
//    }
//
//    ////////////////////////////////////////////////////////////////
//
//    /**
//     * Consider street corners on a Manhattan grid. We want to find the
//     * minimal path from the most northeast city to the most
//     * southeast city. Use Dijstra's algorithm
//     *
//     * Data class
//     *
//     * Note there is NO NEED to implement hashCode or equals!
//     */
//    static class Node extends Data
//    {
//
//        private String name;
//
//        public Node( String name )
//        {
//            this.name = name;
//        }
//
//        public String getName()
//        {
//            return name;
//        }
//
//        /**
//         * only done for debugging purposes
//         */
//        @Override
//        public String toString()
//        {
//            return "Node[name=" + name + ", hashCode=" + hashCode() + "]";
//        }
//    }
//
//    ////////////////////////////////////////////////////////////////
//
//    /**
//     * This is the main Context for shortest path calculation
//     */
//    static class CalculateShortestPath extends Context
//    {
//
//        //These are handles to internal housekeeping arrays set up in initialize
//
//        Map<Node, Boolean> unvisited = new HashMap<Node, Boolean>();
//        Map<Node, Node> pathTo;
//        Node east_neighbor;
//        Node south_neighbor;
//        List<Node> path;
//        Geometry map;
//        Node current;
//        Node destination;
//
//        // Initialization
//
//        void rebind( Node origin_node, Geometry geometries )
//        {
//            current = origin_node;
//            map = geometries;
//
//            bind( map, CartographyMap.class );
//
//            bind( current, CurrentIntersection.class );
//
//            east_neighbor = map.east_neighbor_of( origin_node );
//
//            for (Node n : geometries.nodes())
//            {
//                bind( n, Distance_labeled_graph_node.class );
//            }
//
//            if (east_neighbor != null)
//            {
//                bind( east_neighbor, Neighbor.class );
//            }
//
//            south_neighbor = map.south_neighborOf( origin_node );
//
//            if (south_neighbor != null)
//            {
//                bind( south_neighbor, Neighbor.class );
//            }
//        }
//
//        /**
//         * public initialize. It's overloaded so that the public version doesn't
//         * have to pass a lot of crap; the initialize method takes care of
//         * setting up internal data structures on the first invocation. On
//         * recursion we override the defaults
//         */
//        public CalculateShortestPath( Node origin_node, Node target_node, Geometry geometries, List<Node> path_vector,
//                                      Map<Node, Boolean> unvisited_hash, Map<Node, Node> pathto_hash )
//        {
//
//            destination = target_node;
//
//            rebind( origin_node, geometries );
//
//            // This has to come after rebind is done
//            if (path_vector == null)
//            {
//
//                // This is the fundamental data structure for Dijkstra's algorithm,
//                // called
//                // "Q" in the Wikipedia description. It is a boolean hash that maps
//                // a
//                // node onto false or true according to whether it has been visited
//                this.unvisited = new HashMap<Node, Boolean>();
//
//                // These initializations are directly from the description of the
//                // algorithm
//                for (Node n : geometries.getNodes())
//                {
//                    this.unvisited.put( n, Boolean.TRUE );
//                    n.call( "set_tentative_distance_to", Void.class, INFINITY );
//                }
//
//                current.call( "set_tentative_distance_to", Void.class, 0 );
//
//                this.unvisited.remove( origin_node );
//
//                // The path array is kept in the outermost context and serves to
//                // store the
//                // return path. Each recurring context may add something to the
//                // array along
//                // the way. However, because of the nature of the algorithm,
//                // individual
//                // Context instances don't deliver "partial paths" as partial
//                // answers.
//                this.path = new ArrayList<Node>();
//
//                // The pathTo map is a local associative array that remembers the
//                // arrows between nodes through the array and erases them if we
//                // re-label a node with a shorter distance
//                this.pathTo = new HashMap<Node, Node>();
//
//            }
//            else
//            {
//
//                this.unvisited = unvisited_hash;
//                this.path = path_vector;
//                this.pathTo = pathto_hash;
//            }
//
//            execute();
//        }
//
//        class CurrentIntersection extends Role<Node, CalculateShortestPath>
//        {
//
//            List<Node> unvisited_neighbors()
//            {
//
//                //WATCHOUT: moved the access to data from the context, from outside this method,
//                //to in inside it, otherwise we are introducing state to the role
//                Map<Node, Boolean> unvisited = context.unvisited;
//                Node south_neighbor = context.south_neighbor;
//                Node east_neighbor = context.east_neighbor;
//
//                List<Node> retval = new ArrayList<Node>();
//                if (south_neighbor != null)
//                {
//                    Boolean addIt = unvisited.get( south_neighbor );
//                    if (addIt == Boolean.TRUE)
//                    { //watch out, addIt can be null apparently
//                        retval.add( south_neighbor );
//                    }
//                }
//                if (east_neighbor != null)
//                {
//                    Boolean addIt = unvisited.get( east_neighbor );
//                    if (addIt == Boolean.TRUE)
//                    { //watch out, addIt can be null apparently
//                        retval.add( east_neighbor );
//                    }
//
//                }
//                return retval;
//            }
//        }
//
//        /**
//         * This module serves to provide the methods both for the east_neighbor and south_neighbor roles
//         */
//        class Neighbor extends Role<Node, CalculateShortestPath>
//        {
//
//            boolean relable_node_as( Integer x )
//            {
//                if (x < (Integer) self.getData( "tentative_distance" ))
//                {
//
//                    self.call( "set_tentative_distance_to", Void.class, x );
//                    return true;
//                }
//                else
//                {
//                    return false;
//                }
//            }
//        }
//
//        /**
//         * This is the method that does the work. Called from initialize
//         */
//        public void execute()
//        {
//            // Calculate tentative distances of unvisited neighbors
//            List<Node> unvisited_neighbors = current.call( "unvisited_neighbors", List.class );
//            if (unvisited_neighbors != null)
//            {
//                for (Node neighbor : unvisited_neighbors)
//                {
//
//                    Integer tentativeDistance = (Integer) current.getData( "tentative_distance" );
//                    Integer distanceBetween = map.call( "distance_between", Integer.class, current, neighbor );
//                    boolean relable_node_as = neighbor.call( "relable_node_as", Boolean.class, tentativeDistance + distanceBetween );
//
//                    if (relable_node_as)
//                    {
//                        pathTo.put( neighbor, current );
//                    }
//                }
//            }
//
//            unvisited.remove( current );
//
//            // Are we done?
//
//            if (unvisited.size() == 0)
//            {
//                save_path( this.path );
//            }
//            else
//            {
//
//                // The next current node is the one with the least distance in the
//                // unvisited set
//
//                Node selection = nearest_unvisited_node_to_target();
//
//                // Recur
//                new CalculateShortestPath( selection, destination, map, path, unvisited, pathTo );
//            }
//        }
//
//        Node nearest_unvisited_node_to_target()
//        {
//
//            int min = INFINITY;
//            Node selection = null;
//
//            for (Node intersection : unvisited.keySet())
//            {
//                if (unvisited.get( intersection ))
//                {
//
//                    if (intersection.getData( "tentative_distance", Integer.class ) <= min)
//                    {
//
//                        min = intersection.getData( "tentative_distance", Integer.class );
//                        selection = intersection;
//                    }
//                }
//            }
//            return selection;
//        }
//
//        /**
//         * This method does a simple traversal of the data structures (following
//         * pathTo) to build the directed traversal vector for the minimum path
//         */
//        void save_path( List<Node> pathVector )
//        {
//
//            Node node = destination;
//            do
//            {
//                pathVector.add( node );
//
//                node = pathTo.get( node );
//
//            }
//            while (node != null);
//        }
//
//        public List<Node> getPath()
//        {
//            return path;
//        }
//
//    }
//
//    ////////////////////////////////////////////////////////////////
//
//
//    /**
//     * This is the main Context for shortest distance calculation
//     */
//    static class CalculateShortestDistance extends Context
//    {
//
//        List<Node> path = new ArrayList<Node>();
//        Geometry map;
//        Node destination;
//        Node current;
//
//        //MAP ROLE: SEE COMMON CODE NEAR TOP
//        //DISTANCE LABELED GRAPH NODE: SEE COMMON CODE NEAR TOP
//
//        void rebind( Node origin_node, Geometry geometries )
//        {
//            current = origin_node;
//            destination = geometries.destination();
//            map = geometries;
//
//            bind( map, CartographyMap.class );
//
//            for (Node node : map.nodes())
//            {
//                bind( node, Distance_labeled_graph_node.class );
//            }
//        }
//
//        public CalculateShortestDistance( Node origin_node, Node target_node, Geometry geometries )
//        {
//
//            rebind( origin_node, geometries );
//
//            this.current.call( "set_tentative_distance_to", Integer.class, 0 );
//
//            this.path = new CalculateShortestPath( this.current, this.destination, geometries, null, null, null ).getPath();
//
//            cleanup(); //related to context stacking
//        }
//
//        public int distance()
//        {
//            int retval = 0;
//            Node previous_node = null;
//
//            List<Node> reversed = new ArrayList<Node>( path );
//            Collections.reverse( reversed );
//
//            for (Node node : reversed)
//            {
//
//                if (previous_node == null)
//                {
//                    retval = 0;
//                }
//                else
//                {
//                    retval += this.map.call( "distance_between", Integer.class, previous_node, node );
//                }
//                previous_node = node;
//            }
//            return retval;
//        }
//    }
//
//    ////////////////////////////////////////////////////////////////
//
//    //because Java is not entirely dynamic like Ruby, I have created a
//    //super class to contain the common parts of the geometries.  it also
//    //helps reduce code duplication.
//    static abstract class Geometry extends Data
//    {
//        List<Node> nodes;
//        Node root;
//        Node destination;
//        Map<Pair, Integer> distances;
//        Map<Node, Node> next_down_the_street_from = new HashMap<Node, Node>();
//        Map<Node, Node> next_along_the_avenue_from = new HashMap<Node, Node>();
//
//        public Node east_neighbor_of( Node a )
//        {
//            return next_down_the_street_from.get( a );
//        }
//        public Node south_neighborOf( Node a )
//        {
//            return next_along_the_avenue_from.get( a );
//        }
//        public Node root()
//        {
//            return root;
//        }
//        public Node destination()
//        {
//            return destination;
//        }
//        public List<Node> nodes()
//        {
//            return nodes;
//        }
//        public Node getRoot()
//        {
//            return root;
//        }
//        public Node getDestination()
//        {
//            return destination;
//        }
//        public List<Node> getNodes()
//        {
//            return nodes;
//        }
//        public Map<Pair, Integer> getDistances()
//        {
//            return distances;
//        }
//    }
//
//    ////////////////////////////////////////////////////////////////
//
//    static class ManhattanGeometry1 extends Geometry
//    {
//
//        public ManhattanGeometry1()
//        {
//            this.nodes = new ArrayList<Node>();
//            this.distances = new HashMap<Pair, Integer>();
//
//            String[] names = {"a", "b", "c", "d", "a", "b", "g", "h", "i"};
//
//            for (int i = 0; i < 3; i++)
//            {
//                for (int j = 0; j < 3; j++)
//                {
//                    this.nodes.add( new Node( names[( i * 3 ) + j] ) );
//                }
//            }
//
//            // Aliases to help set up the grid. Grid is of Manhattan form:
//            //
//            //    a - 2 - b - 3 - c
//            //    |       |       |
//            //    1       2       1
//            //    |       |       |
//            //    d - 1 - e - 1 - f
//            //    |               |
//            //    2               4
//            //    |               |
//            //    g - 1 - h - 2 - i
//            //
//            Node a = this.nodes.get( 0 );
//            root = a;
//            Node b = this.nodes.get( 1 );
//            Node c = this.nodes.get( 2 );
//            Node d = this.nodes.get( 3 );
//            Node e = this.nodes.get( 4 );
//            Node f = this.nodes.get( 5 );
//            Node g = this.nodes.get( 6 );
//            Node h = this.nodes.get( 7 );
//            Node i = this.nodes.get( 8 );
//            destination = i;
//
//            for (int s = 0; s < 3; s++)
//            {
//                for (int t = 0; t < 3; t++)
//                {
//                    this.distances.put( new Pair( nodes.get( s ), nodes.get( t ) ), INFINITY );
//                }
//            }
//
//            distances.put( new Pair( a, b ), 2 );
//            distances.put( new Pair( b, c ), 3 );
//            distances.put( new Pair( c, f ), 1 );
//            distances.put( new Pair( f, i ), 4 );
//            distances.put( new Pair( b, e ), 2 );
//            distances.put( new Pair( e, f ), 1 );
//            distances.put( new Pair( a, d ), 1 );
//            distances.put( new Pair( d, g ), 2 );
//            distances.put( new Pair( g, h ), 1 );
//            distances.put( new Pair( h, i ), 2 );
//            distances.put( new Pair( d, e ), 1 );
//
//            distances = Collections.unmodifiableMap( distances );
//
//            next_down_the_street_from.put( a, b );
//            next_down_the_street_from.put( b, c );
//            next_down_the_street_from.put( d, e );
//            next_down_the_street_from.put( e, f );
//            next_down_the_street_from.put( g, h );
//            next_down_the_street_from.put( h, i );
//            next_down_the_street_from = Collections.unmodifiableMap( next_down_the_street_from );
//
//            next_along_the_avenue_from.put( a, d );
//            next_along_the_avenue_from.put( b, e );
//            next_along_the_avenue_from.put( c, f );
//            next_along_the_avenue_from.put( d, g );
//            next_along_the_avenue_from.put( f, i );
//
//            next_along_the_avenue_from = Collections.unmodifiableMap( next_along_the_avenue_from );
//        }
//
//    }
//
//    ////////////////////////////////////////////////////////////////
//
//    static class ManhattanGeometry2 extends Geometry
//    {
//
//        public ManhattanGeometry2()
//        {
//            this.nodes = new ArrayList<Node>();
//            this.distances = new HashMap<Pair, Integer>();
//
//            String[] names = {"a", "b", "c", "d", "a", "b", "g", "h", "i", "j", "k"};
//
//            for (int j = 0; j < 11; j++)
//            {
//                nodes.add( new Node( names[j] ) );
//            }
//
//            // Aliases to help set up the grid. Grid is of Manhattan form:
//            //
//            // a - 2 - b - 3 - c - 1 - j
//            // |       |       |       |
//            // 1       2       1       |
//            // |       |       |       |
//            // d - 1 - e - 1 - f       1
//            // |       |               |
//            // 2       4               |
//            // |       |               |
//            // g - 1 - h - 2 - i - 2 - k
//            //
//            Node a = nodes.get( 0 );
//            root = a;
//            Node b = nodes.get( 1 );
//            Node c = nodes.get( 2 );
//            Node d = nodes.get( 3 );
//            Node e = nodes.get( 4 );
//            Node f = nodes.get( 5 );
//            Node g = nodes.get( 6 );
//            Node h = nodes.get( 7 );
//            Node i = nodes.get( 8 );
//            Node j = nodes.get( 9 );
//            Node k = nodes.get( 10 );
//            destination = k;
//
//            for (int s = 0; s < 11; s++)
//            {
//                for (int t = 0; t < 11; t++)
//                {
//                    distances.put( new Pair( nodes.get( s ), nodes.get( t ) ), INFINITY );
//                }
//            }
//
//            distances.put( new Pair( a, b ), 2 );
//            distances.put( new Pair( b, c ), 3 );
//            distances.put( new Pair( c, f ), 1 );
//            distances.put( new Pair( f, i ), 4 );
//            distances.put( new Pair( b, e ), 2 );
//            distances.put( new Pair( e, f ), 1 );
//            distances.put( new Pair( a, d ), 1 );
//            distances.put( new Pair( d, g ), 2 );
//            distances.put( new Pair( g, h ), 1 );
//            distances.put( new Pair( h, i ), 2 );
//            distances.put( new Pair( d, e ), 1 );
//            distances.put( new Pair( c, j ), 1 );
//            distances.put( new Pair( j, k ), 1 );
//            distances.put( new Pair( i, k ), 2 );
//
//            distances = Collections.unmodifiableMap( distances );
//
//            next_down_the_street_from.put( a, b );
//            next_down_the_street_from.put( b, c );
//            next_down_the_street_from.put( c, j );
//            next_down_the_street_from.put( d, e );
//            next_down_the_street_from.put( e, f );
//            next_down_the_street_from.put( g, h );
//            next_down_the_street_from.put( h, i );
//            next_down_the_street_from.put( i, k );
//
//            next_down_the_street_from = Collections.unmodifiableMap( next_down_the_street_from );
//
//            next_along_the_avenue_from.put( a, d );
//            next_along_the_avenue_from.put( b, e );
//            next_along_the_avenue_from.put( c, f );
//            next_along_the_avenue_from.put( d, g );
//            next_along_the_avenue_from.put( f, i );
//            next_along_the_avenue_from.put( j, k );
//
//            next_along_the_avenue_from = Collections.unmodifiableMap( next_along_the_avenue_from );
//        }
//
//    }
//
//    ////////////////////////////////////////////////////////////////
//
//    /**
//     * Test drivers
//     */
//    public static void main( String[] args )
//    {
//
//        Geometry geometries = new ManhattanGeometry1();
//
//        CalculateShortestPath path = new CalculateShortestPath( geometries.getRoot(),
//                                                                geometries.getDestination(), geometries, null, null, null );
//
//        System.out.println( "Path is: " );
//        for (Node node : path.getPath())
//        {
//            System.out.println( node.getName() );
//        }
//
//        System.out.println( "distance is "
//                                  + new CalculateShortestDistance( geometries.getRoot(),
//                                                                   geometries.getDestination(), geometries ).distance() );
//
//        System.out.println();
//
//        geometries = new ManhattanGeometry2();
//
//        path = new CalculateShortestPath( geometries.getRoot(),
//                                          geometries.getDestination(), geometries, null, null, null );
//
//        System.out.println( "Path is: " );
//        Node last_node = null;
//        for (Node node : path.getPath())
//        {
//            if (last_node != null)
//            {
//                System.out.print( " - " + geometries.distances.get( new Pair( node, last_node ) ) + " - " );
//            }
//            System.out.print( node.getName() );
//            last_node = node;
//        }
//
//        System.out.println();
//        System.out.println( "distance is " + new CalculateShortestDistance( geometries.getRoot(),
//                                                                            geometries.getDestination(), geometries ).distance() );
//    }
//}