package scaladci
package semantics
import util._

class RoleDefinition extends DCIspecification {

  "Can only be at top level in Context" >> {

    @context
    class OkContext(MyRole: Data) {
      role MyRole {}
    }

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          def subLevel() {
            role MyRole ()
          }
        }
      """,
      "(1) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          def subLevel() {
            role MyRole {}
          }
        }
      """,
      "(1) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          def subLevel() {
            role(MyRole)
          }
        }
      """,
      "(2) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          def subLevel() {
            role(MyRole) {}
          }
        }
      """,
      "(3) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          def subLevel() {
            role MyRole
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
        class Context(MyRole: Data) {
          role(role MyRole)
        }
      """,
      "(2) Using `role` keyword on a sub level of the Context is not allowed")

    success
  }


  "Cannot be defined twice (defining same Role name twice)" >> {

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          role MyRole {}
          role MyRole {}
        }
      """,
      "Can't define role `MyRole` twice")

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          role(MyRole)()
          role(MyRole)()
        }
      """,
      "Can't define role `MyRole` twice")

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          role MyRole {}
          role(MyRole)()
        }
      """,
      "Can't define role `MyRole` twice")

    success
  }
}