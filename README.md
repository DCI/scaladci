# Injectionless DCI in Scala

Scala [type macros](http://docs.scala-lang.org/overviews/macros/typemacros.html) allows 
an already instantiated object to play a Role (call role methods) in a 
[DCI](http://en.wikipedia.org/wiki/Data,_context_and_interaction) Context in a type safe and 
injectionless way:



```scala
class MyContext(SomeRole: MyData) extends Context {
  val OtherRole = new OtherData()
  
  SomeRole.foo() // prints "Hello world"
  
  role(SomeRole) {
    def foo() {
      SomeRole.doMyDataStuff() 
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
After AST transformation, the role methods are prefixed with the role names
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
Comments in the code base explains in more detail how it works.

Solution inspired by Risto Välimäki's 
[post](https://groups.google.com/d/msg/object-composition/ulYGsCaJ0Mg/rF9wt1TV_MIJ)
and the 
[Marvin](http://fulloo.info/Examples/Marvin/Introduction/)
DCI language by Rune Funch. 

Have fun!

Marc Grue (2013-01-20)


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

- Type macros is still an experimental feature of Scala and is under heavy 
development (as of January 2013).
- Current method resolution during AST transformation is very simple for now and 
will need to be analyzed and refined according to DCI constraints. 
- _ This is NOT production ready code yet. _


#### Resources
DCI: 
[Object-composition](https://groups.google.com/forum/?fromgroups#!forum/object-composition),
[Full-OO](http://fulloo.info),
[DCI wiki](http://en.wikipedia.org/wiki/Data,_Context,_and_Interaction)<br>
Scala type macros:
[Type macros](http://docs.scala-lang.org/overviews/macros/typemacros.html), 
[Scala macros](http://scalamacros.org),
[ScalaMock](https://github.com/paulbutcher/ScalaMock)
