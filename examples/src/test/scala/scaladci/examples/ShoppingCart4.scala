package scaladci
package examples.shoppingcart4

import DCI._
import scala.collection.mutable

/*
UC now with primary user actions and secondary system responses - still naming
system responsibilities with Role names...

Added "DesiredProduct" role.

Any explicit/implicit logic now shouldn't leak out of the Context..?

See discussion at:
https://groups.google.com/forum/?fromgroups=#!topic/object-composition/JJiLWBsZWu0

===========================================================================
USE CASE 2:	Place Order [user-goal]

Person browsing around finds product(s) in a web shop that he/she wants to buy.

Primary actor.. Web customer ("Customer")
Scope.......... Web shop ("Shop")
Preconditions.. Shop presents product(s) to Customer
Trigger........ Customer wants to buy certain product(s)

Main Success Scenario
---------------------------------------------------------------------------
1. Customer marks Desired Product in Shop.
    - Shop reserves product in Warehouse
    - Shop adds Item to Cart (can repeat from step 1).
    - Shop shows updated contents of Cart to Customer
2. Customer requests to review Order.
    - Shop presents Cart with Items and prices to Customer.
3. Customer pays Order.
    - Shop confirms purchase to Customer.

Deviations
---------------------------------------------------------------------------
1a. Product is out of stock:
    1. Shop informs Customer that Product is out of stock.

1b. Customer has gold membership:
    1. Shop adds discounted product to Cart.

3a. Customer has insufficient funds to pay Order:
    1. Shop informs Customer of insufficient funds on credit card.
        a. Customer removes unaffordable item(s) from Cart:
            1. Go to step 3.
        b. Customer terminates order:
            2. Failure.
===========================================================================

Todo: how to prevent "wrong" invocation order?
*/

// Data
case class Product(val name: String, price: Int)
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

// Context
class PlaceOrder(Shop: Company, Customer: Person) extends Context {

  // Trigger methods
  def customerMarksDesiredProductInShop(productId: Int): Option[Product] =
    Customer.markDesiredProductInShop(productId)
  def customerRequestsToReviewOrder: Seq[(Int, Product)] =
    Customer.reviewOrder
  def customerPaysOrder: Boolean =
    Customer.payOrder
  def customerRemovesProductFromCart(productId: Int): Option[Product] =
    Customer.removeProductFromCart(productId)

  // Roles
  role(Customer) {
    def markDesiredProductInShop(productId: Int): Option[Product] = {
      if (!Shop.stock.isDefinedAt(productId))
        return None
      val product = Shop.stock(productId)
      val discountedPrice = Customer.getMemberPriceOf(product)
      val desiredProduct = product.copy(price = discountedPrice)
      Cart.addItem(productId, desiredProduct)
      Some(desiredProduct)
    }
    def reviewOrder = Cart.getItems
    def removeProductFromCart(productId: Int) = Cart.removeItem(productId: Int)
    def payOrder: Boolean = {
      val orderTotal = Cart.total
      if (orderTotal > Customer.cash)
        return false

      Customer.cash -= orderTotal
      Shop.cash += orderTotal

      // just for debugging...
      Customer.owns ++= Cart.items
      true
    }

    def getMemberPriceOf(product: Product) = {
      val customerIsGoldMember = Shop.goldMembers.contains(Customer)
      val goldMemberReduction = 0.5
      val discountFactor = if (customerIsGoldMember) goldMemberReduction else 1
      (product.price * discountFactor).toInt
    }
  }

  private val Cart = Order(Customer)
  role(Cart) {
    def addItem(productId: Int, product: Product) {
      Cart.items.put(productId, product)
    }
    def removeItem(productId: Int): Option[Product] = {
      if (!Cart.items.isDefinedAt(productId))
        return None
      Cart.items.remove(productId)
    }
    def getItems = Cart.items.toIndexedSeq.sortBy(_._1)
    def total = Cart.items.map(_._2.price).sum
  }
}

