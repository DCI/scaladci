package scaladci
package semantics
import util._
import scala.language.reflectiveCalls

class RoleContract extends DCIspecification {

  // Role contract ...

  "Can be a type" >> {

    @context
    case class Context(a: Data) {

      def trigger = a.foo

      role a {
        def foo = self.number // We know type `Data` (and that it has a number method)
      }
    }
    Context(Data(42)).trigger === 42
  }


  "Can be a structural type (duck typing)" >> {

    @context
    case class Context(a: {def number: Int}) {

      def trigger = a.foo

      role a {
        // We know that the instance (of unknown type) has a `number` method returning Int
        def foo = self.number
      }
    }
    Context(Data(42)).trigger === 42


    case class NastyData(i: Int) {
      def number = {
        println("Firing missiles...")
        i
      }
    }

    @context
    case class NaiveContext(a: {def number: Int}) {

      def trigger = a.foo

      role a {
        // We know that the instance (of unknown type) has a `number` method returning Int
        // - but we don't know that it also fire off missiles!!!
        def foo = self.number
      }
    }
    NaiveContext(NastyData(42)).trigger === 42 // + world war III
  }
}
