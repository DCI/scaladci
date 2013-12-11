//package scaladci.javascript
//
//import org.jscala._
//import scaladci.context
//import scaladci.dci._
//import scala.collection.mutable
//
//// Can't compile this one yet because of missing features of JScala...
//
//object Dijkstra extends App {
//
//  @context // Scala DCI -> Scala vanilla
//  @Javascript // Scala vanilla -> Javascript
//  class Dijkstra(
//    City: ManhattanGrid,
//    CurrentIntersection: Intersection,
//    Destination: Intersection,
//    TentativeDistances: mutable.HashMap[Intersection, Int] = mutable.HashMap[Intersection, Int](),
//    Detours: mutable.Set[Intersection] = mutable.Set[Intersection](),
//    shortcuts: mutable.HashMap[Intersection, Intersection] = mutable.HashMap[Intersection, Intersection]()
//    ) {
//
//    // Algorithm
//    if (TentativeDistances.isEmpty) {
//      TentativeDistances.initialize
//      Detours.initialize
//    }
//    CurrentIntersection.calculateTentativeDistanceOfNeighbors
//    if (Detours.contains(Destination)) {
//      val nextCurrent = Detours.withSmallestTentativeDistance
//      new Dijkstra(City, nextCurrent, destination, TentativeDistances, Detours, shortcuts)
//    }
//
//    // Context helper methods
//    def pathTo(x: Intersection): List[Intersection] = { if (!shortcuts.contains(x)) List(x) else x :: pathTo(shortcuts(x)) }
//    def shortestPath() = pathTo(destination).reverse
//
//    // Roles
//    role(TentativeDistances) {
//      def initialize() {
//        TentativeDistances.put(CurrentIntersection, 0)
//        City.intersections.filter(_ != CurrentIntersection).foreach(TentativeDistances.put(_, Int.MaxValue / 4))
//      }
//    }
//    role(Detours) {
//      def initialize() { Detours ++= City.intersections }
//      def withSmallestTentativeDistance() = { Detours.reduce((x, y) => if (TentativeDistances(x) < TentativeDistances(y)) x else y) }
//    }
//    role(CurrentIntersection) {
//      def calculateTentativeDistanceOfNeighbors() {
//        City.eastNeighbor.foreach(updateNeighborDistance(_))
//        City.southNeighbor.foreach(updateNeighborDistance(_))
//        Detours.remove(CurrentIntersection)
//      }
//      def updateNeighborDistance(neighborIntersection: Intersection) {
//        if (Detours.contains(neighborIntersection)) {
//          val newTentDistanceToNeighbor = currentDistance + lengthOfBlockTo(neighborIntersection)
//          val currentTentDistToNeighbor = TentativeDistances(neighborIntersection)
//          if (newTentDistanceToNeighbor < currentTentDistToNeighbor) {
//            TentativeDistances.update(neighborIntersection, newTentDistanceToNeighbor)
//            shortcuts.put(neighborIntersection, CurrentIntersection)
//          }
//        }
//      }
//      def currentDistance() = TentativeDistances(CurrentIntersection)
//      def lengthOfBlockTo(neighbor: Intersection) = City.distanceBetween(CurrentIntersection, neighbor)
//    }
//    role(City) {
//      def distanceBetween(from: Intersection, to: Intersection) = City.blockLengths(new Block(from, to))
//      def eastNeighbor() = City.nextDownTheStreet.get(CurrentIntersection)
//      def southNeighbor() = City.nextAlongTheAvenue.get(CurrentIntersection)
//    }
//  }
//
//  // Data
//  @Javascript class Intersection(name: Char)
//  @Javascript class Block(x: Intersection, y: Intersection)
//
//  // Environment
//  @Javascript class ManhattanGrid() {
//    val intersections               = ('a' to 'i').map(new Intersection(_)).toList
//    val (a, b, c, d, e, f, g, h, i) = (intersections(0), intersections(1), intersections(2), intersections(3), intersections(4), intersections(5), intersections(6), intersections(7), intersections(8))
//    val nextDownTheStreet           = Map(a -> b, b -> c, d -> e, e -> f, g -> h, h -> i)
//    val nextAlongTheAvenue          = Map(a -> d, b -> e, c -> f, d -> g, f -> i)
//    val blockLengths                = Map(new Block(a, b) -> 2, new Block(b, c) -> 3, new Block(c, f) -> 1, new Block(f, i) -> 4, new Block(b, e) -> 2, new Block(e, f) -> 1, new Block(a, d) -> 1, new Block(d, g) -> 2, new Block(g, h) -> 1, new Block(h, i) -> 2, new Block(d, e) -> 1)
//
//    //    a - 2 - b - 3 - c
//    //    |       |       |
//    //    1       2       1
//    //    |       |       |
//    //    d - 1 - e - 1 - f
//    //    |               |
//    //    2               4
//    //    |               |
//    //    g - 1 - h - 2 - i
//  }
//
//  val startingPoint = new ManhattanGrid().a
//  val destination   = new ManhattanGrid().i
//
//  val test = javascript {
//    val shortestPath = new Dijkstra(new ManhattanGrid(), startingPoint, destination).shortestPath
//    print("Dijkstra:\n" + shortestPath.map(_.name).mkString(" -> "))
//    // a -> d -> g -> h -> i
//  }
//
//  val js = Dijkstra.jscala.javascript ++ Intersection.jscala.javascript ++ Block.jscala.javascript ++ ManhattanGrid.jscala.javascript ++ test // join classes definitions with main code
//  println("-------  js --------")
//  println(js.asString) // prints resulting JavaScript
//  println("\n------- test --------")
//  js.eval() // run using Rhino (https://developer.mozilla.org/en-US/docs/Rhino)
//}