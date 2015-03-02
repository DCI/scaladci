package scaladci
package examples
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.collection.mutable

/*
Shopping cart example (version 7), contributed by Mathew Browne

This is a new version based on version 5. The main changes are the removal of the
CustomerDepartment role and distributing more of the interaction logic to the roles.

Each UC step is a User action followed by a series of system actions with the last
action returning some data or status to the UI.

Each trigger method in the Context is named after the overall description of those
system response actions rather than the User action/step that initiates them as we
had in earlier versions.

See discussion about the current version at:
https://groups.google.com/forum/?fromgroups=#!topic/object-composition/E6fTRc9R9j8

.. and original discussion:
https://groups.google.com/d/msg/object-composition/JJiLWBsZWu0/1u6HW3J_nawJ

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
    - Warehouse confirms Product availability
2. System adds product with qualified price to Cart
    - Company membership database provides eligible customer discount factor for Product
    - UI shows updated content of Cart to Customer
3. Customer requests to review Order
    - System collects Cart items
    - UI shows content of cart to Customer
4. Customer requests to pay Order
    - Payment Gateway confirms Customer has sufficient funds available
    - Payment Gateway initiates transfer of funds to Company
    - Warehouse prepare products for shipment to Customer
    - UI confirms purchase to Customer

Deviations
---------------------------------------------------------------------------
1a. Product is out of stock:
    1. UI informs Customer that Product is out of stock.

1b. Customer has gold membership:
    1. System adds discounted product to Cart.

4a. Customer has insufficient funds to pay Order:
    1. UI informs Customer of insufficient funds available.
        a. Customer removes unaffordable Product from Cart
            1. System updates content of Cart
            1. UI shows updated content of Cart to Customer
===========================================================================
*/

// 4 basic "dumb" Data types - Company now has a bank account
object ShoppingCartModel7 {
  case class Product(name: String, price: Int)
  case class Person(name: String, var cash: Int, owns: mutable.Map[Int, Product] = mutable.Map())
  case class Company(name: String, var bankAccount: Int, stock: mutable.Map[Int, Product], goldMembers: mutable.Set[Person])
  case class Order(customer: Person, items: mutable.Map[Int, Product] = mutable.Map())
}

// Setup for each test
trait ShoppingCart7setup extends Scope {
  import ShoppingCartModel7._
  val (p1, p2, p3)      = (1, 2, 3)
  val (wax, tires, bmw) = (p1 -> Product("Wax", 40), p2 -> Product("Tires", 600), p3 -> Product("BMW", 50000))
  val shop              = Company("Don's Auto shop", 100000, mutable.Map(wax, tires, bmw), mutable.Set())
  val customer          = Person("Matthew", 20000)
}

class ShoppingCart7 extends Specification {
  import ShoppingCartModel7._

  @context
  class PlaceOrder(Company: Company, Customer: Person) {

    // Trigger methods
    def processProductSelection(desiredProductId: Int): Option[Product] = {
      if (!Warehouse.has(desiredProductId))
        return None

      Cart.addItem(desiredProductId)
    }

    def getOrderDetails: Seq[(Int, Product)] = Cart.getItems

    def processPayment: Boolean = {
      if (!PaymentGateway.confirmSufficientFunds) return false
      if (!PaymentGateway.initiateOrderPayment) return false
      Warehouse.shipProducts
    }

    def processProductRemoval(productId: Int): Option[Product] = {
      Cart.removeItem(productId)
    }

    // Roles (in order of "appearance")
    private val Warehouse      = Company
    private val Cart           = Order(Customer)
    private val PaymentGateway = Company
    private val CompanyAccount = Company

    role Warehouse {
      def has(productId: Int) = Warehouse.stock.isDefinedAt(productId)
      def get(productId: Int) = Warehouse.stock(productId)
      def shipProducts = {
        Customer.owns ++= Cart.items
        Cart.items.foreach(i => Warehouse.stock.remove(i._1))
        true // dummy delivery confirmation
      }
    }

    // This role has no methods, because we are only using it to access the goldMembers collection.
    // We still designate it as a role because it is conceptually part of an interaction with the Customer role.
    role Company {}

    role Customer {
      def withdrawFunds(amountToPay: Int) { Customer.cash -= amountToPay }
      def receiveProducts(products: Seq[(Int, Product)]) { Customer.owns ++= products }
      def isGoldMember = Company.goldMembers.contains(Customer)
      def discountFactor = if (isGoldMember) 0.5 else 1
    }

