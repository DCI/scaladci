package scaladci
package test

import org.scalatest.FreeSpec

// Todo: See comments in Context - this should be revised!!

class ContextParamConstraintsTest extends FreeSpec {

  case class Obj() {
    def foo() { x = 1 }
    def bar[T](y: T) = { y.toString }
    var x = 0
  }

//    case class Ctx(var obj: Obj) extends Context {  // won't compile var
  case class Ctx(obj: Obj) extends Context {
    private val rp = obj.as[Role]

    // Uncomment to see compile time error messages:

//    obj.x = -1
//    obj.foo()
//    obj.bar(new Obj())
//    val p = obj

    def run() {
      // violations one level deeper also stopped
//      obj.x = -2
//      obj.foo()
//      obj.bar(new Obj())
//      val q = obj
    }

    private trait Role {self: Obj =>
      def doRoleStuff() {
        self.x = 42 // OK

        // violations on deeper nested levels also stopped
//        obj.x = -3
//        obj.foo()
//        obj.bar(new Obj())
//        val r = obj
      }
    }
  }

  "Context parameters" - {
    "Are immutable" in {
      // case class Ctx(var obj: Obj) extends Context  doesn't compile
      true
    }
  }
  "Data objects" - {
    "Cannot set object field value" in {
      // obj.x = -1  doesn't compile
      true
    }
    "Cannot call object method" in {
      // obj.foo()  doesn't compile
      true
    }
    "Cannot call object method with type parameter" in {
      // obj.bar(new Obj())  doesn't compile
      true
    }
    "Cannot assign object to val/var" in {
      // val q = obj  doesn't compile
      true
    }
  }
}
