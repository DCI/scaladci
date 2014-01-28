package scaladci
package semantics
import util._

class RolePlayer extends DCIspecification {

  // An object can play a Role when ...

  "Object is passed to Context" >> {

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

  "Object is instantiated in Context" >> {

    @context
    case class Context(i: Int) {
      val Foo = Data(i)
      def trigger = Foo.foo

      role Foo {
        def foo = Foo.i * 2
      }
    }

    Context(42).trigger === 42 * 2
  }

  "Object is a reference to another object/RolePlayer in Context" >> {

    @context
    case class Context(Foo: Data) {

      val Bar = Foo
      def trigger = Bar.bar

      role Bar {
        def bar = Bar.i * 2
      }
    }

    val obj = Data(42)
    Context(obj).trigger === 42 * 2
  }
}
