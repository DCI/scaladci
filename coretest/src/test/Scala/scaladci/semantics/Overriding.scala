package scaladci
package semantics
import org.specs2.mutable._

class Overriding extends Specification {


  "Role method overrides data class method" >> {

    case class Data() {
      def number = 1
    }

    @context
    case class Context(myRole: Data) {

      def trigger = myRole.number

      role myRole {
        def number = 2
      }
    }

    Context(Data()).trigger === 2

  }
}
