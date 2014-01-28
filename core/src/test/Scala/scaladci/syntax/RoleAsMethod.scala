package scaladci
package syntax
import util._

class RoleAsMethod extends DCIspecification {

  // `role` as method ...

  "Can define implemented Roles" >> {

    @context class Context(Foo: Data) {
      role(Foo) {
        def roleMethod = 1
      }
    }

    success
  }


  "Can define methodless Roles" >> {

    @context class Context2(Foo: Data) {
      role(Foo)()
    }

    @context class Context3(Foo: Data) {
      role(Foo) {}
    }
    success
  }


  "Needs a Role name" >> {

    expectCompileError(
      """
          @context
          class Context {
            role()
          }
      """,
      "(2) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context {
            role{}
          }
      """,
      "(3) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context(Foo: Data) {
            role(null)
          }
      """,
      "(3) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context(Foo: Data) {
            role()()
          }
      """,
      "(4) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context(Foo: Data) {
            role(){}
          }
      """,
      "(5) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context(Foo: Data) {
            role{}()
          }
      """,
      "(6) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context(Foo: Data) {
            role{}{}
          }
      """,
      "(7) `role` keyword without Role name is not allowed")

    success
  }
}