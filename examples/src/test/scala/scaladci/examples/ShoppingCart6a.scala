package scaladci
package examples.shoppingcart6a

import DCI._
import scala.collection.mutable

/*
Shopping cart example (version 6a) - Distributed DCI model

This version tries out a distributed model where each Role method calls another
Role method until a part of the UC is accomplished. Trygve ReenSkaug describes this
approach here:
https://groups.google.com/forum/#!msg/object-composition/8Qe00Vt3MPc/4v9Wca3NSHIJ

Compare this to version 5a (the DCI-ish mediator version) to see the difference
between the two approaches!

See discussion at:
https://groups.google.com/forum/?fromgroups=#!topic/object-composition/JJiLWBsZWu0

===========================================================================
USE CASE:	Place Order [user-goal]

Person browsing around finds product(s) in a web shop that he/she wants to buy.

Primary actor.. Web customer ("Customer")
Scope.......... Web shop ("Shop")
Preconditions.. Shop presents product(s) to Customer
Trigger........ Customer wants to buy certain product(s)

A "Shopping Cart" is a virtual/visual representation of a potential Order in the UI.
We therefore loosely treat "Order" as synonymous to "Cart".

Main Success Scenario
---------------------------------------------------------------------------
1. Customer selects desired Product [can repeat]
    .1 Warehouse confirms Product availability
    .2 Customer Department provides eligible customer discount factor for Product
    .3 System adds product with qualified price to Cart
    .4 UI shows updated content of Cart to Customer
2. Customer requests to review Order
    .1 System collects Cart items
    .2 UI shows content of cart to Customer
3. Customer requests to pay Order
    .1 Payment Gateway confirms Customer has sufficient funds available
    .2 Payment Gateway initiates transfer of funds to Company
    .3 Warehouse prepare products for shipment to Customer
    .4 UI confirms purchase to Customer

Deviations
---------------------------------------------------------------------------
1a. Product is out of stock:
    1. UI informs Customer that Product is out of stock.

1b. Customer has gold membership:
    1. System adds discounted product to Cart.

3a. Customer has insufficient funds to pay Order:
    1. UI informs Customer of insufficient funds available.
        a. Customer removes unaffordable Product from Cart
            1. System updates content of Cart
            1. UI shows updated content of Cart to Customer
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
class PlaceOrder(Company: Company, Customer: Person) extends Context {

  // Context state ("Methodless roles"?)
  private var EligibleDiscountFactor = 1.0
  private var DesiredProductId = 0

  // Trigger methods
  def processProductSelection(desiredProductId: Int): Option[Product] = {
    DesiredProductId = desiredProductId

    // Step 1.1 Initiate first interaction...
    Warehouse.confirmAvailability(desiredProductId)
  }

  def getOrderDetails: Seq[(Int, Product)] = Cart.getItems

  def processPayment: Boolean = PaymentGateway.confirmSufficientFunds

  def processProductRemoval(productId: Int): Option[Product] = Cart.removeItem(productId)

  // Roles (in order of "appearance")
  private val Warehouse = Company
  private val CustomerDepartment = Company
  private val PaymentGateway = Company
  private val CompanyAccount = Company
  private val Cart = Order(Customer)

  role(Warehouse) {
    def confirmAvailability(productId: Int): Option[Product] = {
      if (!Warehouse.has(DesiredProductId))
        return None

      // Step 1.2 Second interaction...
      CustomerDepartment.calculateEligibleDiscountFactor
    }
    def has(productId: Int) = Warehouse.stock.isDefinedAt(productId)
    def get(productId: Int) = Warehouse.stock(productId)
    def shipProducts = {
      Customer.owns ++= Cart.items
      Cart.items.foreach(i => Warehouse.stock.remove(i._1))
      true // dummy confirmation
    }
  }

  role(CustomerDepartment) {
    def calculateEligibleDiscountFactor = {
      if (Customer.isGoldMember) EligibleDiscountFactor = 0.5

      // Step 1.3 Third interaction...
      Cart.addItem
    }
  }

  role(Customer) {
    def withdrawFunds(amountToPay: Int) { Customer.cash -= amountToPay }
    def receiveProducts(products: Seq[(Int, Product)]) { Customer.owns ++= products }
    def isGoldMember = CustomerDepartment.goldMembers.contains(Customer)
  }

  role(Cart) {
    def addItem = {
      val product = Warehouse.get(DesiredProductId)
      val qualifiedPrice = (product.price * EligibleDiscountFactor).toInt
      val qualifiedProduct = product.copy(price = qualifiedPrice)
      Cart.items.put(DesiredProductId, qualifiedProduct)
      Some(qualifiedProduct)
    }
    def removeItem(productId: Int): Option[Product] = {
      if (!Cart.items.isDefinedAt(productId))
        return None
      Cart.items.remove(productId)
    }
    def getItems = Cart.items.toIndexedSeq.sortBy(_._1)
    def total = Cart.items.map(_._2.price).sum
  }

  role(PaymentGateway) {
    // Step 3.1
    def confirmSufficientFunds: Boolean = {
      if (Customer.cash < Cart.total) return false

      // Step 3.2
      PaymentGateway.initiateOrderPayment
    }
    def initiateOrderPayment = {
      val amount = Cart.total
      Customer.withdrawFunds(amount)
      CompanyAccount.depositFunds(amount)

      // Step 3.3
      Warehouse.shipProducts
    }
  }

  role(CompanyAccount) {
    def depositFunds(amount: Int) { Company.cash += amount }
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
  showResult("SHOPPING CART 6a")

  // Various scenarios
  {
    println("\n######## Main success scenario ####################")
    val placeOrder = new PlaceOrder(shop, customer)
    println(s"Step 1: Customer marks Desired Products in Shop")
    placeOrder.processProductSelection(1)
    placeOrder.processProductSelection(2)
    println(s"Step 2: Customer requests to review order")
    println(s"Shop presents items in cart: \n" +
      placeOrder.getOrderDetails.mkString("\n"))
    println(s"Step 3: Customer pays order")
    val paymentStatus = placeOrder.processPayment
    println(s"Order completed? $paymentStatus\n")
    showResult("Customer bought wax for 40:")
  }
  {
    reset()
    println("\n######## Scenario A ###############################")
    val placeOrder = new PlaceOrder(shop, customer)
    val desiredProduct = 1
    shop.stock.remove(desiredProduct)
    val itemAdded = placeOrder.processProductSelection(desiredProduct)
    if (itemAdded == None)
      println(s"@@ Deviation 1a: Product $desiredProduct out of stock!\n")
    val otherDesiredProduct = 2
    placeOrder.processProductSelection(otherDesiredProduct)
    placeOrder.processPayment
    showResult("Customer bought tires instead for 600:")
  }
  {
    reset()
    println("\n######## Scenario B ###############################")
    val placeOrder = new PlaceOrder(shop, customer)
    // Customer is gold member
    shop.goldMembers.add(customer)
    placeOrder.processProductSelection(1)
    println(s"@@ Deviation 1b: Customer has gold membership\n")
    placeOrder.processPayment
    showResult("Customer has paid half price of 20:")
  }
  {
    reset()
    println("\n######## Scenario C ###############################")
    val placeOrder = new PlaceOrder(shop, customer)
    val desiredProduct = 3
    val itemAdded = placeOrder.processProductSelection(desiredProduct)
    val paymentCompleted = placeOrder.processPayment
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
    val itemAdded = placeOrder.processProductSelection(tires)
    if (itemAdded == None)
      println(s"@@ Deviation 1a: Product $tires out of stock!")

    // Let's buy the BMW instead then - as a gold member that should be possible!
    val BMW = 3
    val newItemAdded = placeOrder.processProductSelection(BMW)
    println(s"@@ Deviation 1b: Customer has gold membership")
    val paymentCompleted = placeOrder.processPayment

    // Ouch - still too expensive
    if (!paymentCompleted)
      println(s"@@ Deviation 3a: ${customer.cash} not enough to buy  " +
        s"${newItemAdded.get.name} (${newItemAdded.get.price} needed)")

    // Ok, no new car today
    placeOrder.processProductRemoval(BMW)
    println(s"@@ Deviation 3a.1.a: Customer removes unaffordable item from cart\n" +
      placeOrder.getOrderDetails.mkString("\n") + "\n")

    // Let's get some wax anyway...
    placeOrder.processProductSelection(1)

    // Now we can afford it
    placeOrder.processPayment

    showResult("Gold customer can't get out-of-stock tires, neither " +
      "too expensive BMW (even with gold discount). Ok, some wax then:")
  }
}