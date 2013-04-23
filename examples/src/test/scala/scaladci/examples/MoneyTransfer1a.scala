package scaladci
package examples.MoneyTransfer1
import DCI._

case class Account(name: String, var balance: Int) {
  def increaseBalance(amount: Int) { balance += amount }
  def decreaseBalance(amount: Int) { balance -= amount }
}

class MoneyTransfer(Source: Account, Destination: Account, amount: Int) extends Context {

  Source.withdraw

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

object Test3 extends App {
  val salary = Account("Salary", 3000)
  val budget = Account("Budget", 1000)
  new MoneyTransfer(salary, budget, 700)
  println("salary.balance: " + salary.balance)
  println("budget.balance: " + budget.balance)
}