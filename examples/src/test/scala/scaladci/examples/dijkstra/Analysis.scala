package scaladci.examples.dijkstra
/*

DISCLAIMER: This is not a tutorial on DCI. It's more of an open workshop sharing my thoughts of how to
implement a little more elaborate mental model of something to code up. In this case it's about the Dijkstra
algorithm for calculating the shortest path between two nodes of a graph with DCI and Scala in a way that
respects the rich mental model presented on the wiki:

http://en.wikipedia.org/wiki/Dijkstra's_algorithm

And it's my first test to see if my type macro approach works for a recursive setup without messing up object
identities.

First, I want to get a clear mental model of what I want to implement...

As of 2013-01-20 I have copy and pasted the Algorithm and Description sections from the wiki page in here and
highlighted the essential terms in the text to start getting a picture of which Roles and Interactions we'll
expect in our Context:

##############################################################################################################
                                                GRAPH MODEL
##############################################################################################################

Algorithm
--------------------------------------------------------------------------------------------------------------
Let the node at which we are starting be called the INITIAL NODE. Let the DISTANCE of node Y be the distance
from the INITIAL NODE to Y. Dijkstra's algorithm will assign some INITIAL DISTANCE VALUES and will try to
improve them step by step.

  1.  Assign to every NODE a TENTATIVE DISTANCE value: set it to zero for our INITIAL NODE and to infinity for
      all other nodes.

  2.	Mark all nodes unvisited. Set the INITIAL NODE as CURRENT [NODE]. Create a set of the UNVISITED NODEs
      called the UNVISITED SET consisting of all the nodes except the INITIAL NODE.

  3.	For the CURRENT NODE, consider all of its UNVISITED NEIGHBORS and calculate their TENTATIVE DISTANCES.
      For example, if the CURRENT NODE A is marked with a DISTANCE of 6, and the EDGE connecting it with a
      NEIGHBOR B has length 2, then the distance to B (through A) will be 6+2=8. If this distance is less than
      the previously recorded TENTATIVE DISTANCE of B, then overwrite that distance. Even though a NEIGHBOR has
      been examined, it is not marked as "visited" at this time, and it remains in the UNVISITED SET.

  4.	When we are done considering all of the NEIGHBORS of the CURRENT NODE, mark the CURRENT NODE as visited
      and remove it from the UNVISITED SET. A visited node will never be checked again.

  5.	If the DESTINATION NODE has been marked visited (when planning a ROUTE between two specific nodes) or
      if the smallest TENTATIVE DISTANCE among the nodes in the UNVISITED SET is infinity (when planning a
      complete traversal), then stop. The algorithm has finished.

  6.	Select the UNVISITED NODE that is marked with the smallest TENTATIVE DISTANCE, and set it as the new
      "CURRENT NODE" then go back to step 3.
--------------------------------------------------------------------------------------------------------------

From a simplistic look at the algorithm text, one could just filter out the Nouns. But that's a very flat
mental model that doesn't tell much about what relationships each Role has to other Roles for instance.
For one, I see 3 different "kinds" of role aspirants with different basic characteristics:

"Acting roles" that play the central parts during an execution:
- Current Node
- Neighbor Node

"Housekeeping roles" that keep track of things, which intersections are currently unvisited etc:
- Unvisited Set
- Tentative Distances

"Provider roles" that merely provide data to acting and housekeeping roles:
- Graph
- Initial Node
- destination Node

Since we will iterate through the graph of nodes to calculate the shortest path to a specific node, we'll
examine each node one by one. So nodes will switch between playing the CurrentNode and NeighBorNode roles
all the time. Those are our "main characters" in the play.

Roles can't hold data according to DCI constraints. That would mess up what data belongs to the Role and what
data to the instance object playing that role. There is no Role object! There is only _an object_ that plays or
doesn't play a Role. "Playing a role" means having a meaningful Role name in a Context, and calling role methods.
That's also a good reason _not_ to implement a Role as a trait or class, since that inevitably leads our thoughts
towards the idea of a Role object coming into life which is not the case with our approach.



Data structures
- Node
- Graph
- Path / Route
- Edge ?
- Distance



##############################################################################################################
                                              CITY MAP MODEL
                                      (Variation on the Graph model)
##############################################################################################################

The Description section on the wiki page turns out to be a variation on the graph model above. It uses some
other words for the same concepts which changes our mental model slightly. So let's highlight those:

Description
--------------------------------------------------------------------------------------------------------------
Suppose you want to find the SHORTEST PATH between two INTERSECTIONS on a CITY MAP, a STARTING POINT and
a DESTINATION. The order is conceptually simple: to start, mark the distance to every intersection on the
map with infinity. This is done not to imply there is an infinite distance, but to note that that intersection
has not yet been visited; some variants of this method simply leave the intersection unlabeled. Now, at each
iteration, select a CURRENT INTERSECTION. For the first iteration the CURRENT INTERSECTION will be the starting
point and the distance to it (the intersection's label) will be zero. For subsequent iterations (after the first)
the CURRENT INTERSECTION will be the closest UNVISITED INTERSECTION to the starting point — this will be easy
to find.

From the CURRENT INTERSECTION, update the distance to every UNVISITED INTERSECTION that is directly CONNECTED
to it. This is done by determining the sum of the distance between an UNVISITED INTERSECTION and the value of
the CURRENT INTERSECTION, and relabeling the UNVISITED INTERSECTION with this value if it is less than its
current value. In effect, the intersection is relabeled if the PATH to it through the CURRENT INTERSECTION is
shorter than the PREVIOUSLY KNOWN PATHS. To facilitate SHORTEST PATH identification, in pencil, mark the ROAD
with an arrow pointing to the relabeled intersection if you label/relabel it, and erase all others pointing to it.
After you have updated the distances to each NEIGHBORing intersection, mark the CURRENT INTERSECTION as visited
and select the UNVISITED INTERSECTION with lowest distance (from the STARTING POINT) -- or lowest label—as the
CURRENT INTERSECTION. Nodes marked as visited are labeled with the shortest PATH from the STARTING POINT to it
and will not be revisited or returned to.

Continue this process of updating the NEIGHBORing intersections with the shortest distances, then marking the
CURRENT INTERSECTION as visited and moving onto the closest UNVISITED INTERSECTION until you have marked the
DESTINATION as visited. Once you have marked the DESTINATION as visited (as is the case with any visited
intersection) you have determined the SHORTEST PATH to it, from the STARTING POINT, and can trace your way back,
following the arrows in reverse.
--------------------------------------------------------------------------------------------------------------

Now our participants might look like this:

Acting roles
- Current Intersection
- Neighbor Intersection

Housekeeping roles
- Unvisited Set
- Tentative Distances
- Shortest Paths

Provider roles
- City Map
- Starting Point
- destination

Data structures
- Intersection
- City Map
- Path / Route
- Road


Let's transform the algorithm for the Graph model to one for a City Map model:

  1.  Assign to every INTERSECTION a TENTATIVE DISTANCE value: set it to zero for our INITIAL INTERSECTION /
      STARTING POINT and to infinity for all other intersections.

  2.	Mark all intersections unvisited. Set the INITIAL INTERSECTION as CURRENT INTERSECTION. Create a set
      of the UNVISITED INTERSECTIONS called the UNVISITED SET consisting of all the intersections except the
      INITIAL INTERSECTION.

  3.	For the CURRENT INTERSECTION, consider all of its UNVISITED NEIGHBOR INTERSECTIONS and calculate their
      TENTATIVE DISTANCES. For example, if the CURRENT INTERSECTION A is marked with a DISTANCE of 6, and the
      ROAD connecting it with a NEIGHBOR INTERSECTION B has length 2, then the distance to B (via A) will
      be 6 + 2 = 8. If this distance is less than the previously recorded TENTATIVE DISTANCE of B, then
      overwrite that distance. Even though a NEIGHBOR INTERSECTION has been examined, it is not marked as
      "visited" at this time, and it remains in the UNVISITED SET.

  4.	When we are done considering all of the NEIGHBOR INTERSECTIONS of the CURRENT INTERSECTION, mark the
      CURRENT INTERSECTION as visited and remove it from the UNVISITED SET. A visited intersection will never
      be checked again.

  5.	If the DESTINATION INTERSECTION has been marked visited (when planning a ROUTE between two specific
      intersections) or if the smallest TENTATIVE DISTANCE among the intersections in the UNVISITED SET is
      infinity (when planning a complete traversal), then stop. The algorithm has finished.

  6.	Select the UNVISITED INTERSECTION that is marked with the smallest TENTATIVE DISTANCE, and set it as
      the new "CURRENT INTERSECTION" then go back to step 3.


##############################################################################################################
                                         MANHATTAN STREET LEVEL MODEL
                                      (Variation on the City Map model)
##############################################################################################################

When we specialize our City Map model to Manhattan, we get an even more refined mental model to work with.
The city map of Manhattan is as we know a square grid of Streets (running east and west) and Avenues (running
north and south), and we could easily adopt the term "Grid" as a specialized term for a city map. But I'll
take another approach closer to street level - that of how a taxi driver in New York would look at things
rather than that of a graph theory specialist.

Our taxi driver Bob would say "City" instead of "Grid" / "City Map" (a Map, Sir??!! I hope you're not trying
to insult me!)

From a specific intersections point of view we can look along an Avenue in southern direction to the
neighboring Southern Intersection, or we can look eastwards down a Street to the neighboring Eastern
Intersection.

"Blocks" connect each Intersection (http://en.wikipedia.org/wiki/City_block).

Bob would also rather say "Route" than "Path" to describe how to get from A to B.

Somehow it seems strange also to say that we have "visited" an Intersection once we have considered its
neighboring intersections. If we instead think of Bob as a pessimistic guy that consider all combinations
of intersections as detours until he has checked that a certain Intersection is not, then we could use
"detours" to contain all Intersections that have not yet been considered.

The shortest route from A to B can be viewed as a shortcut. Following the best shortcuts gives us the
shortest route to our destination (and Bob is not thinking of keyboard shortcuts).

This changes our terminology and brings us more in contact with the mental model of our taxi driver in NY:

Acting roles:
- Current intersection
- Neighbor intersection
  - East intersection
  - South intersection

Housekeeping roles:
- detours (set of yet unconsidered intersections)
- Tentative distances
- Shortcuts

Provider roles:
- City
- Starting point
- destination

Data structures:
- City
- Street / Avenue
- Intersection
- Block
- Route


Let's update the algorithm to reflect our new specialized mental model of route planning in New York. Since
we are not doing a complete traversal, step 5 can be simplified and since we determine whether an Intersection
has been visited/considered not by marking it, but by checking it is in the detours set, we can also simplify
those parts. We end up with:


  1.  Assign a TENTATIVE DISTANCE of zero to our INITIAL INTERSECTION and infinity to all other intersections.

  2.	Set the INITIAL INTERSECTION as CURRENT INTERSECTION.
      Create a set called DETOURS containing all intersections except the INITIAL INTERSECTION.

  3.	For the CURRENT INTERSECTION, check its EASTERN and SOUTHERN NEIGHBOR INTERSECTIONS and calculate
      their TENTATIVE DISTANCES. For example, if the CURRENT INTERSECTION A is marked with a DISTANCE of 6,
      and the BLOCK connecting it with its EASTERN NEIGHBOR INTERSECTION B has length 2, then the distance
      to B (via A) will be 6 + 2 = 8. If this distance (8) is less than the previously recorded TENTATIVE
      DISTANCE of B, then overwrite that distance. Even though a neighbor intersection has been considered,
      it is not marked as a SHORTCUT at this time, and it remains in DETOURS.

  4.	When we are done considering the EASTERN and SOUTHERN NEIGHBOR INTERSECTIONS of the CURRENT
      INTERSECTION, remove the CURRENT INTERSECTION from DETOURS.

  5.	If the DESTINATION intersection is no longer in DETOURS, then stop. The algorithm has finished.

  6.	Select the intersection in DETOURS with the smallest TENTATIVE DISTANCE, and set it as the new
      CURRENT INTERSECTION then go back to step 3.


I think it's good if we stick to a determined and unambiguous set of words for clearly defined parts of
our mental model. If we for instance in our code use more than one of

  Graph, Map, Q, CityMap, Geometry, ManhattanGrid, Grid, ManhattanGeometry etc...

then I think we are not enriching our mental model but rather not being clear about it. One programmers
freedom to use synonyms will lead to another programmers confusion!

If we refer to "The wiki Dijkstra algorithm/description", it should be clear from the above that we are
referring to not one but several mental models! Of course each of them is a variation of a shared
fundamental mental model ("funda_mental_", hmmm...)

It's also worth noticing that there's nothing on the wiki page about Manhattan grids or Eastern/Southern
neighbors. So I admit, I have been confused by which mental model we were actually trying to implement.
That's also why I took some time to think it all over again, and decide more precisely on which mental model
I wanted to use for my Dijkstra implementation here.

I'll go for the "Manhattan street level model". Taking my own medicine means that I'll have to say goodbye
to beloved old friends like "Node" and "Graph". I have to keep remembering that my taxi driver in NY never
uses those words at all...

As a little exercise I'll build the Dijkstra example incrementally in some steps to see where that leads
me - also to see how my type macro does along the way (no pun intended). Please don't take this a tutorial
of how to do DCI, but rather a share of my personal little voyage. Have fun!

Marc Grue
2013-01-21
*/