import scaladci._

/*
  Simple example of DCI in action - the money transfer example

  1. Import scaladci._
  2. Annotate a class with @context - that DCI-enables it.
  3. Define roles names (like Actors in a use case)
  4. Define role methods (what will those actors do...)
  5. Define the trigger that starts off the chain of interactions between the roles

  Normally we lowercase parameter names. "Source" and "Destination" of the MoneyTransfer
  DCI context are an exception since we want to associate them with our role definitions.
  A role definition has to match an object identifier in the context scope. Not necessarily
  a class parameter, any val/var will do as long as the identifier name matches a defined role.

  Compared to this school-book example, Data classes, DCI Contexts and the runtime
  environment would of course normally be in separate tiers.

  More info         - http://github.com/dci/scaladci
  Official website  - http://fulloo.info
  Discussions       - https://groups.google.com/forum/#!forum/object-composition
*/

object MoneyTransferApp extends App {

  // Data - knows nothing of transfers...
  case class Account(name: String, var balance: Int) {
    def increaseBalance(amount: Int) { balance += amount }
    def decreaseBalance(amount: Int) { balance -= amount }
  }

  // DCI Context - encapsulates a specific "process"/"use case"/"network of interactions" etc
  @context
  class MoneyTransfer(Source: Account, Destination: Account, amount: Int) {

    Source.withdraw // Trigger method setting off the use case

    role Source {                       // Role definition
      def withdraw() {                  // Role method
        Source.decreaseBalance(amount)  // Instance method
        Destination.deposit             // Role interacts with other role
      }
    }

    role Destination {
      def deposit() {
        Destination.increaseBalance(amount)
      }
    }
  }


  // Runtime environment

  // Instantiate Data objects
  val salary = Account("Salary", 3000)
  val budget = Account("Budget", 1000)

  // Instantiate Context with data objects and run use case
  new MoneyTransfer(salary, budget, 700)

  // Confirm that amount has transferred
  assert(salary.balance == 3000 - 700, s"Salary balance should have been 3000 - 700 = 2300. Is now ${salary.balance}")
  assert(budget.balance == 1000 + 700, s"Budget balance should have been 1000 + 700 = 1700. Is now ${budget.balance}")
}
