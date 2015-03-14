package scaladci
package examples
import org.specs2.mutable.Specification

/*
Shopping cart example (version 6) - Distributed DCI model

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
Preconditions.. shop presents product(s) to customer
Trigger........ customer wants to buy certain product(s)

A "shopping cart" is a virtual/visual representation of a potential Order in the UI.
We therefore loosely treat "Order" as synonymous to "cart".

Main Success Scenario
---------------------------------------------------------------------------
1. Customer selects desired Product [can repeat]
    .1 Warehouse confirms Product availability
    .2 Customer Department provides eligible Customer discount factor for Product
    .3 System adds Product with qualified price to Cart
    .4 UI shows updated content of Cart to Customer
2. Customer requests to review Order
    .1 System collects Cart items
    .2 UI shows content of Cart to Customer
3. Ccustomer requests to pay Order
    .1 Payment Gateway confirms Customer has sufficient funds available
    .2 Payment Gateway initiates transfer of funds to Company
    .3 Wwarehouse prepare Products for shipment to Customer
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

class ShoppingCart6 extends Specification {
  import ShoppingCartModel._

  @context
  class PlaceOrder(Company: Company, customer: Person) {

    // Context state ("Methodless roles"?)
    private var eligibleDiscountFactor = 1.0
    private var desiredProductId       = 0

    // Trigger methods
    def processProductSelection(desiredProdId: Int): Option[Product] = {
      desiredProductId = desiredProdId

      // Step 1.1 Initiate first interaction...
      warehouse.confirmAvailability(desiredProdId)
    }

    def getOrderDetails: Seq[(Int, Product)] = cart.getItems

    def processPayment: Boolean = paymentGateway.confirmSufficientFunds

    def processProductRemoval(productId: Int): Option[Product] = cart.removeItem(productId)

    // Roles (in order of "appearance")
    private val warehouse          = Company
    private val customerDepartment = Company
    private val paymentGateway     = Company
    private val companyAccount     = Company
    private val cart               = Order(customer)

    role warehouse {
      def confirmAvailability(productId: Int): Option[Product] = {
        if (!warehouse.has(desiredProductId))
          return None

        // Step 1.2 Second interaction...
        customerDepartment.calculateEligibleDiscountFactor
      }
      def has(productId: Int) = warehouse.stock.isDefinedAt(productId)
      def get(productId: Int) = warehouse.stock(productId)
      def shipProducts = {
        customer.owns ++= cart.items
        cart.items.foreach(i => warehouse.stock.remove(i._1))
        true // dummy confirmation
      }
    }

    role customerDepartment {
      def calculateEligibleDiscountFactor = {
        if (customer.isGoldMember) eligibleDiscountFactor = 0.5

        // Step 1.3 Third interaction...
        cart.addItem
      }
    }

    role customer {
      def withdrawFunds(amountToPay: Int) { customer.cash -= amountToPay }
      def receiveProducts(products: Seq[(Int, Product)]) { customer.owns ++= products }
      def isGoldMember = customerDepartment.goldMembers.contains(customer)
    }

    role cart {
      def addItem = {
        val product = warehouse.get(desiredProductId)
        val qualifiedPrice = (product.price * eligibleDiscountFactor).toInt
        val qualifiedProduct = product.copy(price = qualifiedPrice)
        cart.items.put(desiredProductId, qualifiedProduct)
        Some(qualifiedProduct)
      }
      def removeItem(productId: Int): Option[Product] = {
        if (!cart.items.isDefinedAt(productId))
          return None
        cart.items.remove(productId)
      }
      def getItems = cart.items.toIndexedSeq.sortBy(_._1)
      def total = cart.items.map(_._2.price).sum
    }

    role paymentGateway {
      // Step 3.1
      def confirmSufficientFunds: Boolean = {
        if (customer.cash < cart.total) return false

        // Step 3.2
        initiateOrderPayment
      }
      def initiateOrderPayment: Boolean = {
        val amount = cart.total
        customer.withdrawFunds(amount)
        companyAccount.depositFunds(amount)

        // Step 3.3
        warehouse.shipProducts
      }
    }

    role companyAccount {
      def depositFunds(amount: Int) { self.cash += amount }
    }
  }


  // Test various scenarios.
  // (copy and paste of ShoppingCart4a tests)

  "Main success scenario" in new ShoppingCart {

    // Initial status (same for all tests...)
    shop.stock === Map(tires, wax, bmw)
    shop.cash === 100000
    customer.cash === 20000
    customer.owns === Map()

    val order = new PlaceOrder(shop, customer)

    // customer wants wax and tires
    order.processProductSelection(p1)
    order.processProductSelection(p2)

    order.getOrderDetails === Seq(wax, tires)

    val orderCompleted = order.processPayment === true

    shop.stock === Map(bmw)
    shop.cash === 100000 + 40 + 600
    customer.cash === 20000 - 40 - 600
    customer.owns === Map(tires, wax)
  }

  "Product out of stock" in new ShoppingCart {

    // Wax out of stock
    shop.stock.remove(p1)
    shop.stock === Map(tires, bmw)

    val order = new PlaceOrder(shop, customer)

    // customer wants wax
    val itemAdded = order.processProductSelection(p1) === None
    order.getOrderDetails === Seq()

    order.processProductSelection(p2)

    val orderCompleted = order.processPayment === true

    shop.stock === Map(bmw)
    shop.cash === 100000 + 600
    customer.cash === 20000 - 600
    customer.owns === Map(tires)
  }

  "customer has gold membership" in new ShoppingCart {

    // customer is gold member
    shop.goldMembers.add(customer)
    shop.goldMembers.contains(customer) === true

    val order = new PlaceOrder(shop, customer)

    order.processProductSelection(p1)

    val discountedWax = 1 -> Product("Wax", (40 * 0.5).toInt)
    order.getOrderDetails === Seq(discountedWax)

    val orderCompleted = order.processPayment === true

    shop.stock === Map(tires, bmw)
    shop.cash === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(discountedWax)
  }

  "customer has too low credit" in new ShoppingCart {

    val order = new PlaceOrder(shop, customer)

    // customer wants a BMW
    val itemAdded = order.processProductSelection(p3)

    // Any product is added - shop doesn't yet know if customer can afford it
    itemAdded === Some(bmw._2)
    order.getOrderDetails === Seq(bmw)

    // customer tries to pay order
    val paymentStatus = order.processPayment

    // shop informs customer of too low credit
    paymentStatus === false

    // customer removes unaffordable BMW from cart
    order.processProductRemoval(p3)

    // customer aborts shopping and no purchases are made
    shop.stock === Map(tires, wax, bmw)
    shop.cash === 100000
    customer.cash === 20000
    customer.owns === Map()
  }

  "All deviations in play" in new ShoppingCart {

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
    shop.cash === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(p1 -> discountedWax)
  }
}