// Environment (Web Controller or the like...)
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
  showResult("SHOPPING CART 4")

  // Various scenarios
  {
    println("\n######## Main success scenario ####################")
    val placeOrder = new PlaceOrder(shop, customer)
    println(s"Step 1: Customer marks Desired Products in Shop")
    placeOrder.customerMarksDesiredProductInShop(1)
    placeOrder.customerMarksDesiredProductInShop(2)
    println(s"Step 2: Customer requests to review order")
    println(s"Shop presents items in cart: \n" +
      placeOrder.customerRequestsToReviewOrder.mkString("\n"))
    println(s"Step 3: Customer pays order")
    val paymentStatus = placeOrder.customerPaysOrder
    println(s"Order completed? $paymentStatus\n")
    showResult("Customer bought wax for 40:")
  }
  {
    reset()
    println("\n######## Scenario A ###############################")
    val placeOrder = new PlaceOrder(shop, customer)
    val desiredProduct = 1
    shop.stock.remove(desiredProduct)
    val itemAdded = placeOrder.customerMarksDesiredProductInShop(desiredProduct)
    if (itemAdded == None)
      println(s"@@ Deviation 1a: Product $desiredProduct out of stock!\n")
    val otherDesiredProduct = 2
    placeOrder.customerMarksDesiredProductInShop(otherDesiredProduct)
    placeOrder.customerPaysOrder
    showResult("Customer bought tires instead for 600:")
  }
  {
    reset()
    println("\n######## Scenario B ###############################")
    val placeOrder = new PlaceOrder(shop, customer)
    // Customer is gold member
    shop.goldMembers.add(customer)
    placeOrder.customerMarksDesiredProductInShop(1)
    println(s"@@ Deviation 1b: Customer has gold membership\n")
    placeOrder.customerPaysOrder
    showResult("Customer has paid half price of 20:")
  }
  {
    reset()
    println("\n######## Scenario C ###############################")
    val placeOrder = new PlaceOrder(shop, customer)
    val desiredProduct = 3
    val itemAdded = placeOrder.customerMarksDesiredProductInShop(desiredProduct)
    val paymentCompleted = placeOrder.customerPaysOrder
    if (!paymentCompleted)
      println(s"@@ Deviation 3a: Insufficient funds for ${itemAdded.get.name}\n")
    showResult("Order placement aborted:")
  }

  // All deviations and trigger methods in play...
  {
    reset()
    println("\n######## Scenario ABC #############################")
    val placeOrder = new PlaceOrder(shop, customer)

    // Tires out of stock
    val tires = 2
    shop.stock.remove(tires)

    // We have a gold member
    shop.goldMembers.add(customer)

    // Trying to buy tires
    val itemAdded = placeOrder.customerMarksDesiredProductInShop(tires)
    if (itemAdded == None)
      println(s"@@ Deviation 1a: Product $tires out of stock!")

    // Let's buy the BMW instead then - as a gold member that should be possible!
    val BMW = 3
    val newItemAdded = placeOrder.customerMarksDesiredProductInShop(BMW)
    println(s"@@ Deviation 1b: Customer has gold membership")
    val paymentCompleted = placeOrder.customerPaysOrder

    // Ouch - still too expensive
    if (!paymentCompleted)
      println(s"@@ Deviation 3a: ${customer.cash} not enough to buy  " +
        s"${newItemAdded.get.name} (${newItemAdded.get.price} needed)")

    // Ok, no new car today
    placeOrder.customerRemovesProductFromCart(BMW)
    println(s"@@ Deviation 3a.1.a: Customer removes unaffordable item from cart\n" +
      placeOrder.customerRequestsToReviewOrder.mkString("\n"))

    // Let's get some wax anyway...
    placeOrder.customerMarksDesiredProductInShop(1)

    // Now we can afford it
    placeOrder.customerPaysOrder

    showResult("Gold customer can't get out-of-stock tires, neither " +
      "too expensive BMW (even with gold discount). Ok, some wax then:")
  }
}