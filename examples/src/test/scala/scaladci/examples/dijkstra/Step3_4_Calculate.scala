package scaladci
package examples.dijkstra
import collection.mutable
/*
Now comes the meaty part of calculating the tentative distances:

  3.	For the CURRENT INTERSECTION, check its EASTERN and SOUTHERN NEIGHBOR INTERSECTIONS and calculate
      their TENTATIVE DISTANCES. For example, if the CURRENT INTERSECTION A is marked with a DISTANCE of 6,
      and the BLOCK connecting it with its EASTERN NEIGHBOR INTERSECTION B has length 2, then the distance
      to B (via A) will be 6 + 2 = 8. If this distance (8) is less than the previously recorded TENTATIVE
      DISTANCE of B, then overwrite that distance. Even though a neighbor intersection has been considered,
      it is not marked as a SHORTCUT at this time, and it remains in DETOURS.

  4.	When we are done considering the EASTERN and SOUTHERN NEIGHBOR INTERSECTIONS of the CURRENT
      INTERSECTION, remove the CURRENT INTERSECTION from DETOURS.
*/

object Step3_4_Calculate extends App {

  // Context ##################################################################

  @context
  class CalculateShortestPath(
    city: ManhattanGrid,
    currentIntersection: Intersection,
    destination: Intersection
  ) {

    private val tentativeDistances = mutable.HashMap[Intersection, Int]()
    private val detours            = mutable.Set[Intersection]()
    private val EastNeighbor       = city.nextDownTheStreet.get(currentIntersection)
    private val SouthNeighbor      = city.nextAlongTheAvenue.get(currentIntersection)
    private val shortcuts          = mutable.HashMap[Intersection, Intersection]()

    tentativeDistances.initialize
    detours.initialize

    println("Unvisited before:\n" + detours.toList.sortBy(_.name).mkString("\n"))

    currentIntersection.calculateTentativeDistanceOfNeighbors

    // Current intersection ('a') is no longer in the unvisited set:
    println("\nUnvisited after :\n" + detours.toList.sortBy(_.name).mkString("\n"))


    // Roles ##################################################################

    role tentativeDistances {
      def initialize() {
        tentativeDistances.put(currentIntersection, 0)
        city.intersections.filter(_ != currentIntersection).foreach(tentativeDistances.put(_, Int.MaxValue / 4))
      }
    }

    role detours {
      def initialize() { detours ++= city.intersections }
    }

    role currentIntersection {
      def calculateTentativeDistanceOfNeighbors() {

        // STEP 3 in the algorithm
        // The foreach call is only executed if we have a neighbor (Intersection 'c' doesn't have an eastern neighbor)
        EastNeighbor.foreach(updateNeighborDistance(_))
        SouthNeighbor.foreach(updateNeighborDistance(_))

        // STEP 4 in the algorithm (included in this version since it's just a one-liner)
        detours.remove(currentIntersection)
      }

      // example of "internal role method". it could have been private if we wanted to prevent access from the Context.
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

  // Execution ##########################################################
  val startingPoint = ManhattanGrid().a
  val destination   = ManhattanGrid().i
  val shortestPath  = new CalculateShortestPath(ManhattanGrid(), startingPoint, destination)
}