package scaladci
package semantics
import util._

class RoleBody extends DCIspecification {

  // Role body ...

  "Can define role method(s)" >> {

    @context
    case class Context(Foo: Data) {
      def trigger = Foo.bar

      role Foo {
        def bar = 2 * baz
        def baz = 3 * buz
        def buz = 4 * Foo.i
      }
    }
    Context(Data(5)).trigger === 2 * 3 * 4 * 5
  }


  "Cannot be assigned to a Role definition" >> {

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          role Foo = {}
        }
      """,
      "(1) Can't assign a Role body to `Foo`. Please remove `=` before the body definition")

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          role(Foo) = {}
        }
      """,
      "(2) Can't assign a Role body to `Foo`. Please remove `=` before the body definition")

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          role() = {}
        }
      """,
      "(8) `role` keyword without Role name is not allowed")

    @context
    class Context(Foo: Data, Bar: Data) {
      role Foo {} // ok without `=`
      role(Bar) {} // ok without `=`
    }

    success
  }


  "Cannot define state" >> {

    expectCompileError(
      """
        @context
        class Context(Foo: Int) {
          role Foo {
            val notAllowed = 42   // <-- no state in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `Foo`:
        |CODE: val notAllowed = 42
      """)

    expectCompileError(
      """
        @context
        class Context(Foo: Int) {
          role Foo {
            var notAllowed = 42   // <-- no state in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `Foo`:
        |CODE: var notAllowed = 42
      """)

    success
  }


  "Cannot define types aliases (??)" >> {

    expectCompileError(
      """
        @context
        class Context(Foo: Int) {
          role Foo {
            type notAllowed = String   // <-- no type definitions in Roles ...?!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `Foo`:
        |CODE: type notAllowed = String
      """)
    success
  }


  "Cannot define other types" >> {

    expectCompileError(
      """
        @context
        class Context(Foo: Int) {
          role Foo {
            class NoClass  // <-- no class definitions in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `Foo`:
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
        class Context(Foo: Int) {
          role Foo {
            case class NoCaseClass  // <-- no class definitions in Roles!
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `Foo`:
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
        class Context(Foo: Int) {
          role Foo {
            trait NoTrait
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `Foo`:
        |CODE: abstract trait NoTrait extends scala.AnyRef
      """)

    expectCompileError(
      """
        @context
        class Context(Foo: Int) {
          role Foo {
            object NoObject
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `Foo`:
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
        class Context(Foo: Int) {
          role Foo {
            role NestedRole {}
          }
        }
      """,
      """
        |Roles are only allowed to define methods.
        |Please remove the following code from `Foo`:
        |CODE: role.NestedRole(())
      """)

    success
  }
}