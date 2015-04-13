package scaladci
package examples
import org.specs2.mutable.Specification

/*
Shopping cart example (version 2) - containing implicit logic leaking out

UC now with primary user actions and secondary system responses - still naming
system responsibilities with Role names...

Added "desiredProduct" role to see if preventing passing around the product id
of the marked product makes sense.

New trigger method names prevent implicit logic leaking out of the Context.

See discussion at:
https://groups.google.com/forum/?fromgroups=#!topic/object-composition/JJiLWBsZWu0

===========================================================================
USE CASE:	Place Order [user-goal]

Person browsing around finds product(s) in a web shop that he/she wants to buy.

Primary actor.. Web Customer ("Customer")
Scope.......... Web Shop ("Shop")
Preconditions.. Shop presents Product(s) to Customer
Trigger........ Customer wants to buy certain Product(s)

Main Success Scenario
---------------------------------------------------------------------------
1. Customer marks Desired Product in Shop
    - Shop reserves Product in warehouse
    - Shop adds Item to Cart (can repeat from step 1)
    - Shop shows updated contents of Cart to Customer
2. Customer requests to review Order
    - Shop presents Cart with Items and prices to Customer
3. Customer pays Order
    - System confirms sufficient funds are available
    - System initiates transfer of funds
    - System informs Warehouse to ship Products
    - Shop confirms purchase to Customer

Deviations
---------------------------------------------------------------------------
1a. Product is out of stock:
    1. Shop informs customer that Product is out of stock.

1b. Customer has gold membership:
    1. Shop adds discounted product to Cart.

3a. Customer has insufficient funds to pay Order:
    1. Shop informs Customer of insufficient funds on credit card.
        a. Customer removes unaffordable Item(s) from Cart:
            1. Go to step 3.
===========================================================================
*/

// (Same Data model as ShoppingCart1)

class ShoppingCart2 extends Specification {
  import ShoppingCartModel._

  {
    @context
    class PlaceOrder(company: Company, person: Person) {

      // Context house-keeping
      private var desiredProductId: Int = _

      // Trigger methods matching the main success scenario steps
      def customerMarksDesiredProductInshop(productId: Int): Option[Product] = {
        desiredProductId = productId
        customer.markDesiredProductInshop
      }
      def customerRequestsToReviewOrder: Seq[(Int, Product)] = customer.reviewOrder
      def customerPaysOrder: Boolean = customer.payOrder
      def customerRemovesProductFromCart(productId: Int): Option[Product] =
        customer.removeProductFromCart(productId)


      // Roles
      private val customer  = person
      private val shop      = company
      private val warehouse = company
      private val cart      = Order(customer)

      role customer {
        def markDesiredProductInshop = shop.addProductToOrder
        def reviewOrder = cart.getItems
        def removeProductFromCart(productId: Int) = cart.removeItem(productId: Int)
        def payOrder = shop.processOrder
        def availableFunds = customer.cash
        def withDrawFunds(amountToPay: Int) { customer.cash -= amountToPay }
      }

      role shop {
        def addProductToOrder: Option[Product] = {
          if (!warehouse.hasDesiredProduct)
            return None
          val product = warehouse.reserveDesiredProduct
          val discountedPrice = shop.discountPriceOf(product)
          val desiredProduct = product.copy(price = discountedPrice)
          cart.customerMarksDesiredProductInshop(desiredProductId, desiredProduct)
          Some(desiredProduct)
        }
        def processOrder: Boolean = {
          val orderTotal = cart.total
          if (orderTotal > customer.availableFunds)
            return false

          customer.withDrawFunds(orderTotal)
          shop.depositFunds(orderTotal)

          customer.owns ++= cart.items
          cart.items foreach (warehouse.stock remove _._1)
          true
        }
        def discountPriceOf(product: Product) = {
          val customerIsGoldMember = shop.goldMembers contains customer
          val discountFactor = if (customerIsGoldMember) goldMemberReduction else 1
          (product.price * discountFactor).toInt
        }
        def goldMemberReduction = 0.5
        def customerIsGoldMember = shop.goldMembers contains customer
        def depositFunds(amount: Int) { shop.cash += amount }
      }

      role warehouse {
        def hasDesiredProduct = shop.stock.isDefinedAt(desiredProductId)
        def reserveDesiredProduct = shop.stock(desiredProductId) // dummy reservation
      }

      role cart {
        def customerMarksDesiredProductInshop(productId: Int, product: Product) {
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


    /*
    Test various scenarios.

    Basically copy and paste of ShoppingCart1 test with some
    trigger method names and UC steps changed/removed.

    Note how the more expressive trigger method names make
    commenting less needed.
  */

    "Main success scenario" in new ShoppingCart {

      // Initial status (same for all tests...)
      shop.stock === Map(tires, wax, bmw)
      shop.cash === 100000
      customer.cash === 20000
      customer.owns === Map()

      val order = new PlaceOrder(shop, customer)

      // customer wants wax and tires
      order.customerMarksDesiredProductInshop(p1)
      order.customerMarksDesiredProductInshop(p2)

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
      val itemAdded = order.customerMarksDesiredProductInshop(p1) === None
      order.customerRequestsToReviewOrder === Seq()

      order.customerMarksDesiredProductInshop(p2)

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

      order.customerMarksDesiredProductInshop(p1)

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
      val itemAdded = order.customerMarksDesiredProductInshop(p3)

      // Any product is added - shop doesn't yet know if customer can afford it
      itemAdded === Some(bmw._2)
      order.customerRequestsToReviewOrder === Seq(bmw)

      // customer tries to pay order
      val paymentStatus = order.customerPaysOrder

      // shop informs customer of too low credit
      paymentStatus === false

      // customer removes unaffordable BMW from cart
      order.customerRemovesProductFromCart(p3)

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
      val tiresItemAdded = order.customerMarksDesiredProductInshop(p2)

      // Product out of stock!
      shop.stock.contains(p2) === false

      // Nothing added to order yet
      tiresItemAdded === None
      order.customerRequestsToReviewOrder === Seq()

      // Let's buy the BMW instead. As a gold member that should be possible!
      val bmwItemAdded = order.customerMarksDesiredProductInshop(p3)

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
      val waxItemAdded = order.customerMarksDesiredProductInshop(p1)

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