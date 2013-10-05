package scaladci
package examples.moneytransfer1
import DCI._

/*
Simplest versions of the canonical Money Transfer example

NOTE: If a role method has the same signature as an instance method of the original
Data object, the role method will take precedence/override the instance method. This
applies to all three versions below.
*/
case class Account(name: String, var balance: Int) {
  def increaseBalance(amount: Int) { balance += amount }
  def decreaseBalance(amount: Int) { balance -= amount }
}

// Using Role name as reference to the Role Player
class MoneyTransfer(Source: Account, Destination: Account, amount: Int) extends Context {

  Source.withdraw
  println("hej")

  role(Source) {
    def withdraw {
      Source.decreaseBalance(amount)
      Destination.deposit
    }
  }

  role(Destination) {
    def deposit {
      Destination.increaseBalance(amount)
    }
  }
}