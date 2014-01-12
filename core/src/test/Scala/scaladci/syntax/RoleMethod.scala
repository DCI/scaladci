//package scaladci
//package syntax
//
//import org.specs2.mutable._
//import scaladci.util.expectCompileError
//
//class RoleMethod extends Specification {
//
//  case class Data(number: Int)
//
//  "Can" >> {
//
//    "Have side effects" >> {
//
//      @context
//      case class Context(Foo: Data) {
//        def trigger = Foo.roleMethod
//
//        role Foo {
//          def roleMethod() {
//            println("fire missil")
//          }
//        }
//      }
//      Context(Data(7)).trigger
//      ok // Can fire missil
//    }
//
//
//    "Have side effect" >> {
//
//      @context
//      case class Context(Foo: Data) {
//        def trigger = Foo.roleMethod
//
//        role Foo {
//          def roleMethod() = {
//
//          }
//        }
//      }
//      Context(Data(7)).trigger
//      ok // Can call trigger to print "side effect"
//    }
//
//  }
//
//
//  "Can't define" >> {
//
//    "val" >> {
//
//      success
//    }
//
//  }
//}
