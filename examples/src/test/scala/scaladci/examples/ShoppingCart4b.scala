package scaladci
package examples
import org.specs2.mutable.Specification

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

class ShoppingCart4b extends Specification {
  import ShoppingCartModel._

  @context
  class PlaceOrder(Shop: Company, Customer: Person) {
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
    role Customer {
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
        Cart.items foreach (Shop.stock remove _._1)
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


  // Test various scenarios.
  // (copy and paste of ShoppingCart2/3/4 tests with trigger method names changed)

  "Main success scenario" in new shoppingCart {

    // Initial status (same for all tests...)
    shop.stock === Map(tires, wax, bmw)
    shop.cash === 100000
    customer.cash === 20000
    customer.owns === Map()

    val order = new PlaceOrder(shop, customer)

    // Customer wants wax and tires
    order.customerSelectedDesiredProduct(p1)
    order.customerSelectedDesiredProduct(p2)

    order.customerRequestedToReviewOrder === Seq(wax, tires)

    val orderCompleted = order.customerRequestedToPayOrder === true

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
    val itemAdded = order.customerSelectedDesiredProduct(p1) === None
    order.customerRequestedToReviewOrder === Seq()

    order.customerSelectedDesiredProduct(p2)

    val orderCompleted = order.customerRequestedToPayOrder === true

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

    order.customerSelectedDesiredProduct(p1)

    val discountedWax = 1 -> Product("Wax", (40 * 0.5).toInt)
    order.customerRequestedToReviewOrder === Seq(discountedWax)

    val orderCompleted = order.customerRequestedToPayOrder === true

    shop.stock === Map(tires, bmw)
    shop.cash === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(discountedWax)
  }

  "Customer has too low credit" in new shoppingCart {

    val order = new PlaceOrder(shop, customer)

    // Customer wants a BMW
    val itemAdded = order.customerSelectedDesiredProduct(p3)

    // Any product is added - shop doesn't yet know if customer can afford it
    itemAdded === Some(bmw._2)
    order.customerRequestedToReviewOrder === Seq(bmw)

    // Customer tries to pay order
    val paymentStatus = order.customerRequestedToPayOrder

    // Shop informs Customer of too low credit
    paymentStatus === false

    // Customer removes unaffordable BMW from cart
    order.customerRemovedProductFromCart(p3)

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
    val tiresItemAdded = order.customerSelectedDesiredProduct(p2)

    // Product out of stock!
    shop.stock.contains(p2) === false

    // Nothing added to order yet
    tiresItemAdded === None
    order.customerRequestedToReviewOrder === Seq()

    // Let's buy the BMW instead. As a gold member that should be possible!
    val bmwItemAdded = order.customerSelectedDesiredProduct(p3)

    // Discounted BMW is added to order
    val discountedBMW = Product("BMW", (50000 * 0.5).toInt)
    bmwItemAdded === Some(discountedBMW)
    order.customerRequestedToReviewOrder === Seq(p3 -> discountedBMW)

    // Ouch! We couldn't afford it.
    val paymentAttempt1 = order.customerRequestedToPayOrder === false

    // It's still 5000 too much for us, even with the membership discount
    discountedBMW.price - customer.cash === 5000

    // Ok, no new car today
    order.customerRemovedProductFromCart(p3)

    // Order is back to empty
    order.customerRequestedToReviewOrder === Seq()

    // Let's get some wax anyway...
    val waxItemAdded = order.customerSelectedDesiredProduct(p1)

    // Did we get our membership discount on this one?
    val discountedWax = Product("Wax", (40 * 0.5).toInt)
    waxItemAdded === Some(discountedWax)

    // Now we can afford it!
    val paymentAttempt2 = order.customerRequestedToPayOrder === true

    // Not much shopping done Today. At least we got some cheap wax.
    shop.stock === Map(bmw)
    shop.cash === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(p1 -> discountedWax)
  }
}