package scaladci
package syntax
import util._

class RoleMethod extends DCIspecification {

  // A Role method ...

  "Can have side effects" >> {

    @context
    case class Context(Foo: Data) {
      def trigger = Foo.roleMethod

      role Foo {
        def roleMethod() {
          println("fire missil")
        }
      }
    }
    Context(Data(7)).trigger
    ok // Can fire missil
  }


//  "Have side effect" >> {
//
//    @context
//    case class Context(Foo: Data) {
//      def trigger = Foo.roleMethod
//
//      role Foo {
//        def roleMethod() = {
//
//        }
//      }
//    }
//    Context(Data(7)).trigger
//    ok // Can call trigger to print "side effect"
//  }
}
