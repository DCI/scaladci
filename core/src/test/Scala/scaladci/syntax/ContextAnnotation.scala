package scaladci
package syntax
import util._

class ContextAnnotation extends DCIspecification {

  // Context annotation ...

  "@context is standard (??)" >> {

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


  "`@use case class MyUseCase` is an elegant reference to \"Use Case\"" >> {

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
