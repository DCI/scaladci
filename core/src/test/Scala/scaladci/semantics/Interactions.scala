package scaladci
package semantics
import util._

class Interactions extends DCIspecification {

  // Interactions ...

  "Are preferably distributed between Roles" >> {

    @context
    case class Context(a: Data) {
      val b = a
      val c = a

      def distributedInteractions = a.foo

      role a {
        def foo = 2 * b.bar
      }

      role b {
        def bar = 3 * c.baz
      }

      role c {
        def baz = 4 * self.i
      }
    }
    Context(Data(5)).distributedInteractions === 2 * 3 * 4 * 5
  }


  "Can occasionally be centralized in Context" >> {

    @context
    case class Context(a: Data) {
      val b = a
      val c = a

      def centralizedInteractions = a.foo * b.bar * c.baz * c.number

      role a {
        def foo = 2
      }

      role b {
        def bar = 3
      }

      role c {
        def baz = 4
      }
    }
    Context(Data(5)).centralizedInteractions === 2 * 3 * 4 * 5
  }

}
