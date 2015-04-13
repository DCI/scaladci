package scaladci
package examples
import org.specs2.mutable._

import scala.collection.mutable

// DCI implementation of the Dijkstra algorithm
// Using role name as role identifier

class Dijkstra extends Specification {

  // Data
  case class Intersection(name: Char)
  case class Block(x: Intersection, y: Intersection)
  case class ManhattanGrid() {
    val intersections               = ('a' to 'i').map(Intersection).toList
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

  "Using Role name as a Role identifier" >> {

    @context
    class Dijkstra(
      city: ManhattanGrid,
      currentIntersection: Intersection,
      destination: Intersection,
      tentativeDistances: mutable.HashMap[Intersection, Int] = mutable.HashMap[Intersection, Int](),
      detours: mutable.Set[Intersection] = mutable.Set[Intersection](),
      shortcuts: mutable.HashMap[Intersection, Intersection] = mutable.HashMap[Intersection, Intersection]()
      ) {

      // Algorithm
      if (tentativeDistances.isEmpty) {
        tentativeDistances.initialize
        detours.initialize
      }
      currentIntersection.calculateTentativeDistanceOfNeighbors
      if (detours contains destination) {
        val nextCurrent = detours.withSmallestTentativeDistance
        new Dijkstra(city, nextCurrent, destination, tentativeDistances, detours, shortcuts)
      }

      // Context helper methods
      def pathTo(x: Intersection): List[Intersection] = if (!shortcuts.contains(x)) List(x) else x :: pathTo(shortcuts(x))
      def shortestPath = pathTo(destination).reverse

      // Roles

      role tentativeDistances {
        def initialize {
          tentativeDistances.put(currentIntersection, 0)
          city.intersections.filter(_ != currentIntersection).foreach(tentativeDistances.put(_, Int.MaxValue / 4))
        }
      }

      role detours {
        def initialize { detours ++= city.intersections }
        def withSmallestTentativeDistance = { detours.reduce((x, y) => if (tentativeDistances(x) < tentativeDistances(y)) x else y) }
      }

      role currentIntersection {
        def calculateTentativeDistanceOfNeighbors {
          city.eastNeighbor foreach updateNeighborDistance
          city.southNeighbor foreach updateNeighborDistance
          detours remove currentIntersection
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

    // Test
    val startingPoint = ManhattanGrid().a
    val destination = ManhattanGrid().i
    val shortestPath = new Dijkstra(ManhattanGrid(), startingPoint, destination).shortestPath
    shortestPath.map(_.name).mkString(" -> ") === "a -> d -> g -> h -> i"
  }
}