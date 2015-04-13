//package scaladci
//package semantics
//import org.specs2.mutable._
//
//class Overriding extends Specification {
//
//  case class Data() {
//    def number = 1
//  }
//
//  "Role method overrides data class method" >> {
//
//    @context
//    case class Context(myRole: Data) {
//
//      def trigger = myRole.number
//
//      role myRole {
//        def number = 2
//      }
//    }
//
//    Context(Data()).trigger === 2
//
//  }
//}
