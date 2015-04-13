package scaladci
package examples
import org.specs2.mutable._

/*
  Simplest versions of the canonical Money Transfer example

  NOTE: If a role method has the same signature as an instance method of the original
  Data object, the role method will take precedence/override the instance method. This
  applies to all versions below.
*/

class MoneyTransfer1 extends Specification {

  // Data
  case class Account(name: String, var balance: Int) {
    def increaseBalance(amount: Int) { balance += amount }
    def decreaseBalance(amount: Int) { balance -= amount }
  }


  // Using role name as reference to the Role Player - `source.decreaseBalance(amount)`

  @context
  case class MoneyTransfer(source: Account, destination: Account, amount: Int) {

    source.withdraw

    role source {
      def withdraw {
        source.decreaseBalance(amount)
        destination.deposit
      }
    }

    role destination {
      def deposit {
        destination.increaseBalance(amount)
      }
    }
  }


  // Using `self` as reference to the Role Player - `self.decreaseBalance(amount)`

  @context
  case class MoneyTransfer_self(source: Account, destination: Account, amount: Int) {

    source.withdraw

    role source {
      def withdraw {
        // role method takes precedence over instance method!
        self.decreaseBalance(amount)
        destination.deposit
      }
      // Overriding an instance method - this role method takes precedence
      def decreaseBalance(amount: Int) {
        val specialFee = 200
        self.balance -= amount + specialFee
      }
    }

    role destination {
      def deposit {
        self.increaseBalance(amount)
      }
    }
  }


  /*
    Using `this` as reference to the Role Player - `this.decreaseBalance(amount)`
    ATTENTION:
    Using `this` inside a role definition will reference the role-playing object (and not the Context object)!
  */
  @context
  case class MoneyTransfer_this(source: Account, destination: Account, amount: Int) {

    source.withdraw

    role source {
      def withdraw {
        this.decreaseBalance(amount)
        destination.deposit
      }
    }

    role destination {
      def deposit {
        // role method takes precedence over instance method
        this.increaseBalance(amount)
      }
      // Overriding an instance method - this role method takes precedence
      def increaseBalance(amount: Int) {
        val bonus = 10
        this.balance += amount + bonus
      }
    }
  }


  // Test the various syntaxes
  // Note that the Account instance objects keep their identity throughout
  // all the roles they play in the various Contexts (no wrapping here).

  "With various role syntaxes" >> {
    val salary = Account("Salary", 3000)
    val budget = Account("Budget", 1000)

    // Using role name
    MoneyTransfer(salary, budget, 700)
    salary.balance === 3000 - 700
    budget.balance === 1000 + 700

    // Using `self`
    MoneyTransfer_self(salary, budget, 100)
    salary.balance === 2300 - 100 - 200 // Special fee in overriding role method
    budget.balance === 1700 + 100
  }
}
