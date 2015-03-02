package scaladci
package semantics
import util._

class ObjectInstantiation extends DCIspecification {

  "In environment (passed to Context)" >> {

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


  "In Context" >> {

    @context
    case class Context(i: Int) {

      val MyRole = Data(i)

      def trigger = MyRole.foo

      role MyRole {
        def foo = MyRole.i * 2
      }
    }

    Context(42).trigger === 42 * 2
  }


  "As new variable referencing already instantiated object/RolePlayer" >> {

    @context
    case class Context(RoleA: Data) {

      val RoleB = RoleA

      def trigger = RoleB.bar

      role RoleB {
        def bar = RoleB.i * 2
      }
    }

    val obj = Data(42)
    Context(obj).trigger === 42 * 2
  }
}
