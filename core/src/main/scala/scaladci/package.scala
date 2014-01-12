import scala.language.dynamics

package object scaladci {

  // `role` as method
  def role(instance: Any)(roleMethods: => Unit) {}

  // `role` as field
  val role = roleO
  private[scaladci] object roleO extends Dynamic {
    def applyDynamic(obj: Any)(roleBody: => Unit) = roleBody
  }

  // ... or use following lines if you prefer the `role` 'keyword' as an object:

  // `role` as object
  //  private[scaladci] object role extends Dynamic {
  //    def applyDynamic(obj: Any)(roleBody: Unit) = roleBody
  //  }
}