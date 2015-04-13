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
We have also already defined the shop as our scope. So we don't need to write where
the customer is selecting ("customer selects desired product _in shop_").

See discussion at:
https://groups.google.com/forum/?fromgroups=#!topic/object-composition/JJiLWBsZWu0

===========================================================================
USE CASE:	Place Order [user-goal]

Person browsing around finds product(s) in a web shop that he/she wants to buy.

Primary actor.. Web customer ("Customer")
Scope.......... Web shop ("Shop")
Preconditions.. Shop presents product(s) to customer
Trigger........ Customer wants to buy certain product(s)

A "shopping cart" is a virtual/visual representation of a potential Order in the UI.
We therefore loosely treat "Order" as synonymous to "cart".

Main Success Scenario
---------------------------------------------------------------------------
1. Customer selects desired Product [can repeat]
    - Warehouse confirms Product availability
    - Customer Department provides eligible Customer discount factor for Product
    - System adds Product with qualified price to Cart
    - UI shows updated content of Cart to Customer
2. Customer requests to review Order
    - System collects Cart items
    - UI shows content of Cart to Customer
3. Customer requests to pay Order
    - Payment Gateway confirms Customer has sufficient funds available
    - Payment Gateway initiates transfer of funds to Company
    - Warehouse prepare Products for shipment to Customer
    - UI confirms purchase to Customer

Deviations
---------------------------------------------------------------------------
1a. Product is out of stock:
    1. UI informs Customer that Product is out of stock.

1b. Customer has gold membership:
    1. System adds discounted Product to Cart.

3a. Customer has insufficient funds to pay Order:
    1. UI informs Customer of insufficient funds available.
        a. Customer removes unaffordable Product from cart
            1. System updates content of Cart
            1. UI shows updated content of Cart to Customer
===========================================================================
*/

class ShoppingCart5 extends Specification {
  import ShoppingCartModel._

  {
    @context
    class PlaceOrder(company: Company, customer: Person) {

      // Trigger methods
      def processProductSelection(desiredProductId: Int): Option[Product] = {
        if (!warehouse.has(desiredProductId))
          return None

        val discountFactor = customerDepartment.calculateEligibleDiscountFactor

        val product = warehouse.get(desiredProductId)
        val qualifiedPrice = (product.price * discountFactor).toInt
        val qualifiedProduct = product.copy(price = qualifiedPrice)

        cart.addItem(desiredProductId, qualifiedProduct)

        Some(qualifiedProduct)
      }

      def getOrderDetails: Seq[(Int, Product)] = cart.getItems

      def processPayment: Boolean = {
        if (!paymentGateway.confirmSufficientFunds) return false
        if (!paymentGateway.initiateOrderPayment) return false
        warehouse.shipProducts
      }

      def processProductRemoval(productId: Int): Option[Product] = {
        cart.removeItem(productId)
      }

      // Roles (in order of "appearance")
      private val warehouse          = company
      private val customerDepartment = company
      private val paymentGateway     = company
      private val companyAccount     = company
      private val cart               = Order(customer)

      role warehouse {
        def has(productId: Int) = warehouse.stock.isDefinedAt(productId)
        def get(productId: Int) = warehouse.stock(productId)
        def shipProducts = {
          customer.owns ++= cart.items
          cart.items.foreach(i => warehouse.stock.remove(i._1))
          true // dummy delivery confirmation
        }
      }

      role customerDepartment {
        def calculateEligibleDiscountFactor = if (customer.isGoldMember) 0.5 else 1
      }

      role customer {
        def withdrawFunds(amountToPay: Int) { customer.cash -= amountToPay }
        def receiveProducts(products: Seq[(Int, Product)]) { customer.owns ++= products }
        def isGoldMember = customerDepartment.goldMembers.contains(customer)
      }

      role cart {
        def addItem(productId: Int, product: Product) {
          cart.items.put(productId, product)
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
        def confirmSufficientFunds = customer.cash >= cart.total
        def initiateOrderPayment = {
          val amount = cart.total
          customer.withdrawFunds(amount)
          companyAccount.depositFunds(amount)
          true // dummy transaction success
        }
      }

      role companyAccount {
        def depositFunds(amount: Int) { self.cash += amount }
      }
    }


    // Test various scenarios.
    // (copy and paste of ShoppingCart4 tests with trigger method name changes)

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
}