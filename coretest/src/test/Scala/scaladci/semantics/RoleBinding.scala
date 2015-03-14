package scaladci
package semantics
import util._

class RoleBinding extends DCIspecification {

   "Through identifier of environment object" >> {

    // Data object `myRole` binds to Role `myRole` since they share name
    @context
    case class Context(myRole: Data) {
      def trigger = myRole.foo

      role myRole {
        def foo = myRole.i * 2
      }
    }

    val obj = Data(42)
    Context(obj).trigger === 42 * 2
  }


  "Through variable name identifying new object" >> {

    @context
    case class Context(i: Int) {
      // Variable `myRole` identifies instantiated `Data` object in Context
      // Variable `myRole` binds to Role `myRole` since they share name
      val myRole = Data(i)
      def trigger = myRole.foo

      role myRole {
        def foo = myRole.i * 2
      }
    }

    Context(42).trigger === 42 * 2
  }


  "Dynamically through new variable name identifying another object/RolePlayer in Context" >> {

    @context
    case class Context(roleA: Data) {

      // Variable `roleB` references `roleA` object
      // Variable `roleB` binds to Role `roleB` since they share name
      val roleB = roleA

      def trigger = roleB.bar

      role roleB {
        def bar = roleB.i * 2
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
