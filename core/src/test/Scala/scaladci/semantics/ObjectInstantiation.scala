package scaladci
package semantics
import util._

class ObjectInstantiation extends DCIspecification {

  "In environment (passed to Context)" >> {

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


  "In Context" >> {

    @context
    case class Context(i: Int) {

      val Foo = new Data(i)

      def trigger = Foo.foo

      role Foo {
        def foo = Foo.i * 2
      }
    }

    Context(42).trigger === 42 * 2
  }


  "As new variable referencing already instantiated object/RolePlayer" >> {

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
