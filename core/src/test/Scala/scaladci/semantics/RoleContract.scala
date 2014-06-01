package scaladci
package semantics

import util._
import scala.language.reflectiveCalls

class RoleContract extends DCIspecification {

  "Can be a type" >> {

    @context
    case class Context(MyRole: Data) {

      def trigger = MyRole.foo

      role MyRole {
        def foo = self.number // We know type `Data` (and that it has a number method)
      }
    }
    Context(new Data(42)).trigger === 42
  }


  "Can be a structural type (duck typing)" >> {

    @context
    case class Context(MyRole: {def number: Int}) {

      def trigger = MyRole.foo

      role MyRole {
        // We know that the instance (of unknown type) has a `number` method returning Int
        def foo = self.number
      }
    }
    Context(new Data(42)).trigger === 42


    case class NastyData(i: Int) {
      def number = {
        println("Firing missiles...")
        i
      }
    }

    @context
    case class NaiveContext(MyRole: {def number: Int}) {

      def trigger = MyRole.foo

      role MyRole {
        // We know that the instance (of unknown type) has a `number` method returning Int
        // - but we don't know that it also fire off missiles!!!
        def foo = self.number
      }
    }
    NaiveContext(NastyData(42)).trigger === 42 // + world war III
  }


  "Can be a mix of a type and a structural type" >> {

    class Data(i: Int) {
      def number = i
    }
    case class OtherData(i: Int) extends Data(i) {
      def text = "My number is: "
    }

    @context
    case class Context(MyRole: Data {def text: String}) { // <- OtherData will satisfy this contract

      def trigger = MyRole.foo

      role MyRole {
        def foo = self.text + self.number// `Data` has a `number` method and there should also be some `text` method...
      }
    }
    Context(OtherData(42)).trigger === "My number is: 42"
  }


  "Can naïvely rely on a type" >> {

    class Data(i: Int) {
      def number = i
    }
    case class UntrustworthyData(i: Int) extends Data(i) {
      override def number = -666
    }

    @context
    case class Context(MyRole: Data) { // <- We feel falsely safe :-(

      def trigger = MyRole.foo

      role MyRole {
        def foo = self.number
      }
    }
    val externalTrustedObject = UntrustworthyData(42)
    Context(externalTrustedObject).trigger === -666 // Not 42 !!! Auch
  }
}
