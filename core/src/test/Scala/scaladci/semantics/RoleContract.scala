package scaladci
package semantics
import util._
import scala.language.reflectiveCalls

  /*
  Role contracts (or "Role-object contracts")

  For an object to play a Role it needs to satisfy a Role contract that defines
  what methods the Role expect it to have before they can merge to a Role player.
  We call those methods in the object class "instance methods".

  When our Role players start to interact at runtime they will call these methods
  on each other. To maximise our ability to reason about the expected outcome of
  those interactions we therefore need to know what the instance methods do too
  to various extends depending on their nature (we don't need to lookup what
  `toString` does for instance).


  Types - precise but not a silver bullet always...

  One way to know the object's methods is of course to know the objects type.
  We can look at the method definition in the defining class (the objects type)
  and see what it does.

  But what if we get an object that is a subtype of the type we have defined
  in our Role contract? The method could have been overriden and now do nasty
  things that would give us nasty surprises once we run our program.

  Fortunately we can enforce an even more specific type equalling the expected
  type (avoiding subclasses thereof). But even that specific type might in turn
  depend on other classes that could have been subclasses and so on. That seems
  to suggest that we will never be able to reason with 100% certainty about
  the outcome of our program at runtime - unless we define all object classes
  ourselves.


  Structural types (duck typing) - flexible but unpredictable...

  When a Role is more flexible and wants to allow a broader set of objects
  to be able to play it, then a structural type (a la duck-typing) will come
  in handy. The role contract would then define what method signature(s) a
  Role expects. The backside of that coin is of course that we would know
  much less about what those methods do.


  Levels of predictability:

  1) One and only type (enforced with type equality)
  2) Type (could be any subtype too)
  3) Structural type (duck-typing)

 */

class RoleContract extends DCIspecification {


  /******** Type **************************************************************/


  "Can be a type" >> {

    @context
    case class Context(MyRole: Data) {

      def trigger = MyRole.foo

      role MyRole {
        def foo = self.number // We know type `Data` (and that it has a number method)
      }
    }
    Context(Data(42)).trigger === 42
  }


  "Can naïvely trust a type" >> {

    class Data(i: Int) {
      def number = i
    }
    case class DodgySubClass(i: Int) extends Data(i) {
      override def number = -666
    }

    @context
    case class Context(MyRole: Data) { // <- We feel falsely safe :-(

      def trigger = MyRole.foo

      role MyRole {
        def foo = self.number
      }
    }
    val devilInDisguise = DodgySubClass(42)
    Context(devilInDisguise).trigger === -666 // Not 42 !!! Auch
  }


  "Can rely more safely on a specific type" >> {

    class ExpectedType(i: Int) {
      def number = i
    }
    class DodgySubClass(i: Int) extends ExpectedType(i) {
      override def number = -666
    }

    @context
    case class Context[T](MyRole: T)(implicit ev: T =:= ExpectedType) { // <- enforce only this type

      def trigger = MyRole.foo

      role MyRole {
        def foo = self.number
      }
    }

    // A dodgy subclass can't sneak in
    expectCompileError(
      """
        val devilInDisguise = new DodgySubClass(42)
        Context(devilInDisguise).trigger === 42
      """,
      "Cannot prove that DodgySubClass =:= ExpectedType")


    // Only objects of our expected type (and no subtype) can be used:
    val expectedObject = new ExpectedType(42)
    Context(expectedObject).trigger === 42
  }


  /******** Structural Type (duck typing) **************************************************/


  "Can be a structural type (duck typing)" >> {

    @context
    case class Context(MyRole: {def number: Int}) {

      def trigger = MyRole.foo

      role MyRole {
        // We know that the instance (of unknown type) has a `number` method returning Int
        def foo = self.number
      }
    }
    Context(Data(42)).trigger === 42


    case class NastyData(i: Int) {
      def number = {
        println("Firing missiles...")
        i
      }
    }

    @context
    case class NaiveContext(MyRole: {def number: Int}) {

      def trigger = MyRole.foo

      role MyRole {
        // We know that the instance (of unknown type) has a `number` method returning Int
        // - but we don't know that it also fire off missiles!!!
        def foo = self.number
      }
    }
    NaiveContext(NastyData(42)).trigger === 42 // + world war III
  }


  "Can require several instance methods with structural types (duck typing)" >> {

    case class Kid() {
      def age = 16
      def name = "John"
    }
    case class Adult() {
      def age = 32
      def name = "Alex"
    }

    @context
    case class Disco(Visitor: {
      def age: Int
      def name: String}) {

      def letMeDance = Visitor.canIGetIn

      role Visitor {
        def canIGetIn = {
          if (self.age < 18)
            s"Sorry, ${self.name}, you're only ${self.age} years old. I can't let you in."
          else
            s"Welcome, ${self.name}. Shall I take your coat?"
        }
      }
    }
    Disco(Kid()).letMeDance === "Sorry, John, you're only 16 years old. I can't let you in."
    Disco(Adult()).letMeDance === "Welcome, Alex. Shall I take your coat?"
  }


  "Can't omit instance method defined in structural type" >> {

    trait Data {
      def foo: Boolean
      val i: Int
    }
    case class DataA(s: String) extends Data {
      def foo = true
      def text = s
      val i = 1
    }
    case class DataB(s: String, i: Int) extends Data {
      def foo = true
      def text = s
      def number = i
    }
    case class DataC(i: Int) extends Data {
      def foo = false
      def number = i
    }

    @context
    case class Context(MyRole: Data {def text: String}) {

      def trigger = MyRole.bar

      role MyRole {
        def bar = {
          val result = if (self.foo) "Yes!" else "No!"
          val status = self.text + result
          status
        }
      }
    }

    Context(DataA("Will A fulfill the Role contract? ")).trigger === "Will A fulfill the Role contract? Yes!"
    Context(DataB("Will B fulfill the Role contract? ", 42)).trigger === "Will B fulfill the Role contract? Yes!"

    // This won't compile:
    // Context(DataC(911)).trigger === ...

    // Gives the following error message:

    // Type mismatch
    // expected: Data {def text: String}
    // actual:   DataC
  }



  /******** Mix of types and duck typing **************************************************/

  "Can be a mix of a type and a structural type" >> {

    class Data(i: Int) {
      def number = i
    }
    case class OtherData(i: Int) extends Data(i) {
      def text = "My number is: "
    }

    @context
    case class Context(MyRole: Data {def text: String}) { // <- OtherData will satisfy this contract

      def trigger = MyRole.foo

      role MyRole {
        def foo = self.text + self.number // `Data` has a `number` method and there should also be some `text` method...
      }
    }
    Context(OtherData(42)).trigger === "My number is: 42"
  }

}
