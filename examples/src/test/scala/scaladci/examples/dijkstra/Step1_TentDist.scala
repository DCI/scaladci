package scaladci
package examples.dijkstra
import collection.mutable
/*
Some background info about how the type macro transformation of the Abstract Syntax Tree (AST) enables us to
bind instance objects to roles:

When our CalculateShortestPath extends Context there's not yet a Context type. There's no Context class or
trait. In a pre-compilation phase (don't ask me how this goes!), the Scala compiler will look for a Context
type and see that our type macro can generate one! The type macro has access to the AST of the child class
(CalculateShortestPath) and is free to manipulate any part of it before returning a new and transformed
Context type!

We can therefore peek into the source code of CalculateShortestPath in advance and transform various sections
of it to allow our instance objects to play Roles.

What looks like a "role" method call (in the bottom of the CalculateShortestPath classs), actually just acts
as a placeholder for defining our role methods! Since Scala doesn't even allow us to introduce a "role" keyword
with a compiler plugin, this is probably the closest alternative we have (unless we go into the deep halls of
the Scala compiler itself, which takes its master and is much less flexible!). Our "Role Definition Method"
has the following signature:

  def role(instance: AnyRef)(roleMethods: => Unit) {}
          \--- RoleName---/ \----RoleMethods----/ (empty implementation)

We can pass an instance object with a suitable identifier name that will act as the Role Name. The instance
object can come from anywhere.

Inside curly braces that matches the second arguments list (roleMethods: => Unit), we can supply our role
methods as we do with our first role method assignTentativeDistances in the Role "TentativeDistances":

    role TentativeDistances {
      def assignTentativeDistances {
        ...
      }
    }

After AST transformation, the role method assignTentativeDistances is prefixed with the role name
TentativeDistances and the whole method is lifted to the Context namespace:

    private def TentativeDistances_assignTentativeDistances {
      ...
    }

This is in line with what Risto Välimäki suggested here:
https://groups.google.com/d/msg/object-composition/ulYGsCaJ0Mg/rF9wt1TV_MIJ

So we are in no way modifying the instance object. We are not "injecting" methods into it or adding anything
to the instance class. We are simply adding a role method to the Context, that we then call. This is useful
when we also transform the calls to the transformed role methods, so that for instance:

    TentativeDistances.assignTentativeDistances

is transformed into:

    TentativeDistances_assignTentativeDistances

Since the transformed role methods are private there's no risk anyone can use those directly. We only need
to think about the untransformed methods as we see them in code.

After the type macro is done transforming the AST, the Scala compiler type checks the resulting AST as though
it was normal code and we can therefore rely on normal type safety enforced by the compiler (to the extend we
don't do crazy things with the type macro ;-)

Before our IDE understands type macro transformations (the feature is still in an experimental stage), our
role method calls will look like invalid code and we won't get any help to autoComplete role method names
(like assignTentativeDistances). But should we happen to spell a role method name wrong, it won't compile
later anyway, so we're still type safe.

There's still much work to be done to analyze and implement how the type macro handles role method overloading
of instance methods etc...

Now to the first step of the Dijkstra algorithm

  1.  Assign a TENTATIVE DISTANCE of zero to our INITIAL INTERSECTION and infinity to all other intersections.

We start out with one role: TentativeDistance which is mostly a "housekeeping" role that takes care of the
initial assignment of tentative distances as described in step 1 of the algorithm. Notice that we use an
object created inside the Context to play the TentativeDistance role.
*/
object Step1_TentDist extends App {

  // Context ##################################################################

  @context
  class CalculateShortestPath(
    City: ManhattanGrid,
    CurrentIntersection: Intersection,
    Destination: Intersection
  ) {

    // Initialization of a data-holding role player in the Context
    private val TentativeDistances = mutable.HashMap[Intersection, Int]()

    // Run
    TentativeDistances.assignTentativeDistances

    // Tentative distance of Intersection 'a' has been set to 0 and the rest to infinity:
    println("Tentative distances after :\n" + TentativeDistances.mkString("\n"))

    // Adding the first "housekeeping" role, given a role name after the data it "administrates"
    role TentativeDistances {

      // First role method defined
      def assignTentativeDistances {

        // STEP 1:

        // We access the instance methods by using the passed instance identifier:
        TentativeDistances.put(CurrentIntersection, 0)

        // We can access Context parameters directly:
        City.intersections.filter(_ != CurrentIntersection).foreach(TentativeDistances.put(_, Int.MaxValue / 4))
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