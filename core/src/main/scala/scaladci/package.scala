import scala.language.dynamics

package object scaladci {

  // `role` as "keyword"
  // role Foo {...}
  val role = roleO
  private[scaladci] object roleO extends Dynamic {
    def applyDynamic(obj: Any)(roleBody: => Unit) = roleBody
  }

  // `role` as method
  // role(Foo) {...}
  def role(instance: Unit): Unit = {}
  def role(instance: Any)(roleMethods: => Unit): Unit = {}
}