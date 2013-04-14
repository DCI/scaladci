package scaladci
package examples.shoppingcart2
import DCI._
import scala.collection.mutable

/*
UC now with primary user actions and secondary system responses - still naming
system responsibilities with Role names...

Added "DesiredProduct" role.

Any explicit/implicit logic now shouldn't leak out of the Context..?

See discussion at:
https://groups.google.com/forum/?fromgroups=#!topic/object-composition/JJiLWBsZWu0

===========================================================================
USE CASE 2:	Place Order [user-goal]

Person browsing around finds product(s) in a web shop that he/she wants to buy.

Primary actor.. Web customer ("Customer")
Scope.......... Web shop ("Shop")
Preconditions.. Shop presents product(s) to Customer
Trigger........ Customer wants to buy certain product(s)

Main Success Scenario
---------------------------------------------------------------------------
1. Customer marks Desired Product in Shop.
    - Shop reserves product in Warehouse
    - Shop adds Item to Cart (can repeat from step 1).
    - Shop shows updated contents of Cart to Customer
2. Customer requests to review Order.
    - Shop presents Cart with Items and prices to Customer.
3. Customer pays Order.
    - Shop confirms purchase to Customer.
    - Shop delivers Product(s) to Customer.

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
        b. Customer terminates order:
            2. Failure.
===========================================================================

Todo: how to prevent "wrong" invocation order?
*/

// Data
case class Product(val name: String, price: Int)
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

// Context
class PlaceOrder(Shop: Company, Customer: Person) extends Context {

  // Trigger methods
  def customerMarksDesiredProductInShop(productId: Int): Option[Product] = {
    DesiredProduct = productId
    Customer.markDesiredProductInShop
  }
  def customerRequestsToReviewOrder(): Seq[(Int, Product)] = Customer.reviewOrder
  def customerPaysOrder(): Boolean = Customer.payOrder
  def customerRemovesProductFromCart(productId: Int): Option[Product] =
    Customer.removeProductFromCart(productId)

  // Roles
  private var DesiredProduct: Int = _
  private val Warehouse = Shop
  private val Cart = Order(Customer)

  role(Customer) {
    def markDesiredProductInShop = Shop.addProductToOrder
    def reviewOrder = Cart.getItems
    def removeProductFromCart(productId: Int) = Cart.removeItem(productId: Int)
    def payOrder = Shop.processOrder

    def availableFunds = Customer.cash
    def withDrawFunds(amountToPay: Int) { Customer.cash -= amountToPay }

    def receiveProducts() { Customer.owns ++= Cart.items }
  }

  role(Shop) {
    def addProductToOrder(): Option[Product] = {
      if (!Warehouse.hasDesiredProduct)
        return None
      val product = Warehouse.reserveDesiredProduct
      val discountedPrice = Shop.discountPriceOf(product)
      val desiredProduct = product.copy(price = discountedPrice)
      Cart.addItem(DesiredProduct, desiredProduct)
      Some(desiredProduct)
    }
    def processOrder: Boolean = {
      val orderTotal = Cart.total
      if (orderTotal > Customer.availableFunds)
        return false

      Customer.withDrawFunds(orderTotal)
      Shop.depositFunds(orderTotal)

      Warehouse.shipProducts
      Customer.receiveProducts
      true
    }
    def discountPriceOf(product: Product) = {
      val customerIsGoldMember = Shop.goldMembers.contains(Customer)
      val discountFactor = if (customerIsGoldMember) goldMemberReduction else 1
      (product.price * discountFactor).toInt
    }
    def goldMemberReduction = 0.5
    def customerIsGoldMember = Shop.goldMembers.contains(Customer)
    def depositFunds(amount: Int) { Shop.cash += amount }
  }

  role(Warehouse) {
    def hasDesiredProduct = Shop.stock.isDefinedAt(DesiredProduct)
    def reserveDesiredProduct = Shop.stock(DesiredProduct) // dummy reservation
    def shipProducts() { Cart.items.foreach(i => Warehouse.stock.remove(i._1)) }
  }

