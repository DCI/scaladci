package scaladci
package semantics
import scaladci.util._

class NestedContexts extends DCIspecification {

  "Nested" >> {

    @context
    case class Inner(roleB: Data) {
      def trigger = roleB.bar

      role roleB {
        def bar = roleB.i
      }
    }

    @context
    case class Outer(roleA: Data) {
      def trigger = roleA.foo

      role roleA {
        // Sequential execution of inner context
        def foo = "foo" + Inner(roleA).trigger + "bar"
      }
    }

    val obj = Data(42)
    Outer(obj).trigger === "foo42bar"
  }
}
