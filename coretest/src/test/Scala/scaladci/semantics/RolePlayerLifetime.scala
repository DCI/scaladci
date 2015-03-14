package scaladci
package semantics
import util._

class RolePlayerLifetime extends DCIspecification {

  "Is limited to Context execution" >> {

    @context
    case class Context(myRole: Data) {
      def trigger = myRole.bar

      role myRole {
        def bar = myRole.i * 2
      }
    }
    val obj = Data(42)

    // `obj` plays myRole in Context
    Context(obj).trigger === 42 * 2

    // `obj` no longer plays myRole
    expectCompileError(
      "obj.bar",
      "value bar is not a member of RolePlayerLifetime.this.Data")

    success
  }
}
