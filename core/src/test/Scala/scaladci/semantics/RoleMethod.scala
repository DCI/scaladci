package scaladci
package semantics
import util._

class RoleMethod extends DCIspecification {

  // Role method ...

  "Takes precedence over instance method" >> {

    @context
    case class Context1(Foo: Data) {
      def num = Foo.number

      role Foo {
        def number = 43
      }
    }

    val obj = Data(42)
    Context1(obj).num === 43



    // https://groups.google.com/d/msg/object-composition/QfvHXzuP2wU/NwNYnBa10JQJ

    case class Inst() {
      override def toString = "a"
      def foo(str: String) = str + "b"
    }

    @context
   case class Context2() {
      val a = Inst()
      val b = Inst()

      def execute = a.foo

      role a {
        def foo = {
          b.bar(a.toString())
        }

      }

      role b {
        def bar(str: String) = b.foo(str)
      }
    }

    Context2().execute === "ab"




    @context
    case object Context3 {
      val a = Inst()
      val b = Inst()

      def execute = a.foo

      role a {
        def foo = {
          b.bar(a.toString())
        }

      }

      role b {
        def bar(str: String) = b.foo(str)
      }
    }

    Context3.execute === "ab"
  }
}
