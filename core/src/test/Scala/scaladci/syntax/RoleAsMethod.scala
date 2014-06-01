package scaladci
package syntax
import util._

class RoleAsMethod extends DCIspecification {

  "Can define implemented Roles" >> {

    @context class Context(MyRole: Data) {
      role(MyRole) {
        def roleMethod = 1
      }
    }

    success
  }


  "Can define methodless Roles" >> {

    @context class Context3(MyRole: Data) {
      role(MyRole) {}
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
          class Context(MyRole: Data) {
            role(null)
          }
      """,
      "(3) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context(MyRole: Data) {
            role()()
          }
      """,
      "(4) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context(MyRole: Data) {
            role(){}
          }
      """,
      "(5) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context(MyRole: Data) {
            role{}()
          }
      """,
      "(6) `role` keyword without Role name is not allowed")

    expectCompileError(
      """
          @context
          class Context(MyRole: Data) {
            role{}{}
          }
      """,
      "(7) `role` keyword without Role name is not allowed")

    success
  }


  "Needs a Role body" >> {

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          role(MyRole)
        }
      """,
      "missing arguments for method role in package scaladci")

    success
  }


  "Cannot use constant as Role name (?!)" >> {

    expectCompileError(
      """
          @context
          class Context(MyRole: Data) {
            role("MyRole")
          }
      """,
      "(1) String as role name identifier is not allowed. Please use a variable instead. Found: MyRole")


    expectCompileError(
      """
          @context
          class Context(MyRole: Data) {
            role(42)
          }
      """,
      "(2) Integer as role name identifier is not allowed. Please use a variable instead. Found: 42 ")

    expectCompileError(
      """
          @context
          class Context(MyRole: Data) {
            role(42.0)
          }
      """,
      "(3) Double as role name identifier is not allowed. Please use a variable instead. Found: 42.0")

    expectCompileError(
      """
          @context
          class Context(MyRole: Data) {
            role(42f)
          }
      """,
      "(4) Float as role name identifier is not allowed. Please use a variable instead. Found: 42.0")

    success
  }
}