package scaladci
package examples
import scala.language.reflectiveCalls
import org.specs2.mutable._

/*
  More elaborate version of the canonical Money Transfer example
  Inspired by Rune Funch's implementation at
  http://fulloo.info/Examples/Marvin/MoneyTransfer/
*/

class MoneyTransfer2 extends Specification {

  case class LedgerEntry(message: String, amount: Int)

  case class Account(account: String, initialLedgers: List[LedgerEntry]) {
    private val ledgers = new {
      var ledgerList = initialLedgers
      def addEntry(message: String, amount: Int) { ledgerList = ledgerList :+ new LedgerEntry(message, amount) }
      def getBalance = ledgerList.foldLeft(0)(_ + _.amount)
    }

    def balance = ledgers.getBalance
    def increaseBalance(amount: Int) { ledgers.addEntry("depositing", amount) }
    def decreaseBalance(amount: Int) { ledgers.addEntry("withdrawing", -amount) }
  }

  @context
  case class MoneyTransfer(source: Account, destination: Account, amount: Int) {

    def transfer() {
      source.transfer
    }

    role source {
      def withdraw() {
        self.decreaseBalance(amount)
      }
      def transfer() {
        println("source balance is: " + self.balance)
        println("destination balance is: " + destination.balance)
        destination.deposit()
        withdraw()
        println("source balance is now: " + self.balance)
        println("destination balance is now: " + destination.balance)
      }
    }

    role destination {
      def deposit() {
        self.increaseBalance(amount)
      }
    }
  }


  // Test

  "Money transfer with ledgers Marvin/Rune" >> {
    val source = Account("salary", List(LedgerEntry("start", 0), LedgerEntry("first deposit", 1000)))
    val destination = Account("budget", List())

    source.balance === 1000
    destination.balance === 0

    val context = MoneyTransfer(source, destination, 245)
    context.transfer()

    source.balance === 1000 - 245
    destination.balance === 0 + 245
  }
}