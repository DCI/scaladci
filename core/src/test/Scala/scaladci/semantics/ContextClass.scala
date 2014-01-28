package scaladci
package semantics
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

  "Cannot define a nested DCI Context" >> {

    expectCompileError(
      """
        @context
        class OuterContext1 {
          @context
          class NestedContext1
        }
      """,
      "Can't define nested DCI context `NestedContext1` inside DCI context `OuterContext1`")

    expectCompileError(
      """
        @context
        class OuterContext2(Foo: Data) {
          role Foo {
            @context
            class NestedContext2
          }
        }
      """,
      "Can't define nested DCI context `NestedContext2` inside DCI context `OuterContext2`")

    expectCompileError(
      """
        @context
        class OuterContext3(Foo: Data) {
          role Foo {
            def roleMethod {
              @context
              class NestedContext3
            }
          }
        }
      """,
      "Can't define nested DCI context `NestedContext3` inside DCI context `OuterContext3`")

    success
  }
}