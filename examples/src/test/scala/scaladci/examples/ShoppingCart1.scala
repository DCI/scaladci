package scaladci
package examples.shoppingcart1
import dci._
import scala.collection.mutable

/*
Shopping cart example, version 1

Implementing a simple Place Order use case of an online shopping cart to explore
how we could handle various execution paths (scenarios) within a single DCI Context.

See discussion at:
https://groups.google.com/forum/?fromgroups=#!topic/object-composition/JJiLWBsZWu0

===========================================================================
USE CASE:	Place Order [user-goal]

Person browsing around finds product(s) in a web shop that he/she wants to buy.

Primary actor.. Web customer ("Customer")
Scope.......... Web shop ("Shop")
Preconditions.. Shop presents product(s) to Customer
Trigger........ Customer wants to buy certain product(s)

Main Success Scenario
---------------------------------------------------------------------------
1. Customer marks desired Product in Shop.
2. Shop adds Item to Cart (can repeat from step 1).
3. Customer requests to review Order.
4. Shop presents Cart with Items and prices to Customer.
5. Customer pays Order.
6. Shop confirms purchase to Customer.

Deviations
---------------------------------------------------------------------------
2a. Product is out of stock:
    1. Shop informs Customer that Product is out of stock.
    2. Go to step 1 (pick another product).

4a. Customer has gold membership:
    1. Shop presents Cart with Products and discounted prices to Customer.
    2. Go to step 5.

5a. Customer credit is too low:
    1. Shop informs Customer of insufficient funds on credit card.
        a. Customer removes unaffordable item(s) from Cart:
            1. Go to step 5.
        b. Customer terminates order:
            2. Failure.
===========================================================================

Todo: how to prevent "wrong" invocation order?
*/

// 4 basic "dumb" Data types
case class Product(name: String, price: Int)
case class Person(name: String, var cash: Int) {
  val owns = mutable.Map[Int, Product]()
}
case class Company(name: String, var cash: Int) {
  val stock = mutable.Map[Int, Product]()
  val goldMembers = mutable.Set[Person]()
}
case class Order(customer: Person) {
  val items = mutable.Map[Int, Product]()
}

@context
class PlaceOrder(Shop: Company, Customer: Person) {

  // Trigger methods
  def addItem(productId: Int): Option[Product] = Cart.addItem(productId)
  def removeItem(productId: Int): Option[Product] = Cart.removeItem(productId)
  def getCurrentItems = Cart.items.toIndexedSeq.sortBy(_._1)
  def pay = Customer.payOrder

  // Roles
  private val Warehouse = Shop
  private val Cart = Order(Customer)

  role(Customer) {
    def payOrder: Boolean = {
      // Sufficient funds?
      val orderTotal = Cart.total
      if (orderTotal > Customer.cash)
        return false

      // Transfer ownership of items
      Customer.owns ++= Cart.items
      Customer.cash -= orderTotal
      Cart.items.foreach(i => Warehouse.stock.remove(i._1))
      Shop.receivePayment(orderTotal)
      true
    }
    def isGoldMember = Shop.goldMembers.contains(Customer)
    def reduction = if (Customer.isGoldMember) 0.5 else 1
  }

  role(Shop) {
    def receivePayment(amount: Int) { Shop.cash += amount }
  }

  role(Warehouse) {
    def has(productId: Int) = Shop.stock.isDefinedAt(productId)
  }

  role(Cart) {
    def addItem(productId: Int): Option[Product] = {
      // Check warehouse
      if (!Warehouse.has(productId))
        return None

      // Gold member price?
      val product = Warehouse.stock(productId)
      val customerPrice = (product.price * Customer.reduction).toInt

      // Add item with adjusted price to Cart
      val revisedProduct = product.copy(price = customerPrice)
      Cart.items.put(productId, revisedProduct)
      Some(revisedProduct)
    }

    def removeItem(productId: Int): Option[Product] = {
      if (!Cart.items.isDefinedAt(productId))
        return None
      Cart.items.remove(productId)
    }

    def total = Cart.items.map(_._2.price).sum
  }
}

