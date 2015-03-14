package scaladci
package semantics
import scaladci.util._

class RoleDefinition extends DCIspecification {

  "Can only be at top level in Context" >> {

    @context
    class OkContext(myRole: Data) {
      role myRole {}
    }

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          def subLevel() {
            role myRole (()) // Supplying Unit argument as `()`
          }
        }
      """,
      "(1) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          def subLevel() {
            role myRole {}
          }
        }
      """,
      "(1) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          def subLevel() {
            role(myRole)
          }
        }
      """,
      "(2) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          def subLevel() {
            role(myRole) {}
          }
        }
      """,
      "(3) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          def subLevel() {
            role myRole
          }
        }
      """,
      "(4) Using `role` keyword on a sub level of the Context is not allowed")

    success
  }


  "Cannot be inside another Role definition" >> {

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role(role myRole)
        }
      """,
      "(2) Using `role` keyword on a sub level of the Context is not allowed")

    success
  }


  "Cannot be defined twice (defining same Role name twice)" >> {

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role myRole {}
          role myRole {}
        }
      """,
      "Can't define role `myRole` twice")

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role(myRole)(())
          role(myRole)(())
        }
      """,
      "Can't define role `myRole` twice")

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role(myRole){}
          role(myRole){}
        }
      """,
      "Can't define role `myRole` twice")

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role myRole {}
          role(myRole){}
        }
      """,
      "Can't define role `myRole` twice")

    success
  }
}