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


  // Using role name as reference to the Role Player - `Source.decreaseBalance(amount)`

  @context
  class MoneyTransfer(Source: Account, Destination: Account, amount: Int) {

    Source.withdraw

    role Source {
      def withdraw {
        Source.decreaseBalance(amount)
        Destination.deposit
      }
    }

    role Destination {
      def deposit {
        Destination.increaseBalance(amount)
      }
    }
  }


  // Using `self` as reference to the Role Player - `self.decreaseBalance(amount)`

  @context
  class MoneyTransfer_self(Source: Account, Destination: Account, amount: Int) {

    Source.withdraw

    role Source {
      def withdraw {
        // role method takes precedence over instance method!
        self.decreaseBalance(amount)
        Destination.deposit
      }
      // Overriding an instance method - this role method takes precedence
      def decreaseBalance(amount: Int) {
        val specialFee = 200
        self.balance -= amount + specialFee
      }
    }

    role Destination {
      def deposit {
        self.increaseBalance(amount)
      }
    }
  }


  /*
    Using `this` as reference to the Role Player - `this.decreaseBalance(amount)`

    ATTENTION: Our use of `this` here is NOT Scala-idiomatic since `this` would normally
    point to the Context instance!!

    Our Context transformer macro instead turns `this` into a DCI-idiomatic reference
    to the Role Player. Inside each role definition body we can maintain the impression
    of working with "this role".
  */
  @context
  class MoneyTransfer_this(Source: Account, Destination: Account, amount: Int) {

    Source.withdraw

    role Source {
      def withdraw {
        this.decreaseBalance(amount)
        Destination.deposit
      }
    }

    role Destination {
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

  /*
    Alternative role definition syntax where the data instance is supplied as an
    argument to a `role` method.

    This style has the practical advantage that when you mark the data instance,
    most IDEs will show all its occurrences in your context which makes it
    convenient to track a Role.

    On the other hand the role definition looks like a method call (which of
    course is what it is before AST transformation). This goes against the
    semantics of DCI where `role` should be a keyword that signals a Role
    definition.

    In order to stay in line with DCI semantics, all examples uses the more
    semantically correct (and more beautiful)
      `role` RoleName {...}
    syntax throughout all examples. But as you see below, you're free to mix
    which style you want to use anytime.

    The resulting code after source code transformation will be the
    _exact same_ with both styles.
  */
  @context
  class MoneyTransfer_roleDefMethod(Source: Account, Destination: Account, amount: Int) {

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


  // Hey, let's mix all the styles! :-)

  @context
  class MoneyTransfer_mixed(Source: Account, Destination: Account, amount: Int) {

    Source.withdraw

    role(Source) {
      def withdraw {
        self.decreaseBalance(amount)
        Destination.deposit
      }
    }

    role Destination {
      def deposit {
        this.increaseBalance(amount)
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
    new MoneyTransfer(salary, budget, 700)
    salary.balance === 3000 - 700
    budget.balance === 1000 + 700

    // Using `self`
    new MoneyTransfer_self(salary, budget, 100)
    salary.balance === 2300 - 100 - 200 // Special fee in overriding role method
    budget.balance === 1700 + 100

    // Using `this`
    new MoneyTransfer_this(salary, budget, 10)
    salary.balance === 2000 - 10
    budget.balance === 1800 + 10 + 10 // Special bonus in overriding role method

    // Alternative role definition syntax with `role` method
    new MoneyTransfer_roleDefMethod(salary, budget, 50)
    salary.balance === 1990 - 50
    budget.balance === 1820 + 50

    // Alternative role definition syntax with `role` method
    new MoneyTransfer_roleDefMethod(salary, budget, 1)
    salary.balance === 1940 - 1
    budget.balance === 1870 + 1
  }
}
