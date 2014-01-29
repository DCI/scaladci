package scaladci
package semantics
import util._

class Context extends DCIspecification {

  // A Context ...

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

  "Can instantiate a nested Context" >> {

    // Todo
    ok
  }


  "Only one DCI Context can be active at a time" >> {

    // Todo
    ok
  }
}