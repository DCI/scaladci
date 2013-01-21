package scaladci.examples.dijkstra

//Defining the basic Data structures...

object Data extends App {

  // Data  ####################################################################

  // Street/Avenue intersection (former "Node")
  case class Intersection(name: Char)

  // City blocks that connect each Intersection (former "Pair", "Edge", "Arc" etc.)
  // http://en.wikipedia.org/wiki/City_block
  case class Block(x: Intersection, y: Intersection)


  // Test data ################################################################

  // Using only immutable values, so the Grid will remain the same always.

  case class ManhattanGrid() {

    // Intersections
    val intersections               = ('a' to 'i').map(Intersection(_)).toList
    val (a, b, c, d, e, f, g, h, i) = (intersections(0), intersections(1), intersections(2), intersections(3), intersections(4), intersections(5), intersections(6), intersections(7), intersections(8))

    // Street blocks
    val nextDownTheStreet           = Map(a -> b, b -> c, d -> e, e -> f, g -> h, h -> i)

    // Avenue blocks
    val nextAlongTheAvenue          = Map(a -> d, b -> e, c -> f, d -> g, f -> i)

    // Now we have defined the grid (without distances yet):

    //    a - - - b - - - c
    //    |       |       |
    //    |       |       |
    //    |       |       |
    //    d - - - e - - - f
    //    |               |
    //    |               |
    //    |               |
    //    g - - - h - - - i

    // Add distances between intersections ("how long is the block?")
    val blockLengths = Map(
      Block(a, b) -> 2,
      Block(b, c) -> 3,
      Block(c, f) -> 1,
      Block(f, i) -> 4,
      Block(b, e) -> 2,
      Block(e, f) -> 1,
      Block(a, d) -> 1,
      Block(d, g) -> 2,
      Block(g, h) -> 1,
      Block(h, i) -> 2,
      Block(d, e) -> 1
    )

    // The complete Manhattan grid

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

  println("Manhattan grid distances:\n" + ManhattanGrid().blockLengths.mkString("\n"))
}