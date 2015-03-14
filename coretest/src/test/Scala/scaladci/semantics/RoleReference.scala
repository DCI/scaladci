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


  "Can be `this`" >> {

    @context
    case class Context(myRole: Data) {
      def useSelf = myRole.value

      role myRole {
        def value = this.i * 2
      }
    }

    val obj = Data(42)

    Context(obj).useSelf === 42 * 2
  }
}