//package scaladci
//package syntax
//
//import org.specs2.mutable._
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
//  "Using `role` as method" >> {
//
//    @context
//    class Context(Bar: Data) {
//
//
//      role(Bar)
//
//      role(Bar)()
//
//      role(Bar) {}
//
//      role(Bar) {
//        def m = 1
//      }
//
//    }
//
//    success
//  }
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
