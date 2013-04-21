package scaladci
package examples.shoppingcart4b

import DCI._
import scala.collection.mutable

/*
Shopping cart example (version 4b) - only 1 Customer role!

The Cart now has no role methods (would that be what we call a "methodless role"?)

Can we talk about any Interactions having only the Customer role left? We are definitely
drifting away from our mental model...

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
1. Customer selects desired product in shop [can repeat]
    - System confirms product availability
    - System adds product to order
    - UI shows updated contents of cart to Customer
2. Customer requests to review order
    - UI shows cart with products/prices to Customer
3. Customer pays order
    - System confirms sufficient funds are available
    - System initiates transfer of funds
    - UI confirms purchase to Customer

Deviations
---------------------------------------------------------------------------
1a. Product is out of stock:
    1. UI informs Customer that product is out of stock.

1b. Customer has gold membership:
    1. System adds discounted product to order.

3a. Customer has insufficient funds to pay order:
    1. UI informs Customer of insufficient funds.
        a. Customer removes unaffordable item(s) from Cart:
            1. Go to step 3.
===========================================================================
*/

// Data
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

// DCI Context
class PlaceOrder(Shop: Company, Customer: Person) extends Context {
  // "Methodless role"?
  private val Cart = Order(Customer)

  // UC steps taken
  def customerSelectedDesiredProduct(productId: Int): Option[Product] =
    Customer.selectDesiredProduct(productId)
  def customerRequestedToReviewOrder: Seq[(Int, Product)] =
    Customer.reviewOrder
  def customerRequestedToPayOrder: Boolean =
    Customer.payOrder

  // Deviation(s)
  def customerRemovedProductFromCart(productId: Int): Option[Product] =
    Customer.removeProductFromCart(productId)

  // Only 1 role implementation!
  role(Customer) {
    def selectDesiredProduct(productId: Int): Option[Product] = {
      if (!Shop.stock.isDefinedAt(productId))
        return None
      val product = Shop.stock(productId)
      val discountedPrice = Customer.getMemberPriceOf(product)
      val desiredProduct = product.copy(price = discountedPrice)
      Cart.items.put(productId, desiredProduct)
      Some(desiredProduct)
    }
    def reviewOrder = Cart.items.toIndexedSeq.sortBy(_._1)
    def removeProductFromCart(productId: Int): Option[Product] = {
      if (!Cart.items.isDefinedAt(productId))
        return None
      Cart.items.remove(productId)
    }
    def payOrder: Boolean = {
      val orderTotal = Cart.items.map(_._2.price).sum
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
  showResult("SHOPPING CART 4b")

  // Various scenarios
  {
    println("\n######## Main success scenario ####################")
    val placeOrder = new PlaceOrder(shop, customer)
    println(s"Step 1: Customer marks Desired Products in Shop")
    placeOrder.customerSelectedDesiredProduct(1)
    placeOrder.customerSelectedDesiredProduct(2)
    println(s"Step 2: Customer requests to review order")
    println(s"Shop presents items in cart: \n" +
      placeOrder.customerRequestedToReviewOrder.mkString("\n"))
    println(s"Step 3: Customer pays order")
    val paymentStatus = placeOrder.customerRequestedToPayOrder
    println(s"Order completed? $paymentStatus\n")
    showResult("Customer bought wax for 40:")
  }
  {
    reset()
    println("\n######## Scenario A ###############################")
    val placeOrder = new PlaceOrder(shop, customer)
    val desiredProduct = 1
    shop.stock.remove(desiredProduct)
    val itemAdded = placeOrder.customerSelectedDesiredProduct(desiredProduct)
    if (itemAdded == None)
      println(s"@@ Deviation 1a: Product $desiredProduct out of stock!\n")
    val otherDesiredProduct = 2
    placeOrder.customerSelectedDesiredProduct(otherDesiredProduct)
    placeOrder.customerRequestedToPayOrder
    showResult("Customer bought tires instead for 600:")
  }
  {
    reset()
    println("\n######## Scenario B ###############################")
    val placeOrder = new PlaceOrder(shop, customer)
    // Customer is gold member
    shop.goldMembers.add(customer)
    placeOrder.customerSelectedDesiredProduct(1)
    println(s"@@ Deviation 1b: Customer has gold membership\n")
    placeOrder.customerRequestedToPayOrder
    showResult("Customer has paid half price of 20:")
  }
  {
    reset()
    println("\n######## Scenario C ###############################")
    val placeOrder = new PlaceOrder(shop, customer)
    val desiredProduct = 3
    val itemAdded = placeOrder.customerSelectedDesiredProduct(desiredProduct)
    val paymentCompleted = placeOrder.customerRequestedToPayOrder
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
    val itemAdded = placeOrder.customerSelectedDesiredProduct(tires)
    if (itemAdded == None)
      println(s"@@ Deviation 1a: Product $tires out of stock!")

    // Let's buy the BMW instead then - as a gold member that should be possible!
    val BMW = 3
    val newItemAdded = placeOrder.customerSelectedDesiredProduct(BMW)
    println(s"@@ Deviation 1b: Customer has gold membership")
    val paymentCompleted = placeOrder.customerRequestedToPayOrder

    // Ouch - still too expensive
    if (!paymentCompleted)
      println(s"@@ Deviation 3a: ${customer.cash} not enough to buy  " +
        s"${newItemAdded.get.name} (${newItemAdded.get.price} needed)")

    // Ok, no new car today
    placeOrder.customerRemovedProductFromCart(BMW)
    println(s"@@ Deviation 3a.1.a: Customer removes unaffordable item from cart\n" +
      placeOrder.customerRequestedToReviewOrder.mkString("\n"))

    // Let's get some wax anyway...
    placeOrder.customerSelectedDesiredProduct(1)

    // Now we can afford it
    placeOrder.customerRequestedToPayOrder

    showResult("Gold customer can't get out-of-stock tires, neither " +
      "too expensive BMW (even with gold discount). Ok, some wax then:")
  }
}