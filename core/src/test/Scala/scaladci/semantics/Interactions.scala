package scaladci
package semantics
import util._

class Interactions extends DCIspecification {

  "Are preferably distributed between Roles" >> {

    @context
    case class Context(RoleA: Data) {
      val RoleB = RoleA
      val RoleC = RoleA

      def distributedInteractions = RoleA.foo

      role RoleA {
        def foo = 2 * RoleB.bar
      }

      role RoleB {
        def bar = 3 * RoleC.baz
      }

      role RoleC {
        def baz = 4 * self.i
      }
    }
    Context(Data(5)).distributedInteractions === 2 * 3 * 4 * 5
  }


  "Can occasionally be centralized in Context" >> {

    @context
    case class Context(RoleA: Data) {
      val RoleB = RoleA
      val RoleC = RoleA

      def centralizedInteractions = RoleA.foo * RoleB.bar * RoleC.baz * RoleC.number

      role RoleA {
        def foo = 2
      }

      role RoleB {
        def bar = 3
      }

      role RoleC {
        def baz = 4
      }
    }
    Context(Data(5)).centralizedInteractions === 2 * 3 * 4 * 5
  }

}
