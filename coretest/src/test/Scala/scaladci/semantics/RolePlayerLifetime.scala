package scaladci
package semantics
import util._

class RolePlayerLifetime extends DCIspecification {

  "Is limited to Context execution" >> {

    @context
    case class Context(MyRole: Data) {
      def trigger = MyRole.bar

      role MyRole {
        def bar = MyRole.i * 2
      }
    }
    val obj = Data(42)

    // `obj` plays MyRole in Context
    Context(obj).trigger === 42 * 2

    // `obj` no longer plays MyRole
    expectCompileError(
      "obj.bar",
      "value bar is not a member of RolePlayerLifetime.this.Data")

    success
  }
}
