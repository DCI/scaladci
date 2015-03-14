package scaladci
package semantics
import util._

class ObjectInstantiation extends DCIspecification {

  "In environment (passed to Context)" >> {

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


  "In Context" >> {

    @context
    case class Context(i: Int) {

      val myRole = Data(i)

      def trigger = myRole.foo

      role myRole {
        def foo = myRole.i * 2
      }
    }

    Context(42).trigger === 42 * 2
  }


  "As new variable referencing already instantiated object/RolePlayer" >> {

    @context
    case class Context(roleA: Data) {

      val roleB = roleA

      def trigger = roleB.bar

      role roleB {
        def bar = roleB.i * 2
      }
    }

    val obj = Data(42)
    Context(obj).trigger === 42 * 2
  }
}
