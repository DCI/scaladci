package scaladci
package examples
import org.specs2.mutable.Specification

/*
Shopping cart example (version 5) - (Non-DCI?) Centralized Mediator model

This version re-introduces more Roles reflecting the participants of a richer mental
model drawn from both the user and application programmers mental models.

Each UC step is a User action followed by a series of system actions with the last
action returning some data or status to the UI.

Each trigger method in the Context is named after the overall description of those
system response actions rather than the User action/step that initiates them as we
had in earlier versions.

Each trigger method body contains all the system response actions as a centralized
algorithm resembling the Mediator pattern. Trygve Reenskaug has a great explanation
here of how this approach differs from DCI:
https://groups.google.com/forum/#!msg/object-composition/8Qe00Vt3MPc/4v9Wca3NSHIJ

Side note: Changed "marks" to "selects" in UC step 1 since the user might as well
"click", "drag" "tell" etc - all UI words representing the same intention of selection.
We have also already defined the Shop as our scope. So we don't need to write where
the Customer is selecting ("Customer selects desired product _in Shop_").

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
    - Warehouse confirms Product availability
    - Customer Department provides eligible customer discount factor for Product
    - System adds product with qualified price to Cart
    - UI shows updated content of Cart to Customer
2. Customer requests to review Order
    - System collects Cart items
    - UI shows content of cart to Customer
3. Customer requests to pay Order
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

3a. Customer has insufficient funds to pay Order:
    1. UI informs Customer of insufficient funds available.
        a. Customer removes unaffordable Product from Cart
            1. System updates content of Cart
            1. UI shows updated content of Cart to Customer
===========================================================================
*/

class ShoppingCart5 extends Specification {
  import ShoppingCartModel._

  @context
  class PlaceOrder(Company: Company, Customer: Person) {

    // Trigger methods
    def processProductSelection(desiredProductId: Int): Option[Product] = {
      if (!Warehouse.has(desiredProductId))
        return None

      val discountFactor = CustomerDepartment.calculateEligibleDiscountFactor

      val product = Warehouse.get(desiredProductId)
      val qualifiedPrice = (product.price * discountFactor).toInt
      val qualifiedProduct = product.copy(price = qualifiedPrice)

      Cart.addItem(desiredProductId, qualifiedProduct)

      Some(qualifiedProduct)
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
    private val Warehouse          = Company
    private val CustomerDepartment = Company
    private val PaymentGateway     = Company
    private val CompanyAccount     = Company
    private val Cart               = Order(Customer)

    role Warehouse {
      def has(productId: Int) = Warehouse.stock.isDefinedAt(productId)
      def get(productId: Int) = Warehouse.stock(productId)
      def shipProducts = {
        Customer.owns ++= Cart.items
        Cart.items.foreach(i => Warehouse.stock.remove(i._1))
        true // dummy delivery confirmation
      }
    }

    role CustomerDepartment {
      def calculateEligibleDiscountFactor = if (Customer.isGoldMember) 0.5 else 1
    }

    role Customer {
      def withdrawFunds(amountToPay: Int) { Customer.cash -= amountToPay }
      def receiveProducts(products: Seq[(Int, Product)]) { Customer.owns ++= products }
      def isGoldMember = CustomerDepartment.goldMembers.contains(Customer)
    }

    role Cart {
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
      def depositFunds(amount: Int) { self.cash += amount }
    }
  }


  // Test various scenarios.
  // (copy and paste of ShoppingCart4 tests with trigger method name changes)

  "Main success scenario" in new shoppingCart {

    // Initial status (same for all tests...)
    shop.stock === Map(tires, wax, bmw)
    shop.cash === 100000
    customer.cash === 20000
    customer.owns === Map()

    val order = new PlaceOrder(shop, customer)

    // Customer wants wax and tires
    order.processProductSelection(p1)
    order.processProductSelection(p2)

    order.getOrderDetails === Seq(wax, tires)

    val orderCompleted = order.processPayment === true

    shop.stock === Map(bmw)
    shop.cash === 100000 + 40 + 600
    customer.cash === 20000 - 40 - 600
    customer.owns === Map(tires, wax)
  }

  "Product out of stock" in new shoppingCart {

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
    shop.cash === 100000 + 600
    customer.cash === 20000 - 600
    customer.owns === Map(tires)
  }

  "Customer has gold membership" in new shoppingCart {

    // Customer is gold member
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

  "Customer has too low credit" in new shoppingCart {

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
    shop.cash === 100000
    customer.cash === 20000
    customer.owns === Map()
  }

  "All deviations in play" in new shoppingCart {

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