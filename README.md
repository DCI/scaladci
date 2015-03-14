# Scala DCI

###Enabling true object oriented programming in Scala

The [Data Context Interaction (DCI)](http://en.wikipedia.org/wiki/Data,_context_and_interaction) 
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
This is a primitive data class only knowing about its own data and how to manipulate that. 
The concept of a transfer between two accounts we can leave outside its responsibilities and instead 
delegate to a "Context" - the MoneyTransfer Context class. In this way we can keep the Account class 
very slim and avoid that it gradually takes on more and more responsibilities for each use case 
it participates in.

Our Mental Model of a money transfer could be "Withdraw amount from a source account and deposit the 
amount in a destination account". Interacting concepts like our "source"
and "destination" accounts we call "Roles" in DCI. And we can define how they can interact in our
Context to accomplish a money transfer:
```Scala
@context
class MoneyTransfer(source: Account, destination: Account, amount: Int) {

  source.withdraw // Interactions start...

  role source {
    def withdraw() {
      source.decreaseBalance(amount)
      destination.deposit
    }
  }

  role destination {
    def deposit() {
      destination.increaseBalance(amount)
    }
  }
}
```

We want our source code to map as closely to our mental model as possible so that we can confidently and easily
overview and reason about _how the objects will interact at runtime_! We want to expect no surprises at runtime. 
With DCI we have all runtime interactions right there! No need to look through endless convoluted abstractions, 
tiers, polymorphism etc to answer the reasonable question _where is it actually happening, goddammit?!_

At compile time, our @context macro annotation transforms the abstract syntax tree (AST) of our code to enable our
_runtime data objects_ to "have" those extra Role Methods. Well, I'm half lying to you; the 
objects won't "get new methods". Instead we call Role-name prefixed Role methods that are
lifted into Context scope which accomplishes what we intended in our source code. Our code gets transformed as 
though we had written this:

```Scala
class MoneyTransfer(source: Account, destination: Account, amount: Int) {
  
  source_withdraw()
  
  private def source_withdraw() {
    source.decreaseBalance(amount) // Calling Data instance method
    destination_deposit()          // Calling Role method
  }
  
  private def destination_deposit() {
    destination.increaseBalance(amount)
  }
}
```

## "Overriding" instance methods
In some cases we want to override the instance methods of a domain object with a Role method:
```Scala
  role source {
    def withdraw {
      source.decreaseBalance(amount)
      destination.deposit
    }
    
    // Role method with same signature as instance method
    def decreaseBalance(amount: Int) { 
      source.balance -= amount * 3
    }
  }

  role destination {
    def deposit {
      destination.increaseBalance(amount)
    }
  }
```
which will now turn into
```Scala
  private def source_withdraw() {
    source_decreaseBalance(amount) // Notice the underscore - the Role method will be called
    destination_deposit()
  }
  
  private def source_decreaseBalance() {
    source.balance -= amount * 3
  }
  
  private def destination_deposit() {
    destination.increaseBalance(amount)
  }
```
Role methods will always take precedence over instance methods when they have the same signature.

## `self` reference to a Role Player
As an alternative to using the Role name to reference a Role Player we can also use `self`:
```Scala
  role source {
    def withdraw {
      self.decreaseBalance(amount)  
      destination.deposit
    }
  }

  role destination {
    def deposit {
      self.increaseBalance(amount)
    }
  }
```
Using `self` doesn't change how Role methods take precedence over instance methods.


## Multiple roles
We can "assign" or "bind" a domain object to several Roles in our Context by simply making
more variables with role names pointing to that object:
```Scala
@context
class MyContext(someRole: MyData) {
  val otherRole = someRole
  val localRole = new DummyData()
  
  someRole.foo() // prints "Hello world"
  
  role someRole {
    def foo() {
      someRole.doMyDataStuff()
      otherRole.bar()
    }
  }
  
  role otherRole {
    def bar() {
      localRole.say("Hello")
    }
  }
  
  role localRole {
    def say(s: String) {
      println(s + " world")
    }
  }
}
```
As you see in line 3, otherRole is simply a reference pointing to the MyData instance (named someRole).

Inside each role definition we can still use `self`.

We can add as many references/role definitions as we want. This is a way to 
allow different Roles of a Use Case each to have their own meaningful namespace for defining their 
role-specific behavior / role methods.

## How does it work?
In order to have an intuitive syntax like

```scala
role roleName {
  // role methods...
}
```

for defining a Role and its Role methods we need to make a Scala
contruct that is valid before our macro annotation can start transforming our code:

```scala
object role extends Dynamic {
  def applyDynamic(obj: Any)(roleBody: => Unit) = roleBody
}
```

Since the `role` object extends the `Dynamic` marker trait and we have defined an
 `applyDynamic` method, we can invoke methods with arbitrary method names on the
 `role` object. When the compiler find that we are trying to call a method on
  `role` that we haven't defined (it doesn't type check), it will rewrite our code 
  so that it calls `applyDynamic`:
 
