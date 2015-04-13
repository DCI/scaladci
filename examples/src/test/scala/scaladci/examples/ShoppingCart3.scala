package scaladci
package examples
import org.specs2.mutable.Specification

/*
Shopping cart example (version 3) - removing shop role

Delegated most of the earlier shop role responsibilities to the customer role. We can
view the shop as the system as a whole (maybe a context?) that we operate in - or the
system that responds to various customer actions rather than a Role on its own.

Now distinguishing between System and UI as different kinds of responses to the user
inputs. When an item is checked with the warehouse we could see it as the System doing
some technical background checks that doesn't involve the customer whereas the UI is
what communicates with the customer.

"Internally" the systems regards the cart as an Order. The customer primarily thinks
"shopping cart" and might change to a mental model of "order" once he's about to pay?

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
    - System reserves Product in Warehouse
    - System adds Item to Order (can repeat from step 1)
    - UI shows updated contents of Cart to Customer
2. Customer requests to review Order
    - UI presents Cart with Items and prices to Customer
3. Customer pays Order
    - System confirms sufficient funds are available
    - System initiates transfer of funds
    - System informs Warehouse to ship Products
    - UI confirms purchase to Customer

Deviations
---------------------------------------------------------------------------
1a. Product is out of stock:
    1. UI informs Customer that Product is out of stock.

1b. Customer has gold membership:
    1. System adds discounted Product to Order.

3a. Customer has insufficient funds to pay Order:
    1. UI informs Customer of insufficient funds.
        a. Customer removes unaffordable Item(s) from Cart:
            1. Go to step 3.
===========================================================================
*/

class ShoppingCart3 extends Specification {
  import ShoppingCartModel._

  {
    @context
    class PlaceOrder(shop: Company, customer: Person) {

      // UC steps
      def customerMarksdesiredProductInshop(productId: Int): Option[Product] = {
        desiredProductId = productId
        customer.markdesiredProductInshop
      }
      def customerRequestsToReviewOrder: Seq[(Int, Product)] =
        customer.reviewOrder
      def customerPaysOrder: Boolean =
        customer.payOrder

      // Deviation(s)
      def customerRemovesProductFromcart(productId: Int): Option[Product] =
        customer.removeProductFromcart(productId)

      // Context data house-keeping
      private var desiredProductId: Int = _

      // Roles
      private val warehouse = shop
      private val cart      = Order(customer)

      role customer {
        def markdesiredProductInshop: Option[Product] = {
          if (!warehouse.hasdesiredProduct)
            return None
          val product = warehouse.reservedesiredProduct
          val discountedPrice = customer.getMemberPriceOf(product)
          val desiredProduct = product.copy(price = discountedPrice)
          cart.addItem(desiredProductId, desiredProduct)
          Some(desiredProduct)
        }
        def reviewOrder = cart.getItems
        def removeProductFromcart(productId: Int) = cart.removeItem(productId: Int)
        def payOrder: Boolean = {
          val orderTotal = cart.total
          if (orderTotal > customer.cash)
            return false

          customer.cash -= orderTotal
          shop.cash += orderTotal

          customer.owns ++= cart.items
          cart.items foreach (warehouse.stock remove _._1)
          true
        }

        def getMemberPriceOf(product: Product) = {
          val customerIsGoldMember = shop.goldMembers.contains(customer)
          val goldMemberReduction = 0.5
          val discountFactor = if (customerIsGoldMember) goldMemberReduction else 1
          (product.price * discountFactor).toInt
        }
      }

      role warehouse {
        def hasdesiredProduct = shop.stock.isDefinedAt(desiredProductId)
        def reservedesiredProduct = shop.stock(desiredProductId) // dummy reservation
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
    }


    // Test various scenarios.
    // (copy and paste of ShoppingCart2 tests)

    "Main success scenario" in new ShoppingCart {

      // Initial status (same for all tests...)
      shop.stock === Map(tires, wax, bmw)
      shop.cash === 100000
      customer.cash === 20000
      customer.owns === Map()

      val order = new PlaceOrder(shop, customer)

      // customer wants wax and tires
      order.customerMarksdesiredProductInshop(p1)
      order.customerMarksdesiredProductInshop(p2)

      order.customerRequestsToReviewOrder === Seq(wax, tires)

      val orderCompleted = order.customerPaysOrder === true

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
      val itemAdded = order.customerMarksdesiredProductInshop(p1) === None
      order.customerRequestsToReviewOrder === Seq()

      order.customerMarksdesiredProductInshop(p2)

      val orderCompleted = order.customerPaysOrder === true

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

      order.customerMarksdesiredProductInshop(p1)

      val discountedWax = 1 -> Product("Wax", (40 * 0.5).toInt)
      order.customerRequestsToReviewOrder === Seq(discountedWax)

      val orderCompleted = order.customerPaysOrder === true

      shop.stock === Map(tires, bmw)
      shop.cash === 100000 + 20
      customer.cash === 20000 - 20
      customer.owns === Map(discountedWax)
    }

    "customer has too low credit" in new ShoppingCart {

      val order = new PlaceOrder(shop, customer)

      // customer wants a BMW
      val itemAdded = order.customerMarksdesiredProductInshop(p3)

      // Any product is added - shop doesn't yet know if customer can afford it
      itemAdded === Some(bmw._2)
      order.customerRequestsToReviewOrder === Seq(bmw)

      // customer tries to pay order
      val paymentStatus = order.customerPaysOrder

      // shop informs customer of too low credit
      paymentStatus === false

      // customer removes unaffordable BMW from cart
      order.customerRemovesProductFromcart(p3)

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
      val tiresItemAdded = order.customerMarksdesiredProductInshop(p2)

      // Product out of stock!
      shop.stock.contains(p2) === false

      // Nothing added to order yet
      tiresItemAdded === None
      order.customerRequestsToReviewOrder === Seq()

      // Let's buy the BMW instead. As a gold member that should be possible!
      val bmwItemAdded = order.customerMarksdesiredProductInshop(p3)

      // Discounted BMW is added to order
      val discountedBMW = Product("BMW", (50000 * 0.5).toInt)
      bmwItemAdded === Some(discountedBMW)
      order.customerRequestsToReviewOrder === Seq(p3 -> discountedBMW)

      // Ouch! We couldn't afford it.
      val paymentAttempt1 = order.customerPaysOrder === false

      // It's still 5000 too much for us, even with the membership discount
      discountedBMW.price - customer.cash === 5000

      // Ok, no new car today
      order.customerRemovesProductFromcart(p3)

      // Order is back to empty
      order.customerRequestsToReviewOrder === Seq()

      // Let's get some wax anyway...
      val waxItemAdded = order.customerMarksdesiredProductInshop(p1)

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
}