# DCI with Scala macros?

An exploration of doing [DCI](http://en.wikipedia.org/wiki/Data,_context_and_interaction) Role
 assignment in runtime with [Scala macros](http://scalamacros.org/).

*WORK IN PROGRESS!!*

See discussions on the DCI list
[object-composition](https://groups.google.com/forum/?fromgroups#!forum/object-composition)

## Dynamic trait mixin

With traits we can create a RolePlayer:


```scala
val sourceAccount = new Account with Source
```

But what if we already have an account object?

```scala
val account = new Account
val sourceAccount = account with Source // Doesn't compile
```

We can't normally change an instantiated object at runtime in Scala. But with a macro
(or a compiler plugin) we can inspect and modify the Abstract Syntax Tree (AST)
created by the Scala compiler - before it's "finalized". This allows us to do this:

```scala
val account = new Account
val sourceAccount = account.as[Source]
```

The Account class doesn't have an `as[ROLE]` method. So when the account object tries
 to call this, an implicit conversion method within the Context instantiates a new `Binder`
object and passes in the account object. The `as[ROLE]` method of `Binder` now calls the
 `bind` macro that does the actual job of creating an `account with Source`. Here we go:
 
```
trait Context {
  implicit protected def obj2binder[OBJ](obj: OBJ) = new Binder(obj)

  protected class Binder[OBJ](val obj: OBJ) {
    def as[ROLE] = macro Context.bind[OBJ, ROLE]
  }
}
```
And the macro signature:

```
def bind[OBJ: c.WeakTypeTag, ROLE: c.WeakTypeTag](c: MacroContext): c.Expr[ROLEPLAYER] = {...}
```
or simplified:

```
def bind[OBJ, ROLE](c: MacroContext): ROLEPLAYER = {...}
``` 
The macro get's the account object via the MacroContext that "knows the surroundings", namely the
`Binder` class that carries the account object.
 
Now the macro can start doing the magic. It creates a new anonymous class extending
`Account with Source` with the data from the account
 object. Scala classes and case classes can be used for Data objects to play Roles. Eugene 
 Burmako from the Scala core team describes 
 [how it technically works](http://stackoverflow.com/questions/10373318/mixing-in-a-trait-dynamically) 
 in his example that inspired the approach taken here. Also check out the 
 [generated code](https://gist.github.com/2559714#L76) that his solution produces.
 
## Compound objects and re-binding

When we assign an object of type "Self" (like Account) to a Role (like Source) we would like to
be able to later assign it to
another Role. So we need to know what the Self type is in order to use that for the next Role 
binding.

When the object is instantiated from one class, it's easy to know what the Self
part is since there are only two classes in play:

```
Account                          <-- 1. Self
-------
 SELF                                Binding...

Account with Source              <-- 2. Role Player (from Self)
-------      ------
 SELF         ROLE                   Re-binding...

Account with Destination         <-- 3. Role Player (from previous Self)
-------      -----------
 SELF       (other) ROLE
```
But when the Self type is extending one or more traits, we need to somehow distinguish which
traits belong to the compound Self type. A simple way to do that is to add a "marker" trait
in between the compound Self type and the Role:

```
Account with Log                                       <-- 1. Compound Self
----------------   
 compound SELF                                             Binding....
                                                          
Account with Log with PlayingRole with Source          <-- 2. Role Player (from compound Self)
----------------      -----------      ------
 compound SELF           marker         ROLE               Re-binding...

Account with Log with PlayingRole with Destination     <-- 3. Role Player (from previous compound Self)
----------------      -----------      -----------
 compound SELF           marker       (other) ROLE
```
This allow us to deconstruct the Role Player in order to return the Self type without any 
Role capabilities so that we can build a new Role Player with a new Role. I'm not sure yet
if this approach creates some identity implications though...


## Enforcing runtime constraints at compile time

`OurContext extends scaladci.Context` in order to get access to the bind macro. When this is 
called with the `obj.as[ROLE]` syntax, it's like the time is put on hold and we can look into
`OurContext` source code as though we were using reflection at runtime. 
We can walk through every
 line of code in `OurContext` and examine every constraint that we want to enforce and simply
 abort the compilation process if we find that one of our constraints have been violated. 
 
 So far, the following constraints are enforced:
 
- Role identifiers are private to the Context
- Role traits are private to the Context (Role Players can't escape Context - Data objects can)
- Role assignment is not allowed within Role traits (Context keeps that responsibility)
- State in Role traits is not allowed
- Data objects can only be manipulated through self reference inside Roles (to be improved, though..)
  

*It's still to decide what mandatory/optional DCI constraints belongs to this list!* 

Some other (and probably more!) constraints to consider:

- An object can't play more than one Role at any time (how would be the best way to enforce that?)
- Contexts can stack?
- more...

### Object equality and other aspects to be examined...

One question that I'm sure needs attention too is the matter of object identity. When a RolePlayer is
created from an object and a Role trait, the data of the object is used to initialize the new object.
In Scala terms the original object and the new RolePlayer will be structurally equal (like equals 
in java - _not_ referentially equal):

```scala
account == sourceAccount       // structural equality is true
account equals sourceAccount   // structural equality is true
!(account eq sourceAccount)    // referential equality is false !! 
account ne sourceAccount       // referential equality is false !! 
```

The consequences of this and many more aspects needs careful examination. Some initial tests 
do some of this examine on a unit level, and more could probably help diagnose areas in need
of improvement/re-thinking.


## Examples

Here's the good old Money Transfer example. Notice how the Context receives already instantiated
Account objects and then assigns those to the Source/Destination roles.
```scala
case class Account(var acc: String, var balance: Int) {
  def increaseBalance(amount: Int) { balance = balance + amount }
  def decreaseBalance(amount: Int) { balance = balance - amount }
}

class MoneyTransfer(acc1: Account, acc2: Account) extends Context {
  private val source      = acc1.as[Source]                     // <-- Role binding
  private val destination = acc2.as[Destination]                // <-- Role binding

  def transfer(amount: Int) {                                   // <-- Only public member of Context
    source.withdraw(amount)                                     // <-- Trigger method
  }

  private trait Source {self: Account =>                        // <-- Role trait with self type Account
    def withdraw(amount: Int) {                                 // <-- Role method
      print(s"Source      ($acc): $balance - $amount = ")
      decreaseBalance(amount)                                   // <-- Role calls method on self (Account)
      println(balance)
      destination.deposit(amount)                               // <-- Interaction between Roles
    }
  }

  private trait Destination {self: Account =>
    def deposit(amount: Int) {
      print(s"Destination ($acc): $balance + $amount = ")
      increaseBalance(amount)
      println(balance)
    }
  }
}

object MoneyTransferTest extends App {                          // <-- Environment (could be some MVC part)
  val salary = new Account("Salary", 3000)                      // <-- Data object instantiation
  val budget = new Account("Budget", 1000)                      //     (could come from database)
  new MoneyTransfer(salary, budget) transfer 800                // <-- Use case execution
}
```
prints
```
Source      (Salary): 3000 - 800 = 2200
Destination (Budget): 1000 + 800 = 1800
```

## Run the code

```
git clone https://github.com/DCI/scaladci.git
cd scaladci
./sbt
gen-idea    <-- builds IntelliJ project files and resolves dependencies
```
- Open [IntelliJ](http://www.jetbrains.com/idea/nextversion/index.html) ( don't have eclipse solution, sorry)
- Open project (point to the scaladci folder that you cloned to)
- Rebuild project
- Run tests or examples

**Any comments or pull requests are welcome!**

## Resources

#### DCI
- [Object-composition](https://groups.google.com/forum/?fromgroups#!forum/object-composition)
- [Full-OO](http://fulloo.info)
- [DCI wiki](http://en.wikipedia.org/wiki/Data,_Context,_and_Interaction)

#### Scala macro ressources
- [Scala macros](http://scalamacros.org) - Official Scala macro site (by Eugene Burmako)
- [Macrocosmos](https://github.com/retronym/macrocosm) - exploring macros (by Jason Zaugg)
- [Scala macro tests]() - Tests of all macro capabilities. Search for terms that you want 
to explore: `grep -rin "SomeTerm" .`

#### Other Scala macro based projects
- [sqltyped](https://github.com/jonifreeman/sqltyped) - Macro which infers Scala types from database
- [ScalaMock](https://github.com/paulbutcher/ScalaMock) - Native Scala mocking
- [Expecty](https://github.com/pniederw/expecty) - Power Assertions for Scala
- [Slick](https://github.com/slick/slick) - Database query and access library for Scala
- [akmacros](http://www.akshaal.info/search/label/macro) - General purpose macros for Scala


## Appendix - Roles and Collaborations in Scala
[Michael Pradel](http://mp.binaervarianz.de) wrote an interesting thesis in 2008 about
Collaboration between Roles and objects. It's not DCI but describes 
many of the same concepts and concerns involved, so I highly recommend to have a look at:
  
- [pdf-presentation](http://mp.binaervarianz.de/scala_roles_diploma_thesis_slides.pdf) - Slides overview
- [Scala Roles](http://mp.binaervarianz.de/icsoft2008.pdf) - 8-page article by Michael Pradel and Martin Odersky
- [Full thesis](http://mp.binaervarianz.de/scala_roles_diploma_thesis.pdf)
- [code](http://mp.binaervarianz.de/scala_roles/scala_roles.tar.gz)

I tried to apply his solutions to DCI and got it to kind of work. But macros seems like 
a stronger solution since it can check runtime constraints at compile time.