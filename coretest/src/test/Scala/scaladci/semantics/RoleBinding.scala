package scaladci
package semantics
import util._

class RoleBinding extends DCIspecification {

   "Through identifier of environment object" >> {

    // Data object `MyRole` binds to Role `MyRole` since they share name
    @context
    case class Context(MyRole: Data) {
      def trigger = MyRole.foo

      role MyRole {
        def foo = MyRole.i * 2
      }
    }

    val obj = Data(42)
    Context(obj).trigger === 42 * 2
  }


  "Through variable name identifying new object" >> {

    @context
    case class Context(i: Int) {
      // Variable `MyRole` identifies instantiated `Data` object in Context
      // Variable `MyRole` binds to Role `MyRole` since they share name
      val MyRole = Data(i)
      def trigger = MyRole.foo

      role MyRole {
        def foo = MyRole.i * 2
      }
    }

    Context(42).trigger === 42 * 2
  }


  "Dynamically through new variable name identifying another object/RolePlayer in Context" >> {

    @context
    case class Context(RoleA: Data) {

      // Variable `RoleB` references `RoleA` object
      // Variable `RoleB` binds to Role `RoleB` since they share name
      val RoleB = RoleA

      def trigger = RoleB.bar

      role RoleB {
        def bar = RoleB.i * 2
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
