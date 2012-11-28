package scaladci
package examples

import collection.mutable.{HashMap, ListBuffer}
import collection.mutable

object Dijkstra extends App {

  // GLOBAL BOILERPLATE ######################################################

  case class Pair(n1: Node, n2: Node)
  val infinity = Int.MaxValue / 4

  // DATA ####################################################################

  case class Node(name: Char)

  // CONTEXT #################################################################

  class CalculateShortestPath(origin_node: Node,
                              target_node: Node,
                              geometries: ManhattanGeometry,
                              path_vector: ListBuffer[Node] = null,
                              unvisited_hash: HashMap[Node, Boolean] = null,
                              pathTo_hash: HashMap[Node, Node] = null,
                              tent_dist_hash: HashMap[Node, Int] = null) extends Context {

    // Initialization
    private var unvisited      = new mutable.HashMap[Node, Boolean]()
    private var pathTo         = new mutable.HashMap[Node, Node]()
    private var tent_dist      = new mutable.HashMap[Node, Int]()
    private var path           = new ListBuffer[Node]()
    private val destination    = target_node
    private val current        = origin_node.as[CurrentIntersection]
    private val cartographyMap = geometries.as[CartographyMap]
    private var east_neighbor  = cartographyMap.east_neighbor_of(origin_node).map(_.as[Neighbor])
    private var south_neighbor = cartographyMap.south_neighborOf(origin_node).map(_.as[Neighbor])

    if (path_vector == null) {
      geometries.nodes.foreach {
        case some_node if some_node == origin_node =>
          tent_dist.put(origin_node, 0)
        case other_node => {
          unvisited.put(other_node, true)
          tent_dist.put(other_node, infinity)
        }
      }
    } else {
      unvisited = unvisited_hash
      path = path_vector
      pathTo = pathTo_hash
      tent_dist = tent_dist_hash
    }

    execute()

    // ROLES ###############################################################

    private trait CurrentIntersection {self: Node =>
      def unvisited_neighbors() = {
        val retval = ListBuffer[Node with Neighbor]()
        south_neighbor.foreach(retval += _)
        east_neighbor.foreach(retval += _)
        retval
      }
    }

    private trait Neighbor {self: Node =>
      def relable_node_as(x: Int) = {
        if (x < tent_dist.get(self).get) {
          tent_dist.update(self, x)
          true
        } else {
          false
        }
      }
    }

    private trait CartographyMap {self: ManhattanGeometry =>
      def distance_between(a: Node, b: Node) = distances.get(Pair(a, b))
    }

    // CONTEXT METHODS #######################################################

    def execute() {
      current.unvisited_neighbors().foreach {
        neighbor: Node with Neighbor =>
          val tentativeDistance: Int = tent_dist(current)
          val distanceBetween = cartographyMap.distance_between(current, neighbor).get
          val relable_node_as = neighbor.relable_node_as(tentativeDistance + distanceBetween)
          if (relable_node_as)
            pathTo.put(neighbor, current)
      }

      unvisited.remove(current)

      if (unvisited.size > 0) {
        val selection = nearest_unvisited_node_to_target

        // Recur
        new CalculateShortestPath(selection, destination, cartographyMap, path, unvisited, pathTo, tent_dist)
      }
    }

    def nearest_unvisited_node_to_target = {
      var min: Int = infinity
      var selection: Node = null
      for (intersection <- unvisited.keySet) {
        if (unvisited.get(intersection).get) {
          if (tent_dist.get(intersection).get <= min) {
            min = tent_dist.get(intersection).get
            selection = intersection
          }
        }
      }
      selection
    }

    def getPath = {
      def walkBackwards(path_so_far: List[Node], x: Node): List[Node] = {
        pathTo.get(x) match {
          case None => x :: path_so_far
          case Some(y) => walkBackwards(x :: path_so_far, y)
        }
      }
      walkBackwards(List(), destination)
    }
  }

  // TEST DATA ###############################################################

  case class ManhattanGeometry() {
    val nodes                       = ('a' to 'i').map(Node(_)).toList
    val (a, b, c, d, e, f, g, h, i) = (nodes(0), nodes(1), nodes(2), nodes(3), nodes(4), nodes(5), nodes(6), nodes(7), nodes(8))
    val root                        = a
    val destination                 = i

    //    a - 2 - b - 3 - c
    //    |       |       |
    //    1       2       1
    //    |       |       |
    //    d - 1 - e - 1 - f
    //    |               |
    //    2               4
    //    |               |
    //    g - 1 - h - 2 - i

    val distances = (for {
      char1 <- 'a' to 'i' // Outer loop
      char2 <- 'a' to 'i' // Inner loop
    } yield (char1, char2) match {
        case ('a', 'b') => (Pair(a, b), 2)
        case ('b', 'c') => (Pair(b, c), 3)
        case ('c', 'f') => (Pair(c, f), 1)
        case ('f', 'i') => (Pair(f, i), 4)
        case ('b', 'e') => (Pair(b, e), 2)
        case ('e', 'f') => (Pair(e, f), 1)
        case ('a', 'd') => (Pair(a, d), 1) // Comment this line and uncomment next line to see another shortest path
        //        case ('a', 'd') => (Pair(a, d), 4)
        case ('d', 'g') => (Pair(d, g), 2)
        case ('g', 'h') => (Pair(g, h), 1)
        case ('h', 'i') => (Pair(h, i), 2)
        case ('d', 'e') => (Pair(d, e), 1)

        // Give any other node combination infinite distance
        case _ => (Pair(nodes(char1.toInt - 97), nodes(char2.toInt - 97)), infinity)
      }).toMap

    val next_down_the_street_from  = Map(a -> b, b -> c, d -> e, e -> f, g -> h, h -> i)
    val next_along_the_avenue_from = Map(a -> d, b -> e, c -> f, d -> g, f -> i)

    def east_neighbor_of(a: Node) = next_down_the_street_from.get(a)
    def south_neighborOf(a: Node) = next_along_the_avenue_from.get(a)
  }

  // TEST DRIVE ##################################################################

  val geometries = ManhattanGeometry()
  val path       = new CalculateShortestPath(geometries.root, geometries.destination, geometries)
  println("Shortest path: " + path.getPath.map(x => x.name).mkString(" -> "))
  // Shortest path: a -> d -> g -> h -> i
}