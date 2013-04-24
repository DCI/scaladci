# Injectionless DCI in Scala

Scala [type macros](http://docs.scala-lang.org/overviews/macros/typemacros.html) 
allow us to let Data objects play Roles in a 
[DCI](http://en.wikipedia.org/wiki/Data,_context_and_interaction) Context by transforming
the abstract syntax tree (AST) of the Context body at compile time.

After transformation, methods of a Role become available to the Data object which 
now becomes a Role Player in DCI terms. 

We can reference the Role Player with the "self" keyword inside Role methods thus giving 
access to both instance methods (of the original Data object) and other methods of the Role 
it plays. Role methods will take precedence over instance methods when they have the same signature.

We can define Roles and their methods like this:
```scala
class MyContext(SomeRole: MyData) extends Context {
  val OtherRole = new OtherData()
  
  SomeRole.foo() // prints "Hello world"
  
  role(SomeRole) {
    def foo() {
      self.doMyDataStuff() // or SomeRole.doMyDataStuff()
      OtherRole.bar()
    }
  }
  
  role(OtherRole) {
    def bar() {
      println("Hello world")
    }
  }
}
```
After AST transformation, the Role methods are prefixed with the Role names
and lifted into the Context namespace:

```scala
class MyContext(SomeRole: MyData) extends Context {
  val OtherRole = new OtherData()
  
  SomeRole_foo()
  
  private def SomeRole_foo() {
    SomeRole.doMyDataStuff() 
    OtherRole_bar()
  }
  
  private def OtherRole_bar() {
    println("Hello world")
  }
}
```
Comments in the code base explains in more detail how the transformation works.

Solution inspired by Risto Välimäki's 
[post](https://groups.google.com/d/msg/object-composition/ulYGsCaJ0Mg/rF9wt1TV_MIJ)
and the 
[Marvin](http://fulloo.info/Examples/Marvin/Introduction/)
DCI language by Rune Funch. 

Have fun!

Marc Grue<br>
April 2013


## Try it out
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
- ~/.sbt/0.12.2-RC1/boot/org.scala-lang.macro-paradise.scala-2.11.0-SNAPSHOT/

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
