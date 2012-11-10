package scaladci
package test
import org.scalatest.FreeSpec

class ObjectIdentityTest extends FreeSpec {

  case class A()
  trait B

  case class Ctx(a: A, ab: A with B = null) extends Context {
    private val rp  = a.as[Role]
    private val rp2 = ab.as[Role2]

    def objectIsEqualToRolePlayer = (rp equals a) && (rp == a)
    def objectIsIdenticalToRolePlayer = rp eq a
    def getAself = rp.a

    def compoundObjectIsEqualToRolePlayer = (rp2 equals ab) && (rp2 == ab)
    def compoundObjectIsIdenticalToRolePlayer = rp2 eq ab
    def getABselves = (rp2.a, rp2.b, rp2.ab, rp2.ba)

    private trait Role {self: A =>
      //      def x = self // self can't escape Role privacy
      def a = self.asInstanceOf[A]
    }
    private trait Role2 {self: A with B =>
      def a = self.asInstanceOf[A]
      def b = self.asInstanceOf[B]
      def ab = self.asInstanceOf[A with B]
      def ba = self.asInstanceOf[B with A]
    }
  }

  "Object compared to" - {
    val a = A()
    "Role Player" - {
      "is structurally equal" in {
        Ctx(a).objectIsEqualToRolePlayer
      }
      "is not referentially identical" in {
        !Ctx(a).objectIsIdenticalToRolePlayer
      }
    }
    "Self" - {
      val self = Ctx(a).getAself
      "is structurally equal" in {
        assert(self == a)
        assert(self equals a)
      }
      "is not referentially identical" in {
        assert(self ne a)
      }
    }
  }
  "Compound object compared to" - {
    val ab = new A() with B
    "Role Player" - {
      "is structurally equal" in {
        Ctx(null, ab).compoundObjectIsEqualToRolePlayer
      }
      "is not referentially identical" in {
        !Ctx(null, ab).compoundObjectIsIdenticalToRolePlayer
      }
    }
    "Self" - {
      val selves = Ctx(null, ab).getABselves
      "is structurally equal" in {
        assert(selves._3 equals ab)
      }
      "is not referentially identical" in {
        assert(selves._3 ne ab)
      }
    }

    // TODO should it be like this?:

    "Self with traits in different order" - {
      val selves = Ctx(null, ab).getABselves
      "is structurally equal" in {
        assert(selves._4 equals ab)
      }
      "is not referentially identical" in {
        assert(selves._4 ne ab)
      }
    }
    "selected traits of Self" - {
      val selves = Ctx(null, ab).getABselves
      "is structurally equal" in {
        assert(selves._1 equals ab)
        assert(selves._2 equals ab)
      }
      "is not referentially identical" in {
        assert(selves._1 ne ab)
        assert(selves._2 ne ab)
      }
    }
  }
}