    role Cart {
      def addItem(productId: Int) = {
        val product = Warehouse.get(productId)
        val qualifiedPrice = (product.price * Customer.discountFactor).toInt
        val qualifiedProduct = product.copy(price = qualifiedPrice)

        Cart.items.put(productId, qualifiedProduct)
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

    role PaymentGateway {
      def confirmSufficientFunds = Customer.cash >= Cart.total
      def initiateOrderPayment = {
        val amount = Cart.total
        Customer.withdrawFunds(amount)
        CompanyAccount.depositFunds(amount)
        true // dummy transaction success
      }
    }

    role CompanyAccount {
      def depositFunds(amount: Int) { self.bankAccount += amount }
    }
  }


  // Test various scenarios.
  // (copy and paste of ShoppingCart5 tests with added bankAccount property of shop)

  "Main success scenario" in new ShoppingCart7setup {

    // Initial status (same for all tests...)
    shop.stock === Map(tires, wax, bmw)
    shop.bankAccount === 100000
    customer.cash === 20000
    customer.owns === Map()

    val order = new PlaceOrder(shop, customer)

    // Customer wants wax and tires
    order.processProductSelection(p1)
    order.processProductSelection(p2)

    order.getOrderDetails === Seq(wax, tires)

    val orderCompleted = order.processPayment === true

    shop.stock === Map(bmw)
    shop.bankAccount === 100000 + 40 + 600
    customer.cash === 20000 - 40 - 600
    customer.owns === Map(tires, wax)
  }

  "Product out of stock" in new ShoppingCart7setup {

    // Wax out of stock
    shop.stock.remove(p1)
    shop.stock === Map(tires, bmw)

    val order = new PlaceOrder(shop, customer)

    // Customer wants wax
    val itemAdded = order.processProductSelection(p1) === None
    order.getOrderDetails === Seq()

    order.processProductSelection(p2)

    val orderCompleted = order.processPayment === true

    shop.stock === Map(bmw)
    shop.bankAccount === 100000 + 600
    customer.cash === 20000 - 600
    customer.owns === Map(tires)
  }

  "Customer has gold membership" in new ShoppingCart7setup {

    // Customer is gold member
    shop.goldMembers.add(customer)
    shop.goldMembers.contains(customer) === true

    val order = new PlaceOrder(shop, customer)

    order.processProductSelection(p1)

    val discountedWax = 1 -> Product("Wax", (40 * 0.5).toInt)
    order.getOrderDetails === Seq(discountedWax)

    val orderCompleted = order.processPayment === true

    shop.stock === Map(tires, bmw)
    shop.bankAccount === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(discountedWax)
  }

  "Customer has too low credit" in new ShoppingCart7setup {

    val order = new PlaceOrder(shop, customer)

    // Customer wants a BMW
    val itemAdded = order.processProductSelection(p3)

    // Any product is added - shop doesn't yet know if customer can afford it
    itemAdded === Some(bmw._2)
    order.getOrderDetails === Seq(bmw)

    // Customer tries to pay order
    val paymentStatus = order.processPayment

    // Shop informs Customer of too low credit
    paymentStatus === false

    // Customer removes unaffordable BMW from cart
    order.processProductRemoval(p3)

    // Customer aborts shopping and no purchases are made
    shop.stock === Map(tires, wax, bmw)
    shop.bankAccount === 100000
    customer.cash === 20000
    customer.owns === Map()
  }

  "All deviations in play" in new ShoppingCart7setup {

    // Tires out of stock
    shop.stock.remove(p2)
    shop.stock === Map(wax, bmw)

    // We have a gold member
    shop.goldMembers.add(customer)

    val order = new PlaceOrder(shop, customer)

    // Let's get some tires
    val tiresItemAdded = order.processProductSelection(p2)

    // Product out of stock!
    shop.stock.contains(p2) === false

    // Nothing added to order yet
    tiresItemAdded === None
    order.getOrderDetails === Seq()

    // Let's buy the BMW instead. As a gold member that should be possible!
    val bmwItemAdded = order.processProductSelection(p3)

    // Discounted BMW is added to order
    val discountedBMW = Product("BMW", (50000 * 0.5).toInt)
    bmwItemAdded === Some(discountedBMW)
    order.getOrderDetails === Seq(p3 -> discountedBMW)

    // Ouch! We couldn't afford it.
    val paymentAttempt1 = order.processPayment === false

    // It's still 5000 too much for us, even with the membership discount
    discountedBMW.price - customer.cash === 5000

    // Ok, no new car today
    order.processProductRemoval(p3)

    // Order is back to empty
    order.getOrderDetails === Seq()

    // Let's get some wax anyway...
    val waxItemAdded = order.processProductSelection(p1)

    // Did we get our membership discount on this one?
    val discountedWax = Product("Wax", (40 * 0.5).toInt)
    waxItemAdded === Some(discountedWax)

    // Now we can afford it!
    val paymentAttempt2 = order.processPayment === true

    // Not much shopping done Today. At least we got some cheap wax.
    shop.stock === Map(bmw)
    shop.bankAccount === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(p1 -> discountedWax)
  }
}