package scaladci
package examples
import scala.collection.mutable
import org.specs2.mutable.Specification
import org.specs2.specification._

/*
Shopping cart example, version 1

Implementing a simple Place Order use case of an online shopping cart to explore
how we could handle various execution paths (scenarios) within a single DCI Context.

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
1. Customer marks desired Product in Shop.
2. Shop adds Item to Cart (can repeat from step 1).
3. Customer requests to review Order.
4. Shop presents Cart with Items and prices to Customer.
5. Customer pays Order.
6. Shop confirms purchase to Customer.

Deviations
---------------------------------------------------------------------------
2a. Product is out of stock:
    1. Shop informs Customer that Product is out of stock.
    2. Go to step 1 (pick another product).

4a. Customer has gold membership:
    1. Shop presents Cart with Products and discounted prices to Customer.
    2. Go to step 5.

5a. Customer has too low credit:
    1. Customer removes unaffordable item(s) from Cart.
    2. Customer aborts order.
===========================================================================
*/

// 4 basic "dumb" Data types
object ShoppingCartModel {
  case class Product(name: String, price: Int)
  case class Person(name: String, var cash: Int, owns: mutable.Map[Int, Product] = mutable.Map())
  case class Company(name: String, var cash: Int, stock: mutable.Map[Int, Product], goldMembers: mutable.Set[Person])
  case class Order(customer: Person, items: mutable.Map[Int, Product] = mutable.Map())
}

// Setup for each test
trait shoppingCart extends Scope {
  import ShoppingCartModel._
  val (p1, p2, p3)      = (1, 2, 3)
  val (wax, tires, bmw) = (p1 -> Product("Wax", 40), p2 -> Product("Tires", 600), p3 -> Product("BMW", 50000))
  val shop              = Company("Don's Auto shop", 100000, mutable.Map(wax, tires, bmw), mutable.Set())
  val customer          = Person("Matthew", 20000)
}

// Define the Context and test various scenarios
class ShoppingCart1 extends Specification {
  import ShoppingCartModel._

  @context
  class PlaceOrder(Shop: Company, Customer: Person) {

    // Trigger methods
    def addItem(productId: Int): Option[Product] = Cart addItem productId
    def removeItem(productId: Int): Option[Product] = Cart removeItem productId
    def getCurrentItems = Cart.items.toIndexedSeq.sortBy(_._1)
    def pay = Customer.payOrder

    // Roles
    private val Warehouse = Shop
    private val Cart      = Order(Customer)

    role Customer {
      def payOrder: Boolean = {
        // Sufficient funds?
        val orderTotal = Cart.total
        if (orderTotal > Customer.cash)
          return false

        // Transfer ownership of items
        Customer.owns ++= Cart.items
        Cart.items foreach (Warehouse.stock remove _._1)

        // Resolve payment
        Customer.cash -= orderTotal
        Shop receivePayment orderTotal
        true
      }
      def isGoldMember = Shop.goldMembers contains Customer
      def reduction = if (Customer.isGoldMember) 0.5 else 1
    }

    role Shop {
      def receivePayment(amount: Int) { Shop.cash += amount }
    }

    role Warehouse {
      def has(productId: Int) = Shop.stock isDefinedAt productId
    }

    role Cart {
      def addItem(productId: Int): Option[Product] = {
        // In stock?
        if (!Warehouse.has(productId))
          return None

        // Gold member price?
        val product = Warehouse.stock(productId)
        val customerPrice = (product.price * Customer.reduction).toInt

        // Add item with adjusted price to Cart
        val revisedProduct = product.copy(price = customerPrice)
        Cart.items.put(productId, revisedProduct)
        Some(revisedProduct)
      }

      def removeItem(productId: Int): Option[Product] = {
        if (!Cart.items.isDefinedAt(productId))
          return None
        Cart.items.remove(productId)
      }

      def total = Cart.items.map(_._2.price).sum
    }
  }


  // Test various scenarios

  "Main success scenario" in new shoppingCart {

    // Initial status (same for all tests...)
    shop.stock === Map(tires, wax, bmw)
    shop.cash === 100000
    customer.cash === 20000
    customer.owns === Map()

    // hm... when is order created? When customer selects first product?
    val order = new PlaceOrder(shop, customer)

    // Step 1: Customer selects product(s) in UI
    // Step 2: 2 items added to cart (step 1-2 repeated)
    order.addItem(p1)
    order.addItem(p2)

    // Step 3: Customer requests to review order
    // Step 4: Shop presents items in cart:
    order.getCurrentItems === Seq(wax, tires)

    // Step 5: Customer requests to pay order
    val orderCompleted = order.pay

    // Step 6: Order completed?
    orderCompleted === true

    // Outcome
    shop.stock === Map(bmw)
    shop.cash === 100000 + 40 + 600
    customer.cash === 20000 - 40 - 600
    customer.owns === Map(tires, wax)
  }

