package scaladci
package semantics
import util._

class RoleReference extends DCIspecification {

  "Can be a Role name" >> {

    @context
    case class Context(myRole: Data) {
      def useRoleName = myRole.value

      role myRole {
        def value = myRole.i * 2
      }
    }

    val obj = Data(42)

    Context(obj).useRoleName === 42 * 2
  }


  "Can be `self`" >> {

    @context
    case class Context(myRole: Data) {
      def useSelf = myRole.value

      role myRole {
        def value = self.i * 2
      }
    }

    val obj = Data(42)

    Context(obj).useSelf === 42 * 2
  }


  "Cannot be `this` (not Scala ideomatic)" >> {

    // According to Scala semantics, `this` in the example below is pointing to the
    // Context instance. It would be confusing to let it point to the Role Player and
    // since we can access the Context members anyway without it, we disallow using it
    // inside a DCI context:

    expectCompileError(
      """
        @context
        case class Context(myRole: Data) {
          def useThis = myRole.value

          role myRole {
            def value = this.i * 2
          }
        }
      """,
      """
        |`this` in a role method points to the Context which is unintentional from a DCI perspective (where it would normally point to the RolePlayer).
        |Please access Context members directly if needed or use `self` to reference the Role Player.
      """)

    success
  }
}