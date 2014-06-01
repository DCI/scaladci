package scaladci
package semantics
import util._

class RoleDefinition extends DCIspecification {

  "Can only be at top level in Context" >> {

    @context
    class OkContext(Foo: Data) {
      role Foo {}
    }

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          def subLevel() {
            role Foo ()
          }
        }
      """,
      "(1) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          def subLevel() {
            role Foo {}
          }
        }
      """,
      "(1) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          def subLevel() {
            role(Foo)
          }
        }
      """,
      "(2) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          def subLevel() {
            role(Foo) {}
          }
        }
      """,
      "(3) Using `role` keyword on a sub level of the Context is not allowed")

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          def subLevel() {
            role Foo
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
        class Context(Foo: Data) {
          role(role Foo)
        }
      """,
      "(2) Using `role` keyword on a sub level of the Context is not allowed")

    success
  }


  "Cannot be defined twice (defining same Role name twice)" >> {

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          role Foo {}
          role Foo {}
        }
      """,
      "Can't define role `Foo` twice")

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          role(Foo)()
          role(Foo)()
        }
      """,
      "Can't define role `Foo` twice")

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          role Foo {}
          role(Foo)()
        }
      """,
      "Can't define role `Foo` twice")

    success
  }
}