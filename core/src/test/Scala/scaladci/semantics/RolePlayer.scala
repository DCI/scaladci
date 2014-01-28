package scaladci
package semantics
import util._

class RolePlayer extends DCIspecification {

  // A Role Player ...

  "Cannot play role outside a Context" >> {

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
      "value bar is not a member of RolePlayer.this.Data")

    success
  }
}
