package scaladci
package test
import org.scalatest.FreeSpec

class RoleBindingTest extends FreeSpec {

  case class Obj() {
    def me = "me"
  }

  case class Ctx(obj: Obj = null) extends Context {
    private val rp = obj.as[Role]

    // Context methods =============================
    def sayFoo = rp.foo
    def sayFoo(obj: Obj) = (obj.as[Role]).foo
    def bindInRole = rp.playBoss
    def confirmReBind = {
      // Since Roles are private to the Context we collect some test here...
      val rp2 = rp.as[Role2]
      // rp.bar   // can't compile
      // rp2.foo  // can't compile
      (rp == rp2, rp equals rp2, rp eq rp2, rp.foo + rp2.bar)
    }
    def getSelf = rp.asSelf

    // Roles =======================================
    private trait Role {self: Obj =>
      def foo = "foo"
      def asSelf = self.asInstanceOf[Obj]
      def playBoss() {
        //        obj.as[Role2] // Role binding in Role won't compile
      }
    }
    private trait Role2 {self: Obj =>
      def bar = "bar"
    }
  }

  val obj = Obj()

  "Role binding" - {
    "Not possible in" - {
      "Environment - Roles are private to the Context" in {
        //        obj.as[Role] // Won't compile: Role trait nor the 'as' method are accessible outside the Context
        true
      }
      "Role - Macro detects binding calls in Roles at compile time" in {
        Ctx(obj).bindInRole // Won't compile (uncomment 'bindInRole' method in Role...)
        true
      }
    }
    "Possible in" - {
      "Context body" in {
        assert(Ctx(obj).sayFoo === "foo")
      }
      "Context method" in {
        assert(Ctx().sayFoo(obj) === "foo")
      }
    }
  }

  "Role re-binding" - {
    "Old Role Player doesn't know of new Role (of course)" in {
      true // 'rp.bar' doesn't compile in Role
    }
    "New Role Player doesn't know of Old Role (old methods removed)" in {
      true // 'rp2.foo' doesn't compile in Role
    }
    "Old and New Role Player" - {
      // TODO: No object playing two Roles at the same time! Remove these tests?
      val rebindResults = Ctx(obj).confirmReBind
      "Are structurally equal (content is the same)" in {
        assert(rebindResults._1 === true)
        assert(rebindResults._2 === true)
      }
      "Object identities are different" in {
        assert(rebindResults._3 === false)
      }
      "Can call their respective methods" in {
        assert(rebindResults._4 === "foobar")
      }
    }
  }

  "Role un-binding" - {
    val self = Ctx(obj).getSelf
    "Self can escape Role privacy" in {
      assert(self.me === "me")
    }
    "Role method not available to detached Self" in {
      // self.foo // doesn't compile
      true
    }
  }
}