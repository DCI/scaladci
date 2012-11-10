package scaladci
package test
import scala.language.reflectiveCalls

import org.scalatest.FreeSpec

class  RoleContractTest extends FreeSpec {

  case class Obj() {
    def foo = "foo"
    def bar = "bar"
  }

  case class Ctx() extends Context {
    private val rp = Obj().as[Role]

    def run = rp.selfbar

    private trait Role {self: {def bar: String} =>

      def selfbar = self.bar
      //      def selffoo = self.foo  // can't compile
    }
  }
  "Role Contract as anonymous self type" - {
    "Methods defined in self type can be called" in {
      assert(Ctx().run === "bar")
    }
    "Methods of Obj not in self type are not accessible" in {
      // self.foo in Role won't compile
      true
    }
  }

}