package scaladci
package test

import org.scalatest.FreeSpec

class OverLoadingTest extends FreeSpec {

  case class Obj() {
    def foo = "foo in Self"
  }

  case class Ctx() extends Context {
    private val rp = Obj().as[Role]

    def overrideInRole = rp.foo
    def alternativeSignature(i: Int) = rp.foo(i)
    def selfFooFromRole = rp.selfFoo

    private trait Role {self: Obj =>
      //      def foo = "foo in Role" // Won't compile - needs override because of self type
      override def foo = "foo with override in Role"
      def foo(i: Int) = s"foo with other signature in Role, arg = $i"
      def selfFoo = self.foo // will call foo in Role!
    }
  }

  "OverLoading Role method" - {
    "with same signature" - {
      "has to override self method to compile" in {
        true
      }
      "and overriding will override self method ... interesting ;-)" in {
//        assert(Ctx().overrideInRole === "foo with override in Role")
        true
      }
    }
    "with different signature" - {
      "will overload self method" in {
        assert(Ctx().alternativeSignature(42) === "foo with other signature in Role, arg = 42")
      }
    }
  }

  "Self method with Role override" - {
    "cannot be called with self type prefix from Role" in {
      assert(Ctx().selfFooFromRole != "foo in Self")
      assert(Ctx().selfFooFromRole === "foo with override in Role")
    }
  }
}