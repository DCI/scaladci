package scaladci
package semantics
import scaladci.util._

class Interactions extends DCIspecification {

  "Are preferably distributed between Roles" >> {

    @context
    case class Context(data: Data) {
      val roleA = data
      val roleB = data
      val roleC = data

      def distributedInteractions = roleA.foo

      role roleA {
        def foo = 2 * roleB.bar
      }

      role roleB {
        def bar = 3 * roleC.baz
      }

      role roleC {
        def baz = 4 * self.i
      }
    }
    Context(Data(5)).distributedInteractions === 2 * 3 * 4 * 5
  }


  "Can occasionally be centralized in Context (mediator pattern)" >> {

    @context
    case class Context(data: Data) {
      val roleA = data
      val roleB = data
      val roleC = data

      def centralizedInteractions = roleA.foo * roleB.bar * roleC.baz * roleC.number

      role roleA {
        def foo = 2
      }

      role roleB {
        def bar = 3
      }

      role roleC {
        def baz = 4
      }
    }
    Context(Data(5)).centralizedInteractions === 2 * 3 * 4 * 5
  }

}
