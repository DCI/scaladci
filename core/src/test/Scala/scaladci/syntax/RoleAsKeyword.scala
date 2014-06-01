package scaladci
package syntax
import util._

class RoleAsKeyword extends DCIspecification {

  "Can define implemented Roles" >> {

    @context class Context(MyRole: Data) {
      role MyRole {
        def roleMethod = 42
      }
    }

    success
  }


  "Can define methodless Roles" >> {

    @context class Context1(MyRole: Data) {
      role MyRole()
    }

    @context class Context2(MyRole: Data) {
      role.MyRole()
    }

    @context class Context3(MyRole: Data) {
      role MyRole {}
    }

    success
  }


  "Can only define Roles" >> {

    @context
    class ContextWithOkRoleUse(MyRole: Data) {
      role MyRole {}
    }

    // Rejected uses of `role` keyword
    //    @context class Context(MyRole: Data) {role =>
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
    //      role.MyRole
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
        class Context(MyRole: Data) {
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
        class Context(MyRole: Data) {
          role MyRole
        }
      """,
      "(1) To avoid postfix clashes, please write `role MyRole {}` instead of `role MyRole`")

    @context
    class Context1(MyRole: Data) {
      role MyRole {} // with body
    }

    expectCompileError(
      """
        @context
        class Context(RoleA: Data, RoleB: Data) {
          role RoleA // two lines after each other ...
          role RoleB // ... would unintentionally become `role.RoleA(role).RoleB`
        }
      """,
      "(2) To avoid postfix clashes, please write `role RoleA {}` instead of `role RoleA`")

    @context
    class Context2(RoleA: Data, RoleB: Data) {
      role RoleA {}
      role RoleB {}
    }

    success
  }
}