package scaladci
package examples.MoneyTransfer
import scala.language.reflectiveCalls

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

class MoneyTransfer(src: Account, dest: Account, amount: Int) extends Context {
  private val source      = src.as[Source]
  private val destination = dest.as[Destination]

  def transfer() {
    source.transfer
  }

  private trait Source {self: Account =>
    def withdraw() {
      decreaseBalance(amount)
    }
    def transfer() {
      println("Source balance is: " + balance)
      println("Destination balance is: " + destination.balance)
      destination.deposit()
      withdraw()
      println("Source balance is now: " + balance)
      println("Destination balance is now: " + destination.balance)
    }
  }

  private trait Destination {self: Account =>
    def deposit() {
      increaseBalance(amount)
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