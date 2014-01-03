# Pure DCI in Scala (version 0.4)

_Using Scala 2.10.3 with macro-paradise plugin._

The [Data, Context and Interaction (DCI)](http://en.wikipedia.org/wiki/Data,_context_and_interaction) 
paradigm by Trygve Reenskaug and James Coplien embodies true object-orientation where
runtime Interactions between a network of objects in a particular Context 
is understood _and_ coded as first class citizens.

Let's take a simple Data class Account with some basic methods:
```scala
case class Account(name: String, var balance: Int) {
  def increaseBalance(amount: Int) { balance += amount }
  def decreaseBalance(amount: Int) { balance -= amount }
}
```
This is what we in DCI call a "dumb" data class. It only "knows" about its own data and how
to manipulate that. The concept of a transfer between two accounts is outside of its
responsibilities and we delegate this to a Context - the MoneyTransfer context class. 
In this way we can keep the Account class very slim and avoid that it gradually takes on
more and more responsibilities for each use case it participates in.

In a Money Transfer use case we can imagine a "Source" account where we take the money from
and a "Destination" account where we put the money. That could be our intuitive "Mental model" 
of the transfer process. We code the Source and Destination concepts as Roles in the
Context:
```Scala
@context
class MoneyTransfer(Source: Account, Destination: Account, amount: Int) {

  Source.withdraw // Interactions start...

  role Source {
    def withdraw() {
      Source.decreaseBalance(amount)  
      Destination.deposit
    }
  }

  role Destination {
    def deposit() {
      Destination.increaseBalance(amount)
    }
  }
}
```
Our @context macro annotation transforms the abstract syntaxt tree (AST) of the context 
class at compile time by prefixing role methods with role names and lifting those
methods into the context namespace. This is what we get after compilation has transformed our 
MoneyTransfer context:
```Scala
class MoneyTransfer(Source: Account, Destination: Account, amount: Int) {
  
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

## "Overriding" instance methods
In some cases we want to override the instance methods of a domain object with a Role method:
```Scala
  role Source {
    def withdraw {
      Source.decreaseBalance(amount)
      Destination.deposit
    }
    
    // Role method with same signature as instance method
    def decreaseBalance(amount: Int) { 
      Source.balance -= amount * 3
    }
  }

  role Destination {
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

## `self` and `this` references to Role Players
As an alternative to using the Role name to reference a Role Player we can also use `self` or `this`.

If we use `self` our Role definitions would look like this:
```Scala
  role Source {
    def withdraw {
      self.decreaseBalance(amount)  
      Destination.deposit
    }
  }

  role Destination {
    def deposit {
      self.increaseBalance(amount)
    }
  }
```

If we choose, we can also use `this` as a reference inside Role methods to refer to the Role Player.
This is not Scala-idiomatic though since `this` would normally point to the Context instance!
We have allowed this special DCI-idiomatic meaning of `this` since we want to be able to think of
"this role" (or "this Role Player") while we define our Role methods:
```Scala
  role Source {
    def withdraw {
      this.decreaseBalance(amount)  
      Destination.deposit
    }
  }

  role Destination {
    def deposit {
      this.increaseBalance(amount)
    }
  }
```
Using `self` or `this` doesn't change how Role methods take precedence over instance methods.


## Multiple roles
We can "assign" or "bind" a domain object to several Roles in our Context by simply making
more variables with Role names pointing to that object:
```Scala
@context
class MyContext(SomeRole: MyData) {
  val OtherRole = SomeRole
  val LocalRole = new DummyData()
  
  SomeRole.foo() // prints "Hello world"
  
  role SomeRole {
    def foo() {
      SomeRole.doMyDataStuff()
      OtherRole.bar()
    }
  }
  
  role OtherRole {
    def bar() {
      LocalRole.say("Hello")
    }
  }
  
  role LocalRole {
    def say(s: String) {
      println(s + " world")
    }
  }
}
```
As you see in line 3, OtherRole is simply a reference pointing to the MyData instance (named SomeRole). 

Inside each role definition we can still use `self` and `this`.

We can add as many references/role definitions as we want. This is a way to 
allow different Roles of a Use Case each to have their own meaningful namespace for defining their 
role-specific behavior / role methods.

## Try it
```
git clone https://github.com/DCI/scaladci.git
cd scaladci
sbt
gen-idea // (if you use IntelliJ)
```

*NOTE: To use scaladci in your own project, you need to place it in a separate 
sbt-project and let your own project depend on the scaladci project. This will 
allow the macro transformation to take place in a separate compilation run. 
Please have a look at the [build file of scaladci](
https://github.com/DCI/scaladci/blob/master/project/build.scala) to see how 
this is set up.*

Solution inspired by Risto Välimäki's 
[post](https://groups.google.com/d/msg/object-composition/ulYGsCaJ0Mg/rF9wt1TV_MIJ)
and the 
[Marvin](http://fulloo.info/Examples/Marvin/Introduction/)
DCI language by Rune Funch. 

Have fun!

Marc Grue<br>
January 2014



#### Resources
DCI:
[Object-composition](https://groups.google.com/forum/?fromgroups#!forum/object-composition),
[Full-OO](http://fulloo.info),
[DCI wiki](http://en.wikipedia.org/wiki/Data,_Context,_and_Interaction)<br>
Scala:
[Macro annotations](http://docs.scala-lang.org/overviews/macros/annotations.html), 
[Macro paradise](http://docs.scala-lang.org/overviews/macros/paradise.html)