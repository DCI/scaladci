package scaladci
package examples.dijkstra
import collection.mutable
/*
Unvisited set...

  2.	Set the INITIAL INTERSECTION as CURRENT INTERSECTION.
      Create a set called DETOURS containing all intersections except the INITIAL INTERSECTION.
*/
object Step2_Unvisited extends App {

  // Context ##################################################################

  @context
  class CalculateShortestPath(
    city: ManhattanGrid,
    currentIntersection: Intersection,
    destination: Intersection
  ) {

    private val tentativeDistances = mutable.HashMap[Intersection, Int]()
    private val detours            = mutable.Set[Intersection]()

    tentativeDistances.assigntentativeDistances
    detours.markAsUnvisited

    // All Intersections will still be unvisited at this point
    println("Unvisited Intersections:\n" + detours.toList.sortBy(_.name).mkString("\n"))


    // Roles ##################################################################

    role tentativeDistances {
      def assigntentativeDistances() {
        tentativeDistances.put(currentIntersection, 0)
        city.intersections.filter(_ != currentIntersection).foreach(tentativeDistances.put(_, Int.MaxValue / 4))
      }
    }

    // A second role is added in a similar fashion to the first one
    role detours {

      // STEP 2 - All intersections are unvisited from the start so we simply copy the Intersections from the Grid
      def markAsUnvisited() {
        detours ++= city.intersections
      }
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