# Scala DCI demo app

####Minimal project setup for doing DCI in Scala

_See [Scala DCI](https://github.com/DCI/scaladci) for more info on [DCI](http://en.wikipedia.org/wiki/Data,_context_and_interaction) in general and the Scala DCI macro._

To run the demo app simply:
```
git clone https://github.com/DCI/scaladci.git
cd scaladci/demo
sbt
gen-idea // (if you use IntelliJ)
```
Open the created project in Intellij or your favorite IDE and run the
[MoneyTransferApp](https://github.com/DCI/scaladci/blob/master/demo/src/main/scala/MoneyTransferApp.scala)
to see DCI in action.

### DCI Context creation workflow

All you need to create a DCI context and define Roles is to:

  1. Create a new Scala file
  2. Import scaladci._
  2. Annotate a class with @context
  3. Define roles names (like Actors in a use case)
  4. Define role methods (what will those actors do...)
  5. Define the trigger that starts off the chain of interactions between the roles

And you're ready to instantiate the Context and run the use case...

### Building your own app with DCI

You can use this demo app as a template for your own project and just add more dependencies
and content as you go. In the
[SBT build file](https://github.com/DCI/scaladci/blob/master/demo/project/build.scala)
you can see what the minimum requirement is to enable DCI in your own app.
It's basically those two lines:

```scala
"org.scaladci" % "scaladci_2.11.0" % "0.5.1"
...
addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full)
```

#### Multi project app
If your application has more than one project, you'll have to modify your directory structure
compared to this demo app so that it doesn't have `src` at the root level but rather a folder
for each of you projects in your application. As you see in the
root level of [scaladci](https://github.com/DCI/scaladci).