```scala
role.foo(args) ~~> role.applyDynamic("foo")(args)
role.bar(args) ~~> role.applyDynamic("bar")(args)
```

For the purpose of DCI we can presume to call a method on `role` that "happens"
to have a Role name:

```scala
role.source(args)      ~~> role.applyDynamic("source")(args)
role.destination(args) ~~> role.applyDynamic("destination")(args)
```

Scala allow us to replace the `.` with a space and the parentheses with curly
braces:

```scala
role source {args}      ~~> role.applyDynamic("source")(args)
role destination {args} ~~> role.applyDynamic("destination")(args)
```

You see where we're getting at. Now, the `args` signature in our
`applyDynamic` method has a "by-name" parameter type of `=> Unit` that
 allow us to define a block of code that returns nothing:
 
```scala
role source {
  doThis
  doThat
}      
~~> role.applyDynamic("source")(doThis; doThat) // pseudo code
```

The observant reader will note that "source" given the Dynamic invocation
capability is merely a "free text" name that has no connection to the object 
that we have called "source":

```scala
val source = new Account(...) // `source` is an object identifier
role source {...}             // "source" is a method name
```

In order to enforce that the method name "source" points to the object `source`
our `@context` macro annotation checks that the method name has a corresponding 
identifier in the scope of the annotated Context. If it doesn't it won't compile 
and the programmer will be noticed of available identifier names (one could have 
misspelled the Role name for instance).

If one prefers, the old "method syntax" can still be used:

```scala
role(source) {...} // `source` is an object identifier
```
This has the advantage of being inferred by the IDE but at the same time being
less DCI ideomatic since it looks less like a role definition than a method call 
which is not the intention and result after source code transformation.


## Scala DCI demo application

In the [Scala DCI Demo App](https://github.com/DCI/scaladci-demo) you can see an example of
how to create a DCI project.


## Using Scala DCI in your project

ScalaDCI is available for Scala 2.11.6 at [Sonatype](https://oss.sonatype.org/content/repositories/releases/org/scaladci/scaladci_2.11/0.5.2/).
To start coding with DCI in Scala add the following to your SBT build file:

    libraryDependencies ++= Seq(
      "org.scaladci" %% "scaladci" % "0.5.2"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)


## Building Scala DCI
```
git clone https://github.com/DCI/scaladci.git
<open in your IDE>
```


Have fun!

Marc Grue<br>
March 2015

### DCI resources
Discussions - [Object-composition](https://groups.google.com/forum/?fromgroups#!forum/object-composition)<br>
Website - [Full-OO](http://fulloo.info)<br>
Wiki - [DCI wiki](http://en.wikipedia.org/wiki/Data,_Context,_and_Interaction)

### Credits
Trygve Renskaug and James O. Coplien for inventing and developing DCI.

Scala DCI solution inspired by<br>
- Risto Välimäki's [post](https://groups.google.com/d/msg/object-composition/ulYGsCaJ0Mg/rF9wt1TV_MIJ) and<br>
- Rune Funch's [Marvin](http://fulloo.info/Examples/Marvin/Introduction/) DCI language 
and [Maroon](http://runefs.com/2013/02/14/using-moby-to-do-injectionless-dci-in-ruby/) for Ruby.
