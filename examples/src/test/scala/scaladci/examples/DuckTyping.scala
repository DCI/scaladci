package scaladci
package examples
import org.specs2.mutable._
import scala.language.reflectiveCalls

/*
  Role interface with duck typing

  If a Role asks any object to just have a specific set of
  method signatures available, which could be seen as the
  "Role interface" or Role contract", then we are actually
  reducing safety. We would have no clue _which_ object
  has those methods. They could do terrible things.

  On the contrary we know much more from a type although
  polymorphism can sabotage as much.
*/

class DuckTyping extends Specification {

  // Data
  class Account(name: String, var balance: Int) {
    def increaseBalance(amount: Int) { balance += amount }
    def decreaseBalance(amount: Int) { balance -= amount }
  }

  // Evil account that satisfy a "Role contract"
  case class HackedAccount(name: String, var balance: Int) {
    def decreaseBalance(amount: Int) { balance -= amount * 10 }
  }

  // Class having a subset of the original methods. As an alternative
  // to duck typing we can pass this instead to our context.
  case class MyAccount(name: String, initialAmount: Int) extends Account(name, initialAmount) {
    override def increaseBalance(amount: Int) { balance += amount }

    // We could disallow certain methods in a sub class like this
    override def decreaseBalance(amount: Int) = ??? // Not implemented
  }

  @context
  class MoneyTransfer(
    source: { def decreaseBalance(amount: Int) }, // <- structural (duck) type
    destination: MyAccount,
    amount: Int) {

    source.withdraw

    role source {
      def withdraw() {
        source.decreaseBalance(amount)
        destination.deposit
      }
    }

    role destination {
      def deposit() {
        destination.increaseBalance(amount)
      }
    }
  }

  "Ducktyping is less safe" >> {
    val salary = HackedAccount("Salary", 3000)
    val budget = MyAccount("Budget", 1000)

    new MoneyTransfer(salary, budget, 700)

    salary.balance === 3000 - 700 * 10 // Auch! I've been hacked
    budget.balance === 1000 + 700
  }
}