// Environment (Controller or the like...)
object TestPlaceOrder extends App {
  var shop: Company = _
  var customer: Person = _
  def reset() {
    shop = Company("Don's Auto shop", 100000)
    shop.stock ++= List(
      1 -> Product("Wax", 40),
      2 -> Product("Tires", 600),
      3 -> Product("BMW", 50000))
    customer = Person("Matthew", 20000)
  }
  def showResult(msg: String = "") {
    println(s"$msg" +
      "\n--------------------------------------------------" +
      s"\n- Customer cash: ${customer.cash}" +
      s"\n- Customer owns: ${customer.owns.toIndexedSeq.sortBy(_._1).mkString("\n")}" +
      s"\n- Shop cash : ${shop.cash}\n"
    )
  }
  reset()
  showResult("SHOPPING CART 1")

  // Various scenarios
  {
    println("\n######## Main success scenario ####################")
    println(s"Step 1: Customer selects product(s) in UI")
    val desiredProduct = 1
    val order = new PlaceOrder(shop, customer)
    order.addItem(desiredProduct)
    val otherDesiredProduct = 2
    order.addItem(otherDesiredProduct)
    println(s"Step 2: 2 items added to cart (step 1-2 repeated)")
    println(s"Step 3: Customer requests to review order")
    println(s"Step 4: Shop presents items in cart: \n${order.getCurrentItems.mkString("\n")}")
    println(s"Step 5: Customer requests to pay order")
    val paymentStatus = order.pay
    println(s"Step 6: Order completed? $paymentStatus\n")
    showResult("Customer bought wax for 40:")
  }
  {
    reset()
    println("\n######## Scenario A ###############################")
    val desiredProduct = 1
    shop.stock.remove(desiredProduct)
    val order = new PlaceOrder(shop, customer)
    val itemAdded = order.addItem(desiredProduct)
    if (itemAdded == None)
      println(s"@@ Deviation 2a: Product $desiredProduct out of stock!\n")
    val otherDesiredProduct = 2
    order.addItem(otherDesiredProduct)
    order.pay
    showResult("Customer bought tires instead for 600:")
  }
  {
    reset()
    println("\n######## Scenario B ###############################")
    val wax = 1
    shop.goldMembers.add(customer)
    val order = new PlaceOrder(shop, customer)
    order.addItem(wax)
    println(s"@@ Deviation 4a: Customer is gold member\n")
    order.pay
    showResult("Customer has paid half price of 20:")
  }
  {
    reset()
    println("\n######## Scenario C ###############################")
    val desiredProduct = 3
    val order = new PlaceOrder(shop, customer)
    val itemAdded = order.addItem(desiredProduct)
    val paymentCompleted = order.pay
    if (!paymentCompleted)
      println(s"@@ Deviation 5a: Insufficient funds for ${itemAdded.get.name}\n")
    showResult("Order placement aborted:")
  }

  // All deviations and trigger methods in play...
  {
    reset()
    println("\n######## Scenario ABC #############################")
    // Tires out of stock
    val tires = 2
    shop.stock.remove(tires)

    // We have a gold member
    shop.goldMembers.add(customer)
    val order = new PlaceOrder(shop, customer)

    // Trying to buy tires
    val itemAdded = order.addItem(tires)
    if (itemAdded == None)
      println(s"@@ Deviation 2a: Product $tires out of stock!")

    // Let's buy the BMW instead then - as a gold member that should be possible!
    val BMW = 3
    val newItemAdded = order.addItem(BMW)
    println(s"@@ Deviation 4a: Customer is gold member")
    val paymentCompleted = order.pay

    // Ouch - still too expensive
    if (!paymentCompleted)
      println(s"@@ Deviation 5a: ${customer.cash} not enough to buy  " +
        s"${newItemAdded.get.name} (${newItemAdded.get.price} needed)")

    // Ok, no new car today
    order.removeItem(BMW)
    println(s"@@ Deviation 5a.1.a: Remove unaffordable item from cart" +
      s"\n${order.getCurrentItems.mkString("\n")}")

    // Let's get some wax anyway...
    order.addItem(1)

    // Now we can afford it
    order.pay

    showResult("Gold customer can't get out-of-stock tires, neither " +
      "too expensive BMW (even with gold discount). Ok, some wax then:")
  }
}
