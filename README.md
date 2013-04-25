# DCI with Scala

Scala [type macros](http://docs.scala-lang.org/overviews/macros/typemacros.html) 
allow us to let Data objects play Roles in a 
[DCI](http://en.wikipedia.org/wiki/Data,_context_and_interaction) Context by transforming
the abstract syntax tree (AST) of the Context body at compile time.

Let's take a simple Data class Account with some basic instance methods:
```scala
case class Account(name: String, var balance: Int) {
  def increaseBalance(amount: Int) { balance += amount }
  def decreaseBalance(amount: Int) { balance -= amount }
}
```
We can then code a Money Transfer use case by giving some incoming Account objects names 
that correspond to our mental model of our use case. In this case we imagine a Source and a Destination 
account, and thus give the incoming variables in the MoneyTransfer Context those names. 

We add behavior to our Data objects by defining some Role methods:
```Scala
class MoneyTransfer(Source: Account, Destination: Account, amount: Int) extends Context {

  Source.withdraw // Use case enactment

  role(Source) {
    def withdraw {
      Source.decreaseBalance(amount)  
      Destination.deposit
    }
  }

  role(Destination) {
    def deposit {
      Destination.increaseBalance(amount)
    }
  }
}
```
After AST transformation by the type macro, Role methods are prefixed with their corresponding Role names
and lifted into the Context namespace:
```Scala
class MoneyTransfer(Source: Account, Destination: Account, amount: Int) extends Context {
  
  Source_withdraw()
  
  private def Source_withdraw() {
    Source.decreaseBalance(amount) // Calling Data instance method
    Destination_deposit()          // Calling Role method
  }
  
  private def Destination_deposit() {
    Destination.increaseBalance(amount)
  }
}
```
As you see there is no injection of methods into the objects.

## "Overriding" instance methods
We can "override" Data instance methods with Role methods:
```Scala
  role(Source) {
    def withdraw {
      Source.decreaseBalance(amount)
      Destination.deposit
    }
    
    // Role method with same signature as Data instance method
    def decreaseBalance(amount: Int) { 
      Source.balance -= amount * 3
    }
  }

  role(Destination) {
    def deposit {
      Destination.increaseBalance(amount)
    }
  }
```
which will now turn into
```Scala
  private def Source_withdraw() {
    Source_decreaseBalance(amount) // Notice the underscore - the Role method will be called
    Destination_deposit()
  }
  
  private def Source_decreaseBalance() {
    Source.balance -= amount * 3
  }
  
  private def Destination_deposit() {
    Destination.increaseBalance(amount)
  }
```
Role methods will always take precedence over instance methods when they have the same signature.

## "self" and "this" references to Role Players
As an alternative to using the Role name to reference a Role Player we can also use "self" or "this".

If we use "self" our Role definitions would look like this:
```Scala
  role(Source) {
    def withdraw {
      self.decreaseBalance(amount)  
      Destination.deposit
    }
  }

  role(Destination) {
    def deposit {
      self.increaseBalance(amount)
    }
  }
```

If we choose, we can also use "this" as a reference inside Role methods to refer to the Role Player.
This is not Scala-idiomatic though since "this" would normally point to the Context instance! 
We have allowed this special DCI-idiomatic meaning of "this" since we want to be able to think of 
"this role" (or "this Role Player") while we define our Role methods:
```Scala
  role(Source) {
    def withdraw {
      this.decreaseBalance(amount)  
      Destination.deposit
    }
  }

  role(Destination) {
    def deposit {
      this.increaseBalance(amount)
    }
  }
```
"self" and "this" doesn't change how Role methods take precedence over instance methods.


## Multiple roles
We can "assign" or "bind" a data object to several Roles in our Context by simply making
more variables with Role names pointing to that object:
```Scala
class MyContext(SomeRole: MyData) extends Context {
  val OtherRole = SomeRole
  val LocalRole = new DummyData()
  
  SomeRole.foo() // prints "Hello world"
  
  role(SomeRole) {
    def foo() {
      SomeRole.doMyDataStuff()
      OtherRole.bar()
    }
  }
  
  role(OtherRole) {
    def bar() {
      LocalRole.say("Hello")
    }
  }
  
  role(LocalRole) {
    def say(s: String) {
      println(s + " world")
    }
  }
}
```
As you see, OtherRole is simply a reference pointing to the MyData instance (named SomeRole). 

Inside each role definition we can still use "self" and "this".

We can add as many references/role definitions as we want. This is a way to 
allow different Roles of a Use Case each to have their own meaningful namespace for defining their 
role-specific behavior / role methods.

## Try it
```
git clone https://github.com/DCI/scaladci.git
cd scaladci
./sbt
gen-idea
```
If the project won't build, it can be because you have another Scala snapshot version in your central
sbt directory that Intellij uses to compile. You can try to empty the following directories and
then build again:

- ~/.ivy2/cache/org.scala-lang.macro-paradise/
- ~/.sbt/0.12.3/boot/org.scala-lang.macro-paradise.scala-2.11.0-SNAPSHOT/

Solution inspired by Risto Välimäki's 
[post](https://groups.google.com/d/msg/object-composition/ulYGsCaJ0Mg/rF9wt1TV_MIJ)
and the 
[Marvin](http://fulloo.info/Examples/Marvin/Introduction/)
DCI language by Rune Funch. 

Have fun!

Marc Grue<br>
April 2013


####Disclaimer

- Type macros are still an experimental feature of Scala
- This type macro might not cover all uses - please send a note if this is the case!


#### Resources
DCI: 
[Object-composition](https://groups.google.com/forum/?fromgroups#!forum/object-composition),
[Full-OO](http://fulloo.info),
[DCI wiki](http://en.wikipedia.org/wiki/Data,_Context,_and_Interaction)<br>
Scala type macros:
[Type macros](http://docs.scala-lang.org/overviews/macros/typemacros.html), 
[Scala macros](http://scalamacros.org),
[ScalaMock](https://github.com/paulbutcher/ScalaMock)
