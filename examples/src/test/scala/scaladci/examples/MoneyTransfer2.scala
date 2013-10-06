package scaladci
package examples.moneytransfer2
import scala.language.reflectiveCalls
import dci._

/*
More elaborate version of the canonical Money Transfer example
Inspired by Rune Funch's implementation at
http://fulloo.info/Examples/Marvin/MoneyTransfer/
*/
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

  role(Source) {
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

  role(Destination) {
    def deposit() {
      self.increaseBalance(amount)
    }
  }
}

object MoneyTransferMarvinTest extends App {
  val source      = Account("salary", List(LedgerEntry("start", 0), LedgerEntry("first deposit", 1000)))
  val destination = Account("budget", List())
  val context     = new MoneyTransfer(source, destination, 245)
  context.transfer()
}

/* prints:

Source balance is: 1000
Destination balance is: 0
Source balance is now: 755
Destination balance is now: 245

*/