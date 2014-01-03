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
  class MoneyTransfer(Source: Account, Destination: Account, amount: Int) {

    def transfer() {
      Source.transfer
    }

    role Source {
      def withdraw() {
        self.decreaseBalance(amount)
      }
      def transfer() {
        println("Source balance is: " + self.balance)
        println("Destination balance is: " + Destination.balance)
        Destination.deposit()
        withdraw()
        println("Source balance is now: " + self.balance)
        println("Destination balance is now: " + Destination.balance)
      }
    }

    role Destination {
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

    val context = new MoneyTransfer(source, destination, 245)
    context.transfer()

    source.balance === 1000 - 245
    destination.balance === 0 + 245
  }
}