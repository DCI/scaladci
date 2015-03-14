package scaladci
package examples.dijkstra
import collection.mutable

/*
Now comes the recursive part since we want to calculate tentative distances to neighbor Intersections until we
can determine the shortest path to our destination.

The data we had as fields in the Context before, we now change to constructor arguments with default values. On each
recursion we pass those values on to the next nested Context object. We are not creating any new objects apart from the
Context itself!

Our recursion ends when we have either reached our destination or there are no more unvisited nodes

From the city Map Model description:

Continue this process of updating the NEIGHBORing intersections with the shortest distances, then marking the
CURRENT INTERSECTION as visited and moving onto the closest UNVISITED INTERSECTION until you have marked the
DESTINATION as visited. Once you have marked the DESTINATION as visited (as is the case with any visited
intersection) you have determined the SHORTEST PATH to it, from the STARTING POINT, and can trace your way back,
following the arrows in reverse.

From the Manhattan street model algorithm:

  5.	If the DESTINATION intersection is no longer in DETOURS, then stop. The algorithm has finished.

  6.	Select the intersection in DETOURS with the smallest TENTATIVE DISTANCE, and set it as the new
      CURRENT INTERSECTION then go back to step 3.
*/

object Step5_6_Recurse extends App {

  // Context ##################################################################

  @context
  class Dijkstra(
    city: ManhattanGrid,
    currentIntersection: Intersection,
    destination: Intersection,
    tentativeDistances: mutable.HashMap[Intersection, Int] = mutable.HashMap[Intersection, Int](),
    detours: mutable.Set[Intersection] = mutable.Set[Intersection](),
    shortcuts: mutable.HashMap[Intersection, Intersection] = mutable.HashMap[Intersection, Intersection]()
  ) {

    // Since we recurse now, we only want those to be initialized the first time
    if (tentativeDistances.isEmpty) {
      tentativeDistances.initialize
      detours.initialize
    }

    // Main part of algorithm
    currentIntersection.calculateTentativeDistanceOfNeighbors

    // Try to run this version to watch identities change and how tentative distances calculates...
    println(s"\n==============================")
    println(s"This context is new: " + this.hashCode())
    println(s"Intersection 'a' is the same all the way: " + city.a.hashCode())
    println(s"\nCurrent $currentIntersection     East ${city.eastNeighbor.getOrElse("...............")}")
    println(s"South   ${city.southNeighbor.getOrElse("...............")}")
    println("\n" + tentativeDistances.mkString("\n"))

    // STEP 5 - If we haven't found a good route to destination yet, we need to check more intersections...
    if (detours.contains(destination)) {

      // STEP 6 - Select Intersection with smallest tentative distance remaining unconsidered intersections
      val nextCurrent = detours.withSmallestTentativeDistance

      // STEP 3 REPEATED HERE - Recurse until we reach destination
      new Dijkstra(city, nextCurrent, destination, tentativeDistances, detours, shortcuts)
    }

    // Context helper methods

    // Get the (reversed) shortest path from the starting point to destination
    def shortestPath = pathTo(destination).reverse

    // Recursively compound the shortcuts going from destination backwards to the starting point
    def pathTo(x: Intersection): List[Intersection] = {
      if (!shortcuts.contains(x))
        List(x)
      else
        x :: pathTo(shortcuts(x))
    }


    // Roles ##################################################################

    role tentativeDistances {
      def initialize() {
        tentativeDistances.put(currentIntersection, 0)
        city.intersections.filter(_ != currentIntersection).foreach(tentativeDistances.put(_, Int.MaxValue / 4))
      }
    }

    role detours {
      def initialize() { detours ++= city.intersections }
      def withSmallestTentativeDistance = { detours.reduce((x, y) => if (tentativeDistances(x) < tentativeDistances(y)) x else y) }
    }

    role currentIntersection {
      def calculateTentativeDistanceOfNeighbors() {
        city.eastNeighbor.foreach(updateNeighborDistance(_))
        city.southNeighbor.foreach(updateNeighborDistance(_))
        detours.remove(currentIntersection)
      }
      def updateNeighborDistance(neighborIntersection: Intersection) {
        if (detours.contains(neighborIntersection)) {
          val newTentDistanceToNeighbor = currentDistance + lengthOfBlockTo(neighborIntersection)
          val currentTentDistToNeighbor = tentativeDistances(neighborIntersection)
          if (newTentDistanceToNeighbor < currentTentDistToNeighbor) {
            tentativeDistances.update(neighborIntersection, newTentDistanceToNeighbor)
            shortcuts.put(neighborIntersection, currentIntersection)
          }
        }
      }
      def currentDistance = tentativeDistances(currentIntersection)
      def lengthOfBlockTo(neighbor: Intersection) = city.distanceBetween(currentIntersection, neighbor)
    }

    role city {
      def distanceBetween(from: Intersection, to: Intersection) = city.blockLengths(Block(from, to))
      def eastNeighbor = city.nextDownTheStreet.get(currentIntersection)
      def southNeighbor = city.nextAlongTheAvenue.get(currentIntersection)
    }
  }

  // Data #####################################################################
  case class Intersection(name: Char)
  case class Block(x: Intersection, y: Intersection)

  // Test data ################################################################
  case class ManhattanGrid() {
    val intersections               = ('a' to 'i').map(Intersection(_)).toList
    val (a, b, c, d, e, f, g, h, i) = (intersections(0), intersections(1), intersections(2), intersections(3), intersections(4), intersections(5), intersections(6), intersections(7), intersections(8))
    val nextDownTheStreet           = Map(a -> b, b -> c, d -> e, e -> f, g -> h, h -> i)
    val nextAlongTheAvenue          = Map(a -> d, b -> e, c -> f, d -> g, f -> i)
    val blockLengths                = Map(Block(a, b) -> 2, Block(b, c) -> 3, Block(c, f) -> 1, Block(f, i) -> 4, Block(b, e) -> 2, Block(e, f) -> 1, Block(a, d) -> 1, Block(d, g) -> 2, Block(g, h) -> 1, Block(h, i) -> 2, Block(d, e) -> 1)

    //    a - 2 - b - 3 - c
    //    |       |       |
    //    1       2       1
    //    |       |       |
    //    d - 1 - e - 1 - f
    //    |               |
    //    2               4
    //    |               |
    //    g - 1 - h - 2 - i
  }

  val startingPoint = ManhattanGrid().a
  val destination   = ManhattanGrid().i
  val shortestPath  = new Dijkstra(ManhattanGrid(), startingPoint, destination).shortestPath
  println(shortestPath.map(_.name).mkString(" -> "))
  // a -> d -> g -> h -> i
}