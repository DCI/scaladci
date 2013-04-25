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

// Using "self" as reference to the Role Player
class MoneyTransfer_self(Source: Account, Destination: Account, amount: Int) extends Context {

  Source.withdraw

  role(Source) {
    def withdraw {
      // role method takes precedence over instance method
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

/*
Using "this" as reference to the Role Player

ATTENTION: Our use of "this" here is not Scala-idiomatic since "this" would normally
point to the MoneyTransfer_this Context instance. Our Context transformer macro instead turns
"this" into a DCI-idiomatic reference to the Role Player. Inside each role definition body we
can maintain the impression of working with "this role".
*/
class MoneyTransfer_this(Source: Account, Destination: Account, amount: Int) extends Context {

  Source.withdraw

  role(Source) {
    def withdraw {
      this.decreaseBalance(amount)
      Destination.deposit
    }
  }

  role(Destination) {
    def deposit {
      // role method takes precedence over instance method
      this.increaseBalance(amount)
    }
    // Overriding an instance method - this role method takes precedence
    def increaseBalance(amount: Int) { self.balance += amount * 2 }
  }
}

object Test3 extends App {
  val salary = Account("Salary", 3000)
  val budget = Account("Budget", 1000)

  new MoneyTransfer(salary, budget, 700)
  println("salary.balance: " + salary.balance)
  println("budget.balance: " + budget.balance)

  new MoneyTransfer_self(salary, budget, 100)
  println("\nsalary.balance: " + salary.balance + " (decreased with 300)")
  println("budget.balance: " + budget.balance + " (increased with 100)")

  new MoneyTransfer_this(salary, budget, 10)
  println("\nsalary.balance: " + salary.balance + " (decreased with 10)")
  println("budget.balance: " + budget.balance + " (increased with 20)")

  /*
  Expected output:

  salary.balance: 2300
  budget.balance: 1700

  salary.balance: 2000 (decreased with 300)
  budget.balance: 1800 (increased with 100)

  salary.balance: 1990 (decreased with 10)
  budget.balance: 1820 (increased with 20)
  */
}