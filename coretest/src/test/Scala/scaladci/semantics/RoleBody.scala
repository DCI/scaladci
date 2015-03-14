package scaladci
package semantics
import util._

class RoleBody extends DCIspecification {

  "Can define role method(s)" >> {

    @context
    case class Context(myRole: Data) {
      def trigger = myRole.bar

      role myRole {
        def bar = 2 * baz
        def baz = 3 * buz
        def buz = 4 * myRole.i
      }
    }
    Context(Data(5)).trigger === 2 * 3 * 4 * 5
  }


  "Cannot be assigned to a Role definition" >> {

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role myRole = {}
        }
      """,
      "(1) Can't assign a Role body to `myRole`. Please remove `=` before the body definition")

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role(myRole) = {}
        }
      """,
      "(2) Can't assign a Role body to `myRole`. Please remove `=` before the body definition")

    expectCompileError(
      """
        @context
        class Context(myRole: Data) {
          role() = {}
        }
      """,
      "(8) `role` keyword without Role name is not allowed")

    @context
    class Context(roleA: Data, roleB: Data) {
      role roleA {} // ok without `=`
      role(roleB) {} // ok without `=`
    }

    success
  }


  "Cannot define state" >> {

    expectCompileError(
      """
        @context
        class Context(myRole: Int) {
          role myRole {
            val notAllowed = 42   // <-- no state in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `myRole`:
        |CODE: val notAllowed = 42
      """)

    expectCompileError(
      """
        @context
        class Context(myRole: Int) {
          role myRole {
            var notAllowed = 42   // <-- no state in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `myRole`:
        |CODE: var notAllowed = 42
      """)

    success
  }


  "Cannot define types aliases (??)" >> {

    expectCompileError(
      """
        @context
        class Context(myRole: Int) {
          role myRole {
            type notAllowed = String   // <-- no type definitions in Roles ...?!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `myRole`:
        |CODE: type notAllowed = String
      """)
    success
  }


  "Cannot define other types" >> {

    expectCompileError(
      """
        @context
        class Context(myRole: Int) {
          role myRole {
            class NoClass  // <-- no class definitions in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `myRole`:
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
        class Context(myRole: Int) {
          role myRole {
            case class NoCaseClass()  // <-- no class definitions in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `myRole`:
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
        class Context(myRole: Int) {
          role myRole {
            trait NoTrait
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `myRole`:
        |CODE: abstract trait NoTrait extends scala.AnyRef
      """)

    expectCompileError(
      """
        @context
        class Context(myRole: Int) {
          role myRole {
            object NoObject
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `myRole`:
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
        class Context(myRole: Int) {
          role myRole {
            role nestedRole {}
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `myRole`:
        |CODE: role.nestedRole(())
      """)

    success
  }
}