package scaladci
package syntax
import util._

class ContextAnnotation extends DCIspecification {

  "@context is standard (?)" >> {

    @context
    class Context(Foo: Data) {
      def execute = Foo.value
      role Foo {
        def value = self.i * 2
      }
    }
    val obj = Data(42)
    new Context(obj).execute === 84
  }


  "@dci could be meaningful too" >> {

    @dci
    class Context2(Foo: Data) {
      def execute = Foo.value
      role Foo {
        def value = self.i * 2
      }
    }
    val obj = Data(42)
    new Context2(obj).execute === 84
  }


  "@use with case class could be meaningful for @use cases" >> {

    @use case class MyUseCase(Foo: Data) {
      def execute = Foo.value
      role Foo {
        def value = self.i * 2
      }
    }
    val obj = Data(42)
    MyUseCase(obj).execute === 84
  }
}
