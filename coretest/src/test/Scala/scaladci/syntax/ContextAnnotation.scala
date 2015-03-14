package scaladci
package syntax
import util._

class ContextAnnotation extends DCIspecification {

  "@context is standard (?)" >> {

    @context
    class Context(myRole: Data) {
      def execute = myRole.value
      role myRole {
        def value = self.i * 2
      }
    }
    val obj = Data(42)
    new Context(obj).execute === 84
  }


  "@dci could be meaningful too" >> {

    @dci
    class Context2(myRole: Data) {
      def execute = myRole.value
      role myRole {
        def value = self.i * 2
      }
    }
    val obj = Data(42)
    new Context2(obj).execute === 84
  }


  "@use with case class could be meaningful for @use cases" >> {

    @use case class MyUseCase(myRole: Data) {
      def execute = myRole.value
      role myRole {
        def value = self.i * 2
      }
    }
    val obj = Data(42)
    MyUseCase(obj).execute === 84
  }
}
