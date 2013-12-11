package scaladci.javascript

import org.jscala._
import scaladci.context
import scaladci.dci._

/*
  Scala DCI code transformation to Javascript code

  Our @context annotation first transforms our Scala DCI code with role definitions
  to plain vanilla Scala code. In the same compilation we then apply a further
  transformation from this vanilla Scala code to Javascript code with the help of
  JScala (http://jscala.org).

  Caveats:
  - Case classes can't be used :-(
  - Data and Context classes used has to be annotated with @Javascript
  - @Javascript annotated classes have to be joined to the main code (see below)
  - [minor bug]: "myMethod()" works, "myMethod" doesn't
*/

object MoneyTransfer extends App {

  @Javascript
  class Account(name: String, var balance: Int) {
    def increaseBalance(amount: Int) { balance += amount }
    def decreaseBalance(amount: Int) { balance -= amount }
  }

  @context    // Scala DCI -> Scala vanilla
  @Javascript // Scala vanilla -> Javascript
  class MoneyTransfer(val Source: Account, Destination: Account, amount: Int) {

    Source.withdraw

    role(Source) {
      def withdraw() {
        Source.decreaseBalance(amount)
        Destination.deposit()
      }
    }

    role(Destination) {
      def deposit() {
        Destination.increaseBalance(amount)
      }
    }
  }

  val test = javascript {
    val salary = new Account("Salary", 3000)
    val budget = new Account("Budget", 1000)

    new MoneyTransfer(salary, budget, 700)
    print("salary.balance: " + salary.balance + "\n")
    print("budget.balance: " + budget.balance)
  }

  val js = Account.jscala.javascript ++ MoneyTransfer.jscala.javascript ++ test // join classes definitions with main code
  println("-------  js --------")
  println(js.asString) // prints resulting JavaScript
  println("\n------- test --------")
  js.eval() // run using Rhino (https://developer.mozilla.org/en-US/docs/Rhino)
}

/*
Expected:

-------  js --------
{
  function Account(name, balance) {
    this.name = name;
    this.balance = balance;
    this.increaseBalance = function (amount) {
      this.balance = this.balance + amount;
    };
    this.decreaseBalance = function (amount) {
      this.balance = this.balance - amount;
    };
  };
  function MoneyTransfer(Source, Destination, amount) {
    this.Source = Source;
    this.Destination = Destination;
    this.amount = amount;
    this.Source_withdraw = function () {
      this.Source.decreaseBalance(this.amount);
      this.Destination_deposit();
    };
    this.Destination_deposit = function () {
      this.Destination.increaseBalance(this.amount);
    };
    this.Source_withdraw()
  };
  var salary = new this.Account("Salary", 3000);
  var budget = new this.Account("Budget", 1000);
  new this.MoneyTransfer(salary, budget, 700);
  print(("salary.balance: " + salary.balance) + "\n");
  print("budget.balance: " + budget.balance);
}

------- test --------
salary.balance: 2300
budget.balance: 1700
*/