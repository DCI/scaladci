package scaladci
package examples.MoneyTransfer1Test

case class Account(var acc: String, var balance: Int) {
  def increaseBalance(amount: Int) { balance = balance + amount }
  def decreaseBalance(amount: Int) { balance = balance - amount }
}

class MoneyTransfer(acc1: Account, acc2: Account, amount: Int) extends Context {
  private val source = acc1.as[Source]
  private val destination = acc2.as[Destination]

  source.withdraw()

  private trait Source {self: Account =>
    def withdraw() {
      decreaseBalance(amount)
      destination.deposit()
    }
  }

  private trait Destination {self: Account =>
    def deposit() {
      increaseBalance(amount)
    }
  }
}

object MoneyTransfer1Test extends App {
  val salary = new Account("Salary", 3000)
  val budget = new Account("Budget", 1000)
  new MoneyTransfer(salary, budget, 700)
}

// (without output)