  role(Cart) {
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
      s"\n- Shop cash : ${shop.cash}" +
      s"\n- Shop stock: \n${shop.stock.toIndexedSeq.sortBy(_._1).mkString("\n")}\n"
    )
  }
  reset()
  showResult("Initial data:")

  // Various scenarios
  {
    println("\n######## Main success scenario ####################")
    println(s"Step 1: Customer selects product(s) in UI")
    val desiredProduct = 1
    val placeOrder = new PlaceOrder(shop, customer)
    placeOrder.customerMarksDesiredProductInShop(desiredProduct)
    val otherDesiredProduct = 2
    placeOrder.customerMarksDesiredProductInShop(otherDesiredProduct)
    println(s"Step 2: 2 items added to cart (step 1-2 repeated)")
    println(s"Step 3: Customer requests to review order")
    println(s"Step 4: Shop presents items in cart: \n" +
      placeOrder.customerRequestsToReviewOrder.mkString("\n"))
    println(s"Step 5: Customer requests to pay order")
    val paymentStatus = placeOrder.customerPaysOrder
    println(s"Step 6: Order completed? $paymentStatus\n")
    showResult("Customer bought wax for 40:")
  }
  {
    reset()
    println("\n######## Scenario A ###############################")
    val desiredProduct = 1
    shop.stock.remove(desiredProduct)
    val placeOrder = new PlaceOrder(shop, customer)
    val itemAdded = placeOrder.customerMarksDesiredProductInShop(desiredProduct)
    if (itemAdded == None)
      println(s"@@ Deviation 2a: Product $desiredProduct out of stock!\n")
    val otherDesiredProduct = 2
    placeOrder.customerMarksDesiredProductInShop(otherDesiredProduct)
    placeOrder.customerPaysOrder
    showResult("Customer bought tires instead for 600:")
  }
  {
    reset()
    println("\n######## Scenario B ###############################")
    val wax = 1
    shop.goldMembers.add(customer)
    val placeOrder = new PlaceOrder(shop, customer)
    placeOrder.customerMarksDesiredProductInShop(wax)
    println(s"@@ Deviation 4a: Customer is gold member\n")
    placeOrder.customerPaysOrder
    showResult("Customer has paid half price of 20:")
  }
  {
    reset()
    println("\n######## Scenario C ###############################")
    val desiredProduct = 3
    val placeOrder = new PlaceOrder(shop, customer)
    val itemAdded = placeOrder.customerMarksDesiredProductInShop(desiredProduct)
    val paymentCompleted = placeOrder.customerPaysOrder
    if (!paymentCompleted)
      println(s"@@ Deviation 5a: Insufficient funds for ${itemAdded.get.name}\n")
    showResult("Order placement aborted:")
  }

  // All deviations and trigger methods in play...
  {
    reset()
    println("\n######## Scenario ABC #############################")
    // Tires out of stock
    val tires = 2
    shop.stock.remove(tires)

    // We have a gold member
    shop.goldMembers.add(customer)
    val placeOrder = new PlaceOrder(shop, customer)

    // Trying to buy tires
    val itemAdded = placeOrder.customerMarksDesiredProductInShop(tires)
    if (itemAdded == None)
      println(s"@@ Deviation 2a: Product $tires out of stock!")

    // Let's buy the BMW instead then - as a gold member that should be possible!
    val BMW = 3
    val newItemAdded = placeOrder.customerMarksDesiredProductInShop(BMW)
    println(s"@@ Deviation 4a: Customer is gold member")
    val paymentCompleted = placeOrder.customerPaysOrder

    // Ouch - still too expensive
    if (!paymentCompleted)
      println(s"@@ Deviation 5a: ${customer.cash} not enough to buy  " +
        s"${newItemAdded.get.name} (${newItemAdded.get.price} needed)")

    // Ok, no new car today
    placeOrder.customerRemovesProductFromCart(BMW)
    println(s"@@ Deviation 5a.1.a: Remove unaffordable item from cart\n" +
      placeOrder.customerRequestsToReviewOrder.mkString("\n") + "\n")

    // Let's get some wax anyway...
    placeOrder.customerMarksDesiredProductInShop(1)

    // Now we can afford it
    placeOrder.customerPaysOrder

    showResult("Gold customer can't get out-of-stock tires, neither " +
      "too expensive BMW (even with gold discount). Ok, some wax then:")
  }
}