  "Deviation 2a - Product out of stock" in new shoppingCart {

    // Wax is out of stock!
    shop.stock.remove(p1)
    shop.stock === Map(tires, bmw)

    val order = new PlaceOrder(shop, customer)

    // Customer wants wax
    val itemAdded = order.addItem(p1)

    // 2a. Product out of stock!
    shop.stock.contains(p1) === false

    // 2a.1. Shop informs Customer that Product is out of stock.
    itemAdded === None
    order.getCurrentItems === Seq()

    // 2a.2. Customer picks tires instead
    order.addItem(p2)

    // Order completed
    val orderCompleted = order.pay === true

    // Outcome
    shop.stock === Map(bmw)
    shop.cash === 100000 + 600
    customer.cash === 20000 - 600
    customer.owns === Map(tires)
  }

  "Deviation 4a - Customer has gold membership" in new shoppingCart {

    // Customer is gold member
    shop.goldMembers.add(customer)

    val order = new PlaceOrder(shop, customer)

    // Customer orders wax
    order.addItem(p1)

    // 4a. Customer has gold membership
    shop.goldMembers.contains(customer) === true

    // 4a.1. Shop presents Cart with wax at discounted price
    val discountedWax = 1 -> Product("Wax", (40 * 0.5).toInt)
    order.getCurrentItems === Seq(discountedWax)

    // Order completed
    val orderCompleted = order.pay === true

    // Outcome
    shop.stock === Map(tires, bmw)
    shop.cash === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(discountedWax)
  }

  "Deviation 5a - Customer has too low credit" in new shoppingCart {

    val order = new PlaceOrder(shop, customer)

    // Customer wants a BMW
    val itemAdded = order.addItem(p3)

    // Any product is added - shop doesn't yet know if customer can afford it
    itemAdded === Some(bmw._2)
    order.getCurrentItems === Seq(bmw)

    // 5. Customer tries to pay order
    val paymentStatus = order.pay

    // 5a. Shop informs Customer of too low credit
    paymentStatus === false

    // 5a.1. Customer removes unaffordable BMW from cart
    order.removeItem(p3)

    // Customer aborts shopping
    shop.stock === Map(tires, wax, bmw)
    shop.cash === 100000
    customer.cash === 20000
    customer.owns === Map()
  }

  "All deviations in play" in new shoppingCart {

    // Tires are out of stock
    shop.stock.remove(p2)
    shop.stock === Map(wax, bmw)

    // We have a gold member
    shop.goldMembers.add(customer)

    val order = new PlaceOrder(shop, customer)

    // Let's get some tires
    val tiresItemAdded = order.addItem(p2)

    // 2a. Product out of stock!
    shop.stock.contains(p2) === false

    // Nothing added to order yet
    tiresItemAdded === None
    order.getCurrentItems === Seq()

    // Let's buy the BMW instead. As a gold member that should be possible!
    val bmwItemAdded = order.addItem(p3)

    // Discounted BMW is added to order
    val discountedBMW = Product("BMW", (50000 * 0.5).toInt)
    bmwItemAdded === Some(discountedBMW)
    order.getCurrentItems === Seq(p3 -> discountedBMW)

    // Ouch! We couldn't afford it.
    val paymentAttempt1 = order.pay === false

    // It's still 5000 too much for us, even with the membership discount
    discountedBMW.price - customer.cash === 5000

    // Ok, no new car today
    order.removeItem(p3)

    // Order is back to empty
    order.getCurrentItems === Seq()

    // Let's get some wax anyway...
    val waxItemAdded = order.addItem(p1)

    // Did we get our membership discount on this one?
    val discountedWax = Product("Wax", (40 * 0.5).toInt)
    waxItemAdded === Some(discountedWax)

    // Now we can afford it!
    val paymentAttempt2 = order.pay === true

    // Not much shopping done Today. At least we got some cheap wax.
    shop.stock === Map(bmw)
    shop.cash === 100000 + 20
    customer.cash === 20000 - 20
    customer.owns === Map(p1 -> discountedWax)
  }
}