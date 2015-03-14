package scaladci
package syntax
import util._

class DCIcontext extends DCIspecification {

  "Can only be a class, abstract class, case class, trait or object" >> {

    @context
    class ClassContext(myRole: Data) {
      role myRole {}
    }

    @context
    abstract class AbstractClassContext(myRole: Data) {
      role myRole {}
    }

    @context
    case class CaseClassContext(myRole: Data) {
      role myRole {}
    }

    @context
    trait TraitContext {
      val myRole = Data(42)
      role myRole {}
    }

    @context
    object ObjectContext {
      // Objects to play a Role are instantiated inside the Context object
      val myRole = Data(42)
      role myRole {}
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