package scaladci
package examples.MoneyTransfer2Test

case class Account(var acc: String, var balance: Int) {
  def increaseBalance(amount: Int) { balance = balance + amount }
  def decreaseBalance(amount: Int) { balance = balance - amount }
}

class MoneyTransfer(acc1: Account, acc2: Account) extends Context {
  private val source      = acc1.as[Source]
  private val destination = acc2.as[Destination]

  def transfer(amount: Int) {
    source.withdraw(amount)
  }

  private trait Source {self: Account =>
    def withdraw(amount: Int) {
      print(s"Source      ($acc): $balance - $amount = ")
      decreaseBalance(amount)
      println(balance)
      destination.deposit(amount)
    }
  }

  private trait Destination {self: Account =>
    def deposit(amount: Int) {
      print(s"Destination ($acc): $balance + $amount = ")
      increaseBalance(amount)
      println(balance)
    }
  }
}

object MoneyTransfer2Test extends App {
  val salary = new Account("Salary", 3000)
  val budget = new Account("Budget", 1000)
  new MoneyTransfer(salary, budget) transfer 800
}

/* prints:

Source      (Salary): 3000 - 800 = 2200
Destination (Budget): 1000 + 800 = 1800

*/