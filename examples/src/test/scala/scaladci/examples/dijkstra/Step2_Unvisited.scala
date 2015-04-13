package scaladci
package examples.dijkstra
import scala.collection.mutable

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

    // todo: infer type from right hand side expression
    private val tentativeDistances: mutable.HashMap[Intersection, Int] = mutable.HashMap[Intersection, Int]()
    private val detours           : mutable.Set[Intersection]          = mutable.Set[Intersection]()

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

  // Execution ##########################################################
  val startingPoint = ManhattanGrid().a
  val destination   = ManhattanGrid().i
  val shortestPath  = new CalculateShortestPath(ManhattanGrid(), startingPoint, destination)
}