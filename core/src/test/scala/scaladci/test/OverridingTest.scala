package scaladci
package test

import org.scalatest.FreeSpec

class OverridingTest extends FreeSpec {

  case class Instance() {
    def foo = "foo in instance"
  }

  case class Ctx() extends Context {
    private val rp = Instance().as[Role]

    def overrideInRole = rp.foo
    def overloadInRole(i: Int) = rp.foo(i)
    def selfFooFromRole = rp.selfFoo

    private trait Role {self: Instance =>
      //      def foo = "foo in Role" // Won't compile - needs override because of self type
      override def foo = "foo override in Role"
      def foo(i: Int) = s"foo overload in Role, arg = $i"
      def selfFoo = self.foo // will call foo in Role, since foo is explicitly overriden in the Role!
    }
  }

  "Overriding Role method" - {
    "won't compile without override modifier" in {
      // won't compile (try unchecking line 20)
      true
    }
    "will compile with override modifier" in {
      assert(Ctx().overrideInRole === "foo override in Role")
    }
    "called via self calls overriden method in Role" in {
      assert(Ctx().selfFooFromRole === "foo override in Role")
    }

  }
  "Overloading Role method works" - {
    assert(Ctx().overloadInRole(42) === "foo overload in Role, arg = 42")
  }
}