package scaladci
package semantics
import util._

class ObjectIdentity extends DCIspecification {

  "Is same when playing a Role" >> {

    @context
    case class Context(Foo: Data) {
      val Bar = Data(42)

      def foo = Foo.me
      def bar = Bar.me

      role Foo {
        def me = self
      }

      role Bar {
        def me = self
      }
    }

    val obj1 = Data(42)
    val obj2 = Data(42)

    // BEWARE:
    // `==` or `equals` only compares values
    (obj1 == obj2) === true
    (obj1 equals obj2) === true

    // whereas `eq` compares referential identity of objects
    (obj1 eq obj2) === false

    // Object identity is preserved
    (obj1 eq Context(obj1).foo) === true

    // Although `bar` has same value (Data(42)) as foo, they're still two different objects
    (obj1 eq Context(obj1).bar) === false
  }
}
