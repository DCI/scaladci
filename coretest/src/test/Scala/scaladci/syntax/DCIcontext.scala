package scaladci
package syntax
import util._

class DCIcontext extends DCIspecification {

  "Can only be a class, abstract class, case class, trait or object" >> {

    @context
    class ClassContext(MyRole: Data) {
      role MyRole {}
    }

    @context
    abstract class AbstractClassContext(MyRole: Data) {
      role MyRole {}
    }

    @context
    case class CaseClassContext(MyRole: Data) {
      role MyRole {}
    }

    @context
    trait TraitContext {
      val MyRole = Data(42)
      role MyRole {}
    }

    @context
    object ObjectContext {
      // Objects to play a Role are instantiated inside the Context object
      val MyRole = Data(42)
      role MyRole {}
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