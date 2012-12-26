package scaladci
package examples
import collection.mutable

object Dijkstra2 extends App {

  // DATA ####################################################################

  case class Node(name: Char)
  case class Graph(data: Map[Node, Map[Node, Int]])

  // CONTEXT #################################################################

  class Dijkstra(graph: Graph, origin: Node, destination: Node) extends Context {

    private val tentDist  = mutable.HashMap[Node, Int]()
    private val unvisited = mutable.Set[Node]()
    private var previous  = mutable.HashMap[Node, Node]()
    private val allNodes  = graph.as[AllNodes]

    def getShortestPath = {
      allNodes.assignInfiniteTentativeDistances()
      allNodes.markAsUnvisited()
      while (unvisited.nonEmpty) {
        val currentNode = allNodes.closestUnvisited
        allNodes.unvisitedNeighborsOf(currentNode).foreach(_.as[Neighbor].considerVia(currentNode))
        unvisited.remove(currentNode)
      }
      pathTo(destination).reverse
    }

    def pathTo(n: Node): List[Node] = if (n == origin) List(n) else n :: pathTo(previous(n))

    // ROLES #################################################################

    private trait AllNodes {graph: Graph =>
      def nodes = graph.data.keys
      def assignInfiniteTentativeDistances() {
        tentDist += origin -> 0
        nodes.filter(_ != origin).foreach(tentDist += _ -> Int.MaxValue / 4)
      }
      def markAsUnvisited() { unvisited ++= nodes }
      def unvisitedNeighborsOf(n: Node) = graph.data(n).filter(n => unvisited.contains(n._1)).keys
      def distanceBetween(current: Node, neighbor: Node) = graph.data(current)(neighbor)
      def closestUnvisited = unvisited.reduce((a, b) => if (tentDist(a) < tentDist(b)) a else b)
    }

    private trait Neighbor {self: Node =>
      def considerVia(currentNode: Node) {
        val newTentDistance = tentDist(currentNode) + allNodes.distanceBetween(currentNode, self)
        if (newTentDistance < tentDist(self)) {
          tentDist.update(self, newTentDistance)
          previous.put(self, currentNode)
        }
      }
    }
  }

  // TEST ####################################################################

  object manhattan {
    val ns                          = ('a' to 'i').map(Node(_)).toList
    val (a, b, c, d, e, f, g, h, i) = (ns(0), ns(1), ns(2), ns(3), ns(4), ns(5), ns(6), ns(7), ns(8))

    //    a - 2 - b - 3 - c
    //    |       |       |
    //    1       2       1
    //    |       |       |
    //    d - 1 - e - 1 - f
    //    |               |
    //    2               4
    //    |               |
    //    g - 1 - h - 2 - i

    def grid = Graph(Map(
      a -> Map(b -> 2, d -> 1), // Comment this line and uncomment next line to see another shortest path
      //      a -> Map(b -> 2, d -> 4),
      b -> Map(c -> 3, e -> 2),
      c -> Map(f -> 1),
      d -> Map(e -> 1, g -> 2),
      e -> Map(f -> 1),
      f -> Map(i -> 4),
      g -> Map(h -> 1),
      h -> Map(i -> 2),
      i -> Map()
    ))
    def origin = a
    def destination = i
  }

  val path = new Dijkstra(manhattan.grid, manhattan.origin, manhattan.destination).getShortestPath
  println("Shortest path: " + path.map(_.name).mkString(" -> "))
  // Shortest path: a -> d -> g -> h -> i
}