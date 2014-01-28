package scaladci
package syntax
import util._

class ContextClass extends DCIspecification {

  // A Context class ...

  "Can only be a class, case class or object" >> {

    @context
    class ClassContext(Foo: Data) {
      role Foo {}
    }

    @context
    case class CaseClassContext(Foo: Data) {
      role Foo {}
    }

    @context
    object ObjectContext {
      // Objects to play a Role are instantiated inside the Context object
      val Foo = Data(42)
      role Foo {}
    }

    expectCompileError(
      """
        @context
        trait TraitContext
      """,
      "Using a trait as a DCI context is not allowed")

    expectCompileError(
      """
        @context
        abstract class AbstractClassContext
      """,
      "Using abstract class as a DCI context is not allowed")

    expectCompileError(
      """
        @context
        val outOfContext = "no go"
      """,
      """
        |Only classes/case classes/objects can be transformed to DCI Contexts. Found:
        |val outOfContext = "no go"
      """)

    success
  }

  "Can be an empty stub" >> {

    @context
    class EmptyClassContext

    success
  }

  "Cannot be named `role`" >> {

    expectCompileError(
      """
        @context
        class role
      """,
      "Context class can't be named `role`")

    success
  }
}