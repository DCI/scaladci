package scaladci
package examples.shoppingcart4c

import scala.collection.mutable

/*
Shopping cart example (version 4c) - no Roles!!

No role methods, no interaction between objects (except with the Cart).

No DCI anymore - only procedural algorithms in the Context which could be
any class now (notice it's not extending Context anymore).

Could we call it a Service now? If so, the specifications/requirements
below doesn't tell a story any more.

See discussion at:
https://groups.google.com/forum/?fromgroups=#!topic/object-composition/JJiLWBsZWu0

===========================================================================
Shopping Cart Service (disclaimer: don't know how to specify a Service...)

Specifications:
---------------------------------------------------------------------------
Add product to cart:
  - reserve product in Warehouse
  - add item to order
  - show updated contents of cart to customer

Review order:
  - present cart with current items/prices to customer

Pay order:
  - confirm sufficient funds are available
  - initiate transfer of funds
  - confirm purchase to customer

Remove item from cart:
  - show updated cart to customer

Requirements
---------------------------------------------------------------------------
Product out of stock:
  - don't add item to cart
  - inform customer of shortage

Customer has gold membership:
  - calculate discount on products

Customer has insufficient funds to pay Order:
  - inform customer of insufficient funds on credit card
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

// No DCI Context any longer - is it a "Service" now??
class PlaceOrder(Shop: Company, Customer: Person) {
  // No Role any longer
  private val cart = Order(Customer)

  // Service methods
  def customerMarksDesiredProductInShop(productId: Int): Option[Product] = {
    if (!Shop.stock.isDefinedAt(productId))
      return None
    val product = Shop.stock(productId)

    // get price with discount if any
    val customerIsGoldMember = Shop.goldMembers.contains(Customer)
    val goldMemberReduction = 0.5
    val discountFactor = if (customerIsGoldMember) goldMemberReduction else 1
    val discountedPrice = (product.price * discountFactor).toInt

    val desiredProduct = product.copy(price = discountedPrice)
    cart.items.put(productId, desiredProduct)
    Some(desiredProduct)
  }

  def customerRequestsToReviewOrder: Seq[(Int, Product)] = {
    cart.items.toIndexedSeq.sortBy(_._1)
  }

  def customerPaysOrder: Boolean = {
    val orderTotal = cart.items.map(_._2.price).sum
    if (orderTotal > Customer.cash)
      return false

    Customer.cash -= orderTotal
    Shop.cash += orderTotal

    // just for debugging...
    Customer.owns ++= cart.items
    true
  }

  def customerRemovesProductFromCart(productId: Int): Option[Product] = {
    if (!cart.items.isDefinedAt(productId))
      return None
    cart.items.remove(productId)
  }

  // (no role implementations!)
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
  showResult("SHOPPING CART 4c")

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