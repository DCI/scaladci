import scala.language.dynamics

package object scaladci {

  // role foo {...}
  val role = roleO
  private[scaladci] object roleO extends Dynamic {
    def applyDynamic(obj: Any)(roleBody: => Unit) = roleBody
  }
}