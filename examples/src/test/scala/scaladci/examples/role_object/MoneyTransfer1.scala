package scaladci
package examples.role_object.moneytransfer1

// DISCLAIMER: Non-DCI compliant role-object approach
// Simplest possible version of canonical Money Transfer example

case class Account(name: String, var balance: Int) {
  def increaseBalance(amount: Int) { balance += amount }
  def decreaseBalance(amount: Int) { balance -= amount }
}

// See: https://groups.google.com/forum/?fromgroups=#!topic/object-composition/JJiLWBsZWu0

class MoneyTransfer(source: Account, destination: Account, amount: Int) {

  Source.withdraw

  private object Source {
    def withdraw {
      source.decreaseBalance(amount)
      Destination.deposit
    }
  }

  private object Destination {
    def deposit {
      destination.increaseBalance(amount)
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