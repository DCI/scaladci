package scaladci
package syntax
import util._

class RoleAsKeyword extends DCIspecification {

  "Can define implemented Roles" >> {

    @context class Context(myRole: Data) {
      role myRole {
        def roleMethod = 42
      }
    }

    success
  }


  "Can define methodless Roles" >> {

    @context class Context1(myRole: Data) {
      role myRole()
    }

    @context class Context2(myRole: Data) {
      role.myRole()
    }

    @context class Context3(myRole: Data) {
      role myRole {}
    }

    success
  }


  "Can only define Roles" >> {

    @context
    class ContextWithOkRoleUse(myRole: Data) {
      role myRole {}
    }

    // Rejected uses of `role` keyword
    //    @context class Context(myRole: Data) {role =>
    //
    //      val value = role
    //      var value = role
    //
    //      val role = 42
    //      var role = 42
    //
    //      def role() = 42
    //
    //      class role
    //      case class role()
    //      trait role
    //      object role {}
    //
    //      type role
    //
    //      val x = this.role
    //      role.myRole
    //    }

    expectCompileError(
      """
        @context
        class Context {
          role =>
        }
      """,
      "Using `role` keyword as a template name is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          val foo = role
        }
      """,
      "Using `role` keyword as a return value is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          val role = 42  // val
        }
      """,
      "Using `role` keyword as a variable name is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          val role = 42  // var
        }
      """,
      "Using `role` keyword as a variable name is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          def role() = 42
        }
      """,
      "Using `role` keyword as a method name is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          def hej = {
            trait role
            42
          }
        }
      """,
      "Using `role` keyword as a trait name is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          trait role
        }
      """,
      "Using `role` keyword as a trait name is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          case class role()
        }
      """,
      "Using `role` keyword as a case class name is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          class role
        }
      """,
      "Using `role` keyword as a class name is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          object role
        }
      """,
      "Using `role` keyword as an object name is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          type role
        }
      """,
      "Using `role` keyword as a type alias is not allowed")

    expectCompileError(
      """
        @context
        class Context {
          val x = this.role
        }
      """,
      "Using `role` keyword as a selector name after a quantifier is not allowed")

    success
  }


  "Needs a Role name" >> {

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role
        }
      """,
      "(1) `role` keyword without Role name is not allowed")

    success
  }


  "Needs body to avoid postfix clashes" >> {

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role myRole
        }
      """,
      "(1) To avoid postfix clashes, please write `role myRole {}` instead of `role myRole`")

    @context
    class Context1(myRole: Data) {
      role myRole {} // with body
    }

    expectCompileError(
      """
        @context
        class Context(roleA: Data, roleB: Data) {
          role roleA // two lines after each other ...
          role roleB // ... would unintentionally become `role.roleA(role).roleB`
        }
      """,
      "(2) To avoid postfix clashes, please write `role roleA {}` instead of `role roleA`")

    @context
    class Context2(roleA: Data, roleB: Data) {
      role roleA {}
      role roleB {}
    }

    success
  }
}