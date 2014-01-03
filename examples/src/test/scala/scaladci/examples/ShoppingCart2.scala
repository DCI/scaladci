package scaladci
package examples
import org.specs2.mutable.Specification

/*
Shopping cart example (version 2) - containing implicit logic leaking out

UC now with primary user actions and secondary system responses - still naming
system responsibilities with Role names...

Added "DesiredProduct" role to see if preventing passing around the product id
of the marked product makes sense.

New trigger method names prevent implicit logic leaking out of the Context.

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
1. Customer marks Desired Product in Shop
    - Shop reserves product in Warehouse
    - Shop adds Item to Cart (can repeat from step 1)
    - Shop shows updated contents of Cart to Customer
2. Customer requests to review Order
    - Shop presents Cart with Items and prices to Customer
3. Customer pays Order
    - System confirms sufficient funds are available
    - System initiates transfer of funds
    - System informs warehouse to ship products
    - Shop confirms purchase to Customer

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
===========================================================================
*/

// (Same Data model as ShoppingCart1)

class ShoppingCart2 extends Specification {
  import ShoppingCartModel._

  @context
  class PlaceOrder(Shop: Company, Customer: Person) {

    // Trigger methods matching the main success scenario steps
    def customerMarksDesiredProductInShop(productId: Int): Option[Product] = {
      DesiredProduct = productId
      Customer.markDesiredProductInShop
    }
    def customerRequestsToReviewOrder: Seq[(Int, Product)] = Customer.reviewOrder
    def customerPaysOrder: Boolean = Customer.payOrder
    def customerRemovesProductFromCart(productId: Int): Option[Product] =
      Customer.removeProductFromCart(productId)

    // Roles
    private var DesiredProduct: Int = _
    private val Warehouse           = Shop
    private val Cart                = Order(Customer)

    role Customer {
      def markDesiredProductInShop = Shop.addProductToOrder
      def reviewOrder = Cart.getItems
      def removeProductFromCart(productId: Int) = Cart.removeItem(productId: Int)
      def payOrder = Shop.processOrder
      def availableFunds = Customer.cash
      def withDrawFunds(amountToPay: Int) { Customer.cash -= amountToPay }
    }

    role Shop {
      def addProductToOrder: Option[Product] = {
        if (!Warehouse.hasDesiredProduct)
          return None
        val product = Warehouse.reserveDesiredProduct
        val discountedPrice = Shop.discountPriceOf(product)
        val desiredProduct = product.copy(price = discountedPrice)
        Cart.customerMarksDesiredProductInShop(DesiredProduct, desiredProduct)
        Some(desiredProduct)
      }
      def processOrder: Boolean = {
        val orderTotal = Cart.total
        if (orderTotal > Customer.availableFunds)
          return false

        Customer.withDrawFunds(orderTotal)
        Shop.depositFunds(orderTotal)

        Customer.owns ++= Cart.items
        Cart.items foreach (Warehouse.stock remove _._1)
        true
      }
      def discountPriceOf(product: Product) = {
        val customerIsGoldMember = Shop.goldMembers contains Customer
        val discountFactor = if (customerIsGoldMember) goldMemberReduction else 1
        (product.price * discountFactor).toInt
      }
      def goldMemberReduction = 0.5
      def customerIsGoldMember = Shop.goldMembers contains Customer
      def depositFunds(amount: Int) { Shop.cash += amount }
    }

    role Warehouse {
      def hasDesiredProduct = Shop.stock.isDefinedAt(DesiredProduct)
      def reserveDesiredProduct = Shop.stock(DesiredProduct) // dummy reservation
    }

