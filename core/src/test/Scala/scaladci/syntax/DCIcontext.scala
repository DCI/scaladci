package scaladci
package syntax
import util._

class DCIcontext extends DCIspecification {

  "Can only be a class, abstract class, case class, trait or object" >> {

    @context
    class ClassContext(Foo: Data) {
      role Foo {}
    }

    @context
    abstract class AbstractClassContext(Foo: Data) {
      role Foo {}
    }

    @context
    case class CaseClassContext(Foo: Data) {
      role Foo {}
    }

    @context
    trait TraitContext {
      val Foo = Data(42)
      role Foo {}
    }

    @context
    object ObjectContext {
      // Objects to play a Role are instantiated inside the Context object
      val Foo = Data(42)
      role Foo {}
    }

    expectCompileError(
      """
        @context
        val outOfContext = "no go"
      """,
      """
        |Only classes/case classes/objects can be transformed to DCI Contexts. Found:
        |val outOfContext = "no go"
      """)

    success
  }


  "Can be an empty stub" >> {

    @context
    class EmptyClassContext

    success
  }


  "Cannot be named `role`" >> {

    expectCompileError(
      """
        @context
        class role
      """,
      "Context class can't be named `role`")

    success
  }
}