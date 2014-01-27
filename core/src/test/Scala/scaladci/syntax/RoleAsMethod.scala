package scaladci
package syntax
import util.expectCompileError

import org.specs2.mutable._

class RoleAsMethod extends Specification {

  case class Data(i: Int)


  "Methodless Roles" >> {

//    @context class Context1(Foo: Data) {
      //      role =>
      //      val x = 7
      //      val y = role.x
      //      val role = 9

      //      case class role {}

      //            val value = role

      //      val role = 8
      //      class role {}
      //       trait role {}
      //      type role = String
      //      object role {}
      //      val x = this.role

      //      //      role(null)
      //      //      role()
      //      role(Foo)
      //      role(Foo)()
      //      role(Foo) {}
      //
      //      val x = 1
      //      role.Foo
      //
      //      // noRoleDefinitions kicking in:
      //      //      role(role Foo)
      //      //      role(role Foo)()
      //      //      role(role Foo) {}
      //
      //      // nested role definitions not allowed
      ////      def hej() {
      ////        role Foo {}
      ////      }
      //      def hey() {
      ////          role Foo {}
      //        def you() {
      //          val role = 7
      //        }
      //      }
      //
      //
      //      val role = 1
      //
      //      //
      //      role(Foo)(role(Foo))
      //      role(Foo) {role(Foo)}
      //      role(Foo) {
      //        role Foo()
      //      }
      //
      //
      //      val z = 1
      //
      //
      //      role Foo {role Foo {}}
      //      role Foo {
      //        role Foo {
      //
      //        }
      //      }
      //
      //      role Foo
      //        role Foo {}
      //
      //      role Foo
      //        role Foo()
      //
      //      role(Foo) Foo
      //
      //      role(Foo) = Foo
//
//    }

    @context class Context2(Foo: Data) {
      role(Foo)()
    }

    @context class Context3(Foo: Data) {
      role(Foo) {}
    }
    success
  }


  "Implemented Roles" >> {

    @context class Context(Foo: Data) {
      role(Foo) {
        def roleMethod = 1
      }
    }

    success
  }


  "Missing Role name" >> {

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