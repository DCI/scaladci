package scaladci
package semantics
import util._

class RoleBody extends DCIspecification {

  "Can define role method(s)" >> {

    @context
    case class Context(MyRole: Data) {
      def trigger = MyRole.bar

      role MyRole {
        def bar = 2 * baz
        def baz = 3 * buz
        def buz = 4 * MyRole.i
      }
    }
    Context(Data(5)).trigger === 2 * 3 * 4 * 5
  }


  "Cannot be assigned to a Role definition" >> {

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          role MyRole = {}
        }
      """,
      "(1) Can't assign a Role body to `MyRole`. Please remove `=` before the body definition")

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          role(MyRole) = {}
        }
      """,
      "(2) Can't assign a Role body to `MyRole`. Please remove `=` before the body definition")

    expectCompileError(
      """
        @context
        class Context(MyRole: Data) {
          role() = {}
        }
      """,
      "(8) `role` keyword without Role name is not allowed")

    @context
    class Context(RoleA: Data, RoleB: Data) {
      role RoleA {} // ok without `=`
      role(RoleB) {} // ok without `=`
    }

    success
  }


  "Cannot define state" >> {

    expectCompileError(
      """
        @context
        class Context(MyRole: Int) {
          role MyRole {
            val notAllowed = 42   // <-- no state in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `MyRole`:
        |CODE: val notAllowed = 42
      """)

    expectCompileError(
      """
        @context
        class Context(MyRole: Int) {
          role MyRole {
            var notAllowed = 42   // <-- no state in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `MyRole`:
        |CODE: var notAllowed = 42
      """)

    success
  }


  "Cannot define types aliases (??)" >> {

    expectCompileError(
      """
        @context
        class Context(MyRole: Int) {
          role MyRole {
            type notAllowed = String   // <-- no type definitions in Roles ...?!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `MyRole`:
        |CODE: type notAllowed = String
      """)
    success
  }


  "Cannot define other types" >> {

    expectCompileError(
      """
        @context
        class Context(MyRole: Int) {
          role MyRole {
            class NoClass  // <-- no class definitions in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `MyRole`:
        |CODE: class NoClass extends scala.AnyRef {
        |  def <init>() = {
        |    super.<init>();
        |    ()
        |  }
        |}
      """)

    expectCompileError(
      """
        @context
        class Context(MyRole: Int) {
          role MyRole {
            case class NoCaseClass()  // <-- no class definitions in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `MyRole`:
        |CODE: case class NoCaseClass extends scala.Product with scala.Serializable {
        |  def <init>() = {
        |    super.<init>();
        |    ()
        |  }
        |}
      """)

    expectCompileError(
      """
        @context
        class Context(MyRole: Int) {
          role MyRole {
            trait NoTrait
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `MyRole`:
        |CODE: abstract trait NoTrait extends scala.AnyRef
      """)

    expectCompileError(
      """
        @context
        class Context(MyRole: Int) {
          role MyRole {
            object NoObject
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `MyRole`:
        |CODE: object NoObject extends scala.AnyRef {
        |  def <init>() = {
        |    super.<init>();
        |    ()
        |  }
        |}
      """)

    success
  }


  "Cannot define a nested role" >> {

    expectCompileError(
      """
        @context
        class Context(MyRole: Int) {
          role MyRole {
            role NestedRole {}
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `MyRole`:
        |CODE: role.NestedRole(())
      """)

    success
  }
}