    role Cart {
      def customerMarksDesiredProductInShop(productId: Int, product: Product) {
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


  /*
    Test various scenarios.

    Basically copy and paste of ShoppingCart1 test with some
    trigger method names and UC steps changed/removed.

    Note how the more expressive trigger method names make
    commenting less needed.
  */

  "Main success scenario" in new shoppingCart {

    // Initial status (same for all tests...)
    shop.stock === Map(wax, tires, bmw)
    shop.cash === 100000
    customer.cash === 20000
    customer.owns === Map()

    val order = new PlaceOrder(shop, customer)

    // Customer wants wax and tires
    order.customerMarksDesiredProductInShop(p1)
    order.customerMarksDesiredProductInShop(p2)

    order.customerRequestsToReviewOrder === Seq(wax, tires)

    val orderCompleted = order.customerPaysOrder === true

    shop.stock === Map(bmw)
    shop.cash === 100000 + 40 + 600
    customer.cash === 20000 - 40 - 600
    customer.owns === Map(wax, tires)
  }

  "Product out of stock" in new shoppingCart {

    // Wax out of stock
    shop.stock.remove(p1)
    shop.stock === Map(tires, bmw)

    val order = new PlaceOrder(shop, customer)

    // Customer wants wax
    val itemAdded = order.customerMarksDesiredProductInShop(p1) === None
    order.customerRequestsToReviewOrder === Seq()

    order.customerMarksDesiredProductInShop(p2)

    val orderCompleted = order.customerPaysOrder === true

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

    order.customerMarksDesiredProductInShop(p1)

    val discountedWax = 1 -> Product("Wax", (40 * 0.5).toInt)
    order.customerRequestsToReviewOrder === Seq(discountedWax)

    val orderCompleted = order.customerPaysOrder === true

    shop.stock === Map(tires, bmw)
    shop.cash === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(discountedWax)
  }

  "Customer has too low credit" in new shoppingCart {

    val order = new PlaceOrder(shop, customer)

    // Customer wants a BMW
    val itemAdded = order.customerMarksDesiredProductInShop(p3)

    // Any product is added - shop doesn't yet know if customer can afford it
    itemAdded === Some(bmw._2)
    order.customerRequestsToReviewOrder === Seq(bmw)

    // Customer tries to pay order
    val paymentStatus = order.customerPaysOrder

    // Shop informs Customer of too low credit
    paymentStatus === false

    // Customer removes unaffordable BMW from cart
    order.customerRemovesProductFromCart(p3)

    // Customer aborts shopping and no purchases are made
    shop.stock === Map(wax, tires, bmw)
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
    val tiresItemAdded = order.customerMarksDesiredProductInShop(p2)

    // Product out of stock!
    shop.stock.contains(p2) === false

    // Nothing added to order yet
    tiresItemAdded === None
    order.customerRequestsToReviewOrder === Seq()

    // Let's buy the BMW instead. As a gold member that should be possible!
    val bmwItemAdded = order.customerMarksDesiredProductInShop(p3)

    // Discounted BMW is added to order
    val discountedBMW = Product("BMW", (50000 * 0.5).toInt)
    bmwItemAdded === Some(discountedBMW)
    order.customerRequestsToReviewOrder === Seq(p3 -> discountedBMW)

    // Ouch! We couldn't afford it.
    val paymentAttempt1 = order.customerPaysOrder === false

    // It's still 5000 too much for us, even with the membership discount
    discountedBMW.price - customer.cash === 5000

    // Ok, no new car today
    order.customerRemovesProductFromCart(p3)

    // Order is back to empty
    order.customerRequestsToReviewOrder === Seq()

    // Let's get some wax anyway...
    val waxItemAdded = order.customerMarksDesiredProductInShop(p1)

    // Did we get our membership discount on this one?
    val discountedWax = Product("Wax", (40 * 0.5).toInt)
    waxItemAdded === Some(discountedWax)

    // Now we can afford it!
    val paymentAttempt2 = order.customerPaysOrder === true

    // Not much shopping done Today. At least we got some cheap wax.
    shop.stock === Map(bmw)
    shop.cash === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(p1 -> discountedWax)
  }
}