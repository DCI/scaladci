package scaladci
package semantics
import util._

class RoleBinding extends DCIspecification {

   "Through identifier of environment object" >> {

    // Data object `Foo` binds to Role `Foo` since they share name
    @context
    case class Context(Foo: Data) {
      def trigger = Foo.foo

      role Foo {
        def foo = Foo.i * 2
      }
    }

    val obj = Data(42)
    Context(obj).trigger === 42 * 2
  }


  "Through variable name identifying new object" >> {

    @context
    case class Context(i: Int) {
      // Variable `Foo` identifies instantiated `Data` object in Context
      // Variable `Foo` binds to Role `Foo` since they share name
      val Foo = Data(i)
      def trigger = Foo.foo

      role Foo {
        def foo = Foo.i * 2
      }
    }

    Context(42).trigger === 42 * 2
  }


  "Dynamically through new variable name identifying another object/RolePlayer in Context" >> {

    @context
    case class Context(Foo: Data) {

      // Variable `Bar` references `Foo` object
      // Variable `Bar` binds to Role `Bar` since they share name
      val Bar = Foo

      def trigger = Bar.bar

      role Bar {
        def bar = Bar.i * 2
      }
    }

    val obj = Data(42)
    Context(obj).trigger === 42 * 2
  }


  "Todo: All Roles in a Context are bound to objects in a single, atomic operation" >> {

    // Todo: How to demonstrate/reject un-atomic bindings?
    ok
  }
}
