package scaladci
package util
import org.specs2.mutable._

trait DCIspecification extends Specification {

  // Data class
  case class Data(i: Int) {
    // Instance method
    def number = i
  }
}
