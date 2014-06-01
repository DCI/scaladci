package scaladci
package semantics
import util._

class RolePlayerLifetime extends DCIspecification {

  "Is limited to Context execution" >> {

    @context
    case class Context(Foo: Data) {
      def trigger = Foo.bar

      role Foo {
        def bar = Foo.i * 2
      }
    }
    val obj = Data(42)

    // `obj` plays Foo in Context
    Context(obj).trigger === 42 * 2

    // `obj` no longer plays Foo
    expectCompileError(
      "obj.bar",
      "value bar is not a member of RolePlayerLifetime.this.Data")

    success
  }
}
