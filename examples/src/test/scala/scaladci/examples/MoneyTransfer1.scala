package scaladci
package examples.moneytransfer1
import DCI._

// Simplest possible version of canonical Money Transfer example

case class Account(name: String, var balance: Int) {
  def increaseBalance(amount: Int) { balance += amount }
  def decreaseBalance(amount: Int) { balance -= amount }
}

// Using Role name as reference
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

/*
Using "self" as reference
"this" would point to the Context and is not allowed in the role method body.
Adding example of a role method overriding an instance method (works the same
when using a Role reference instead of "self")
*/
class MoneyTransfer2(Source: Account, Destination: Account, amount: Int) extends Context {

  Source.withdraw

  role(Source) {
    def withdraw {
      // role method takes precedence over instance method if they have the same signature!
      self.decreaseBalance(amount)
      Destination.deposit
    }
    // Overriding an instance method - this role method takes precedence
    def decreaseBalance(amount: Int) { self.balance -= amount * 3 }
  }

  role(Destination) {
    def deposit {
      self.increaseBalance(amount)
    }
  }
}

object Test3 extends App {
  val salary = Account("Salary", 3000)
  val budget = Account("Budget", 1000)

  new MoneyTransfer(salary, budget, 700)
  println("salary.balance: " + salary.balance)
  println("budget.balance: " + budget.balance)

  new MoneyTransfer2(budget, salary, 10)
  println("\nsalary.balance: " + salary.balance)
  println("budget.balance: " + budget.balance)

  /*
  Expected output:

  salary.balance: 2300
  budget.balance: 1700

  salary.balance: 2310
  budget.balance: 1670 // 1690 if un-commenting the decreaseBalance role method (line 48)!
  */
}