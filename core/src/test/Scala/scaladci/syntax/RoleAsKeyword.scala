package scaladci
package syntax
import scaladci.util.expectCompileError

import org.specs2.mutable._

class RoleAsKeyword extends Specification {

  case class Data(i: Int)


  "Methodless Roles" >> {

    @context class Context1(Foo: Data) {
      role Foo()
    }

    @context class Context2(Foo: Data) {
      role.Foo()
    }

    @context class Context3(Foo: Data) {
      role Foo {}
    }

    success
  }


  "Implemented Roles" >> {

    @context class Context(Foo: Data) {
      role Foo {
        def roleMethod = 1
      }
    }

    success
  }


  "Missing Role name" >> {

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          role
        }
      """,
      "(1) `role` keyword without Role name is not allowed")

    success
  }

  "Postfix danger avoided" >> {

    expectCompileError(
      """
        @context
        class Context(Foo: Data) {
          role Foo
        }
      """,
      "(1) To avoid postfix clashes, please write `role Foo {}` instead of `role Foo`")

    success
  }

  "Postfix clash avoided" >> {

    expectCompileError(
      """
        @context
        class Context(Foo: Data, Bar: Data) {
          role Bar // two lines after each other ...
          role Foo // ... unintentionally becomes `role.Foo(role).Bar`
        }
      """,
      "(2) To avoid postfix clashes, please write `role Bar {}` instead of `role Bar`")

    success
  }

}