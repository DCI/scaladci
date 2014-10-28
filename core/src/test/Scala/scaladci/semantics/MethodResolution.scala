package scaladci
package semantics
import scala.language.reflectiveCalls
import scaladci.util._


class MethodResolution extends DCIspecification {

  class Obj {
    def foo = "FOO"
    def bar = "BAR" + foo
  }

  "Role method takes precedence over instance method" >> {

    @context
    case class Context1(a: Obj) {

      def resolve = a.foo

      role a {
        def foo = "foo"
      }
    }
    Context1(new Obj).resolve === "foo"


    // https://groups.google.com/d/msg/object-composition/MhrHIn9LaNs/ShkSPbJ-rlsJ

    @context
    case class Context2(a: Obj, b: Obj) {

      def resolve = a.foo + b.foo

      role a {
        def foo = "foo"
      }
      role b {}
    }
    Context2(new Obj, new Obj).resolve === "fooFOO"


    // Is this example correctly interpreted, Rune?
    @context
    case class Context3(a: Obj) {

      def resolve = a.foo

      role a {
        def foo = self.bar
      }
    }
    Context3(new Obj).resolve === "BARFOO"


    @context
    case class Context4(a: Obj) {

      def resolve = a.foo + a.bar

      role a {
        def foo = self.bar
        def bar = "bar"
      }
    }
    Context4(new Obj).resolve === "BARFOObar"


    @context
    case class Context5(a: Obj) {

      def resolve = a.foo

      role a {
        def foo = self.bar + a.bar
        def bar = "bar" // not called
      }
    }
    Context5(new Obj).resolve === "BARFOObar"


    @context
    case class Context6(a: Obj, b: Obj) {

      def resolve = a.foo + b.bar

      role a {
        def foo = self.bar
      }
      role b {
        def bar = "bar" // not called
      }
    }
    Context6(new Obj, new Obj).resolve === "BARFOObar"


    @context
    case class Context7(a: Obj, b: Obj) {

      def resolve = a.foo

      role a {
        def foo = self.bar + b.bar
      }
      role b {
        def bar = "bar" // not called
      }
    }
    Context7(new Obj, new Obj).resolve === "BARFOObar"


    // Todo: are these tests shredding sufficient light on method resolution?!
  }


  "With name clashes in Context (??)" >> {

    // https://groups.google.com/d/msg/object-composition/QfvHXzuP2wU/M622DO1y_JYJ
    // https://groups.google.com/d/msg/object-composition/QfvHXzuP2wU/PIkAZdcWp5QJ
    // https://gist.github.com/runefs/4338821

    class A {
      def foo = "a"
    }

    type roleContract = {def foo: String}

    @context
    case class C(var y: roleContract, var z: roleContract) {

      role y {}

      role z {
        def foo = "z"
      }

      def print() { println(s"foo of y ${y.foo}\nfoo of z ${z.foo}") }

      def foo = "context"

      def initialize(someT: roleContract, someK: roleContract) {
        y = someT
        z = someK
      }
      def rebind(a: roleContract) {
        initialize(a, this)
        print()
        initialize(this, a)
      }
    }

    def doIt(someT: roleContract, someK: roleContract) {
      val c = new C(someT, someK)
      c.print()
      c.rebind(someT)
      c.print()
    }

    val a = new A
    //    doIt(a, a)

    /* Prints as expected:

      foo of y a
      foo of z z
      foo of y a
      foo of z z
      foo of y context
      foo of z z

    */
    success
  }


  "Egon Elbre's Toture Test, the Tournament" >> {

    // https://groups.google.com/d/msg/object-composition/AsvEI7iJSDs/_HX5S4Ep9Q4J

    case class Player(name: String, msg: String) {
      def say() {
        println(s"[$name] $msg")
      }
    }

    def doInterview(player: Player) {
      println("[Interviewer] Hello!")
      player.say()
    }

    def callback(fn: () => Unit) {
      fn()
    }

    @context
    case class Battle(Id: Int, Bear: Player, Lion: Player) {

      def start() {
        println(Id + " battle commencing:")
        Bear.fight()
      }
      def interview() {
        doInterview(Bear)
        doInterview(Lion)
      }

      role Bear {
        def say() {
          println(Id + " [" + self.name + "] Grrrr.....")
        }
        def fight() {
          Bear.say()
          Lion.fight()
        }
      }

      role Lion {
        def say() {
          println(Id + " [" + self.name + "] Meow.....")
        }
        def fight() {
          callback(Lion.say)
        }
      }
    }

    val human = Player("Jack", "says Hello!")
    val cpu = Player("Cyborg", "bleeps Hello!")

    val b1 = Battle(1, human, cpu)
    val b2 = Battle(2, cpu, human)
    val b3 = Battle(3, cpu, cpu)

    b1.start()
    b2.start()
    b3.start()

    b1.interview()

    /* Prints as expected:

      1 battle commencing:
      1 [Jack] Grrrr.....
      1 [Cyborg] Meow.....
      2 battle commencing:
      2 [Cyborg] Grrrr.....
      2 [Jack] Meow.....
      3 battle commencing:
      3 [Cyborg] Grrrr.....
      3 [Cyborg] Meow.....
      [Interviewer] Hello!
      [Jack] says Hello!
      [Interviewer] Hello!
      [Cyborg] bleeps Hello!

    */
    success
  }
}
