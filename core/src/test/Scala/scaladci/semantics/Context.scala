package scaladci
package semantics
import util._

class Context extends DCIspecification {

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
        class OuterContext2(MyRole: Data) {
          role MyRole {
            @context
            class NestedContext2
          }
        }
      """,
      "Can't define nested DCI context `NestedContext2` inside DCI context `OuterContext2`")

    expectCompileError(
      """
        @context
        class OuterContext3(MyRole: Data) {
          role MyRole {
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


  "Todo: Can instantiate a nested Context" >> {

    // Todo
    ok
  }


  "Todo: Can play a Role in another Context" >> {

    // Todo
    ok
  }



  // "Only one DCI Context can be active at a time"
  // Isn't that only possible with parallel execution ??

  "Todo: Cannot be active at the same time as another Context" >> {

    // Todo ??
    ok
  }
}