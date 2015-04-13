package scaladci
package semantics
import org.specs2.mutable._

import scaladci.util._

/*
Method shadowing
- when a role method and instance method have the same name (not necessarily signature!)

If method shadowing occurs, a role method will always take precedence over an instance method.

Compile time shadowing
It doesn't make sense to declare a role contract expecting objects having some method
and at the same time make a role method with that name since the latter will always precede.

So we try to defer the role contract type at compile time in order to detect conflicting
method names.


Then the role myRole wouldn't
know which of the methods to choose. We call it "explicit shadowing" when we can determine
(at compile time) that a role contract collides with a role method. In that case we let
the compiler throw an error, and you then have 2 choices:

1. Rename your role method, or
2. Change your role contract

Runtime shadowing
If at runtime an object turns out to have a method with the same name as a role method,
and we couldn't detect such "implicit shadowing" at compile time, then the role method
will always take precedence.
*/


class MethodShadowing extends Specification {

  class Obj {
    def foo = "FOO"
    val bar = "BAR"
    type cat = Int
  }
  case class Obj0() {
    def foo = "FOO"
  }
  case class Obj1(i: Int) {
    def foo = "FOO"
  }
  class Sub extends Obj {
    def sub = "SUB"
  }

  "Explicit" >> {

    "Context parameter binding various kinds" >> {

      expectCompileError(
        """
          @context
          case class Context(myRole: Obj) {
            role myRole {
              def foo = "foo" // <-- shadows instance method Obj.foo
            }
          }
        """
        , "Role method name `foo` in `myRole` shadows `method foo` of `MethodShadowing.this.Obj`. " +
          "Please re-name role method `foo` or change the role contract.")


      expectCompileError(
        """
          @context
          case class Context(myRole: Obj) {
            role myRole {
              def bar = "bar" // <-- shadows instance property Obj.bar
            }
          }
        """
        , "Role method name `bar` in `myRole` shadows `value bar` of `MethodShadowing.this.Obj`. " +
          "Please re-name role method `bar` or change the role contract.")


      expectCompileError(
        """
          @context
          case class Context(myRole: Obj) {
            role myRole {
              def cat = "cat" // <-- shadows instance type Obj.cat
            }
          }
        """
        , "Role method name `cat` in `myRole` shadows `type cat` of `MethodShadowing.this.Obj`. " +
          "Please re-name role method `cat` or change the role contract.")


      expectCompileError(
        """
          @context
          case class Context(myRole: {def foo: String}) {
            role myRole {
              def foo = "foo" // <-- shadows instance structural type
            }
          }
        """
        , "Role method name `foo` in `myRole` shadows `method foo` of `AnyRef{def foo: String}`. " +
          "Please re-name role method `foo` or change the role contract.")

      success
    }


    "Recursive assignments checked" >> {

      // Getting type from context parameter
      // myRole <- obj1: Obj
      expectCompileError(
        """
          @context
          case class Context1(obj1: Obj) {
            val myRole = obj1
            role myRole {
              def foo = "FOO"
            }
          }
        """
        , "Role method name `foo` in `myRole` shadows `method foo` of `MethodShadowing.this.Obj`. " +
          "Please re-name role method `foo` or change the role contract.")

      // re-assignment
      // myRole <- obj2 <- obj1: Obj
      expectCompileError(
        """
          @context
          case class Context2(obj1: Obj) {
            val obj2 = obj1
            val myRole = obj2
            role myRole {
              def foo = "FOO"
            }
          }
        """
        , "Role method name `foo` in `myRole` shadows `method foo` of `MethodShadowing.this.Obj`. " +
          "Please re-name role method `foo` or change the role contract.")

      // double re-assignment (etc...)
      // myRole <- obj3  <- obj2 <- obj1: Obj
      expectCompileError(
        """
          @context
          case class Context3(obj1: Obj) {
            val obj2 = obj1
            val obj3 = obj2
            val myRole = obj3
            role myRole {
              def foo = "FOO"
            }
          }
        """
        , "Role method name `foo` in `myRole` shadows `method foo` of `MethodShadowing.this.Obj`. " +
          "Please re-name role method `foo` or change the role contract.")

      success
    }


//    "Case class instantiation" >> {
//
//      @context
//      class Context1 {
//        val myRole = new Obj0()
//        role myRole {
//          def foo = "FOO"
//        }
//      }
//
//      @context
//      class Context2 {
//        val myRole = Obj0()
//        role myRole {
//          def foo = "FOO"
//        }
//      }
//
//      @context
//      class Context3 {
//        val myRole = Obj1(42)
//        role myRole {
//          def foo = "FOO"
//        }
//      }
//
//
//
//      //
//      //      @context
//      //      case class Context4(obj: Obj) {
//      //        val obj3   = obj
//      //        //        val obj2 = obj3
//      //        val obj2   = new Obj
//      //        //        val myRole: Obj = obj2
//      //        //        val myRole = obj2
//      //        //        val myRole = new Obj
//      //        val myRole = obj2
//      //        //        val myRole = obj
//      //        def trigger = myRole.foo
//      //        role myRole {
//      //          def foo = "FOO"
//      //        }
//      //      }
//      //      Context2(new Obj).trigger === "bar"
//
//      //      expectCompileError(
//      //        """
//      //          @context
//      //          case class Context2(obj: Obj) {
//      //            val myRole = obj
//      //            role myRole {
//      //              def foo = "FOO"
//      //            }
//      //          }
//      //        """
//      //        , "Role method name `foo` in `myRole` shadows `method foo` of `MethodShadowing.this.Obj`. " +
//      //          "Please re-name role method `foo` or change the role contract.")
//      success
//    }



    "Structural type instantiation" >> {

      case class Data(i: Int) {
        def foo = "FOO"
      }

      object Repository {
        def get(i: Int) = new Data(i)
      }

//      @context
//      case class Context(id: Int) {
//        val myRole: Data = Repository.get(id) // <-- type hint
//        role myRole {
//          def foo = "foo"
//        }
//      }

//      @context
//      case class Context(myRole: {def foo: String}) {
//        role myRole {
//          def foo = "foo"
//        }
//      }
//
//      @context
//      case class Context1(obj:Obj) {
//        val myRole: {def foo: String} = obj
//        role myRole {
//          def foo = "foo"
//        }
//      }

//      @context
//      case class Context1(obj:Obj) {
//        val myRole = obj
//        role myRole {
//          type Contract = {def foo: String}
//          def foo = "foo"
//        }
//      }

//      @context
//      case class Context(myRole:Obj) {
//        role myRole {
//          type Contract = {def foo: String}
//          def foo = "foo"
//        }
//      }




//      @context
//      case class Context1(obj: Obj {def doh: String}) {
//        val myRole: {def foo: String} = obj
//        role myRole {
//          def doh = "foo"
//        }
//      }
//
//
//
//      @context
//      class Context4 {
//        val myRole: {def foo: String} = new Obj
//        role myRole {
//          type Contract = {def foo: String}
//          def foo = "foo"
//        }
//      }
//
//
//
//
//      @context
//      class Context2 {
//        val myRole: {def foo: String} = new Obj
//        role myRole {
//          def foo = "foo"
//        }
//      }


      //
      //      @context
      //      case class Context4(obj: Obj) {
      //        val obj3   = obj
      //        //        val obj2 = obj3
      //        val obj2   = new Obj
      //        //        val myRole: Obj = obj2
      //        //        val myRole = obj2
      //        //        val myRole = new Obj
      //        val myRole = obj2
      //        //        val myRole = obj
      //        def trigger = myRole.foo
      //        role myRole {
      //          def foo = "FOO"
      //        }
      //      }
      //      Context2(new Obj).trigger === "bar"

      //      expectCompileError(
      //        """
      //          @context
      //          case class Context2(obj: Obj) {
      //            val myRole = obj
      //            role myRole {
      //              def foo = "FOO"
      //            }
      //          }
      //        """
      //        , "Role method name `foo` in `myRole` shadows `method foo` of `MethodShadowing.this.Obj`. " +
      //          "Please re-name role method `foo` or change the role contract.")
      success
    }
  }

  //  trait Account
  //
  //  class CheckingAccount extends Account {
  //    def transferFrom(amount: Int) = "transferFrom"
  //  }
  //  class SpecialCheckingAccount extends CheckingAccount {
  //    def transferStart(amount: Int) = "transferStart"
  //  }
  //
  //  @context
  //  case class MoneyTransfer(source: Account) {
  //
  //    def transfer(amount: Int) = source.transferStart(amount)
  //
  //    role source {
  //      def transferStart(amount: Int) = "shadowing transferStart!"
  //    }
  //  }
  //
  //  // Runtime
  //  MoneyTransfer(new SpecialCheckingAccount).transfer(100) === "shadowing transferStart!"

  //
  //  class Data {
  //    def foo = "FOO"
  //  }
  //  class Sub extends Data {
  //    def baz = "BAZ"
  //  }
  //
  //  //  "Subtype in disguise" >> {
  //  //
  //  //    @context
  //  //    case class Context(myRole: Data) {
  //  //
  //  //      def trigger = myRole.baz
  //  //
  //  //      role myRole {
  //  //        def baz = "baz"
  //  //      }
  //  //    }
  //  //    // Runtime - myRole.baz is shadowing Sub.baz
  //  //    Context(new Sub).trigger === "baz"
  //  //  }
  //  class Hej
  //
  //  "Subtype in disguise" >> {
  //
  //    //    class Data {
  //    //      def foo = "FOO"
  //    //    }
  //
  //
  ////    class Data {
  ////      def foo = "FOO"
  ////    }
  ////    class Sub extends Data {
  ////      def baz = "BAZ"
  ////    }
  ////
  ////    @context
  ////    case class Context1(myRole: {def foo: String}) { // foo "explicitly intentional"
  ////
  ////      def trigger = myRole.foo
  ////
  ////      role myRole {
  ////        def foo = "foo"
  ////      }
  ////    }
  ////    // Disallowed
  ////    Context1(new Data).trigger === "foo"
  //
  //
  //    import scala.reflect.runtime.{universe => ru}
  //    def getTypeTagx[T: ru.TypeTag](obj: T) = ru.typeTag[T]
  //
  //    import scala.reflect.runtime.universe._
  //    def paramInfo[T: TypeTag](x: T) = {
  //      val targs = typeOf[T] match { case TypeRef(_, _, args) => args }
  //      println(s"type of $x has type arguments $targs")
  //      s"type of $x has type arguments $targs"
  //    }
  //
  ////    @context
  //    case class Context2(myRole: Data) { // foo "implicitly intentional"?
  //
  //      def trigger1 = myRole.foo
  ////      def trigger2 = myRole.baz
  //
  //  val xx = new Hej
  //
  //
  //
  //        def trigger2 {
  //          //          getTypeTag(xx) //.tpe.members
  ////          throw new RuntimeException(getTypeTagx(xx).tpe.members.toString)
  //          throw new RuntimeException(paramInfo(xx).toString)
  //        }
  ////      role myRole {
  //////        def foo = "foo"
  ////        def baz = "baz"
  ////      }
  //    }
  //    // Disallowed
  //    Context2(new Sub).trigger2
  ////    Context2(new Data).trigger2 === "foo"
  //
  //    // Not disallowed??
  ////    Context2(new Sub).trigger2 === "baz"
  //
  //    ok
  //  }

  //  implicit class test(any: Any) {
  //    def bar = "zzz"
  //  }

  //  class Data {
  //    def foo = "FOO"
  //    val xx = 7
  //  }
  //  class Sub extends Data {
  //    def bar = "BAR"
  //  }

  //  "hej" >> {
  //
  //    @context
  //    case class Context1(myRole: Data) {
  //
  //      def trigger = myRole.bar
  //
  //      role myRole {
  //        type Contract = {
  //          def foo
  //        }
  //        def bar = "bar" // <-- Sub.bar detected at runtime
  //      }
  //    }
  //    // Disallowed at runtime
  //    Context1(new Sub).trigger === "bar"
  //
  //
  //    //    @context
  //    //    case class Context2(myRole: Data) {
  //    //
  //    //      def trigger = myRole.bar
  //    //
  //    //      role myRole {
  //    //        def foo = "foo" // <-- Data.foo detected at compile-time
  //    //        def bar = "bar" // ok
  //    //      }
  //    //    }
  //    //    // Disallowed at compile-time
  //    //    Context2(new Data).trigger === "bar"
  //
  //
  //    //    @context
  //    //    case class Context1(myRole: Data) {
  //    //
  //    //      def trigger = myRole.foo + myRole.bar
  //    //
  //    //      role myRole {
  //    //        def bar = "bar"
  //    //      }
  //    //    }
  //    //    // bar is unique
  //    //    Context1(new Data).trigger === "FOObar"
  //    //
  //    //    // bar is shadowing - silently allowed
  //    //    Context1(new Sub).trigger === "FOObar"
  //    //
  //    //
  //    //
  //    //    @context
  //    //    case class Context1(myRole: Data) {
  //    //
  //    //      def trigger = myRole.foo
  //    //
  //    //      role myRole {
  //    //        def foo = "foo"
  //    //      }
  //    //    }
  //    //    // Compile: myRole.foo shadows Data.foo - explicitly disallow
  //    //    // Runtime: (won't compile)
  //    //    Context1(new Data).trigger === "I won't compile"
  //    //
  //    //
  //    //
  //    @context
  //    case class Context2(myRole: Data) {
  //      util.checkShadows("myRole", myRole, List("bar", "buz"))
  //
  //      def trigger = myRole.bar
  //
  //      role myRole {
  //        def bar = "bar"
  //      }
  //    }
  //    // Compile: we don't know if myRole.bar shadows - silently allow!
  //    // Runtime: myRole.bar shadows Sub.bar - myRole.bar wins
  //    //    Context2(new Sub).trigger === "bar"
  //    Context2(new Sub).trigger === "bar"
  //    //
  //    //    @context
  //    //    case class Context(myRole: Obj) {
  //    //
  //    //      def trigger = myRole_bar
  //    //
  //    //      private def myRole_bar = "bar"
  //    //    }
  //    //    val ctx = Context(Obj())
  //    //
  //    //    // Context execution.
  //    //    ctx.trigger === "bar"
  //    //
  //    //    // Role methods are never available outside the context
  //    //    // This won't compile:
  //    //    // ctx.myRole.bar === "bar"
  //    //    //    Error:(168, 17) value bar is not a member of RoleContract.this.Obj
  //    //    //         ctx.myRole.bar === ???
  //    //    //                    ^
  //    //
  //    //    // But we can still call _instance_ methods!
  //    //    ctx.myRole.foo === "FOO"
  //
  //
  //    //    class Data {
  //    //      def foo = "FOO"
  //    //      private def bar = 7
  //    //      private val baz = 7
  //    //    }
  //
  //    //    @context
  //    //    case class Context(myRole: Data) {
  //    //      util.checkShadows("myRole", myRole, List("bar", "buz"))
  //    //
  //    //      def trigger = myRole.foo
  //    //
  //    //      role myRole {
  //    //        @shadow
  //    //        def foo = "foo"
  //    //      }
  //    //    }
  //    //    Context(new Data).trigger === "foo"
  //
  //    //        def buz = "buz"
  //  }


  //  List(
  //    <caseaccessor> <paramaccessor> val myRole: Data = _,
  //    def <init>(myRole: Data) = {
  //      super.<init>();
  //      ()
  //    },
  //        util.checkShadows("myRole", myRole, List("baz", "buz")),
  //        def trigger = myRole_baz,
  //        private def myRole_baz = "baz",
  //        private def myRole_buz = "buz")
  //
  //  List(<caseaccessor> <paramaccessor> val myRole: Data = _, def <init>(myRole: Data) = {
  //    super.<init>();
  //    ()
  //  }, def trigger = myRole_baz, private def myRole_baz = "baz", private def myRole_buz = "buz")


  //  "hej" >> {
  //    //      util.checkShadows("myRole", myRole, List("baz", "buz"))
  //
  //    @context
  //    case class Context1(myRole: Data) {
  //      def trigger = myRole.foo
  //      role myRole {
  //        type Contract = {
  //          def foo: String
  //        }
  //        def foo = "FOO"
  //      }
  //    }
  //
  //    @context
  //    case class Context2(myRole: {def foo: String} ) {
  //      def trigger = myRole.foo
  //      role myRole {
  //        def foo = "FOO"
  //      }
  //    }
  //
  //    @context
  //    case class Context3(myRole: Data) {
  //      def trigger = myRole.foo
  //      role myRole {
  //        type Contract = Data
  //        def foo = "FOO"
  //      }
  //    }
  //
  //    @context
  //    case class Context4(myRole: Data) {
  //      def trigger = myRole.foo
  //      role myRole {
  //        def foo = "FOO"
  //      }
  //    }
  //
  //    @context
  //    case class Context5(obj: {def foo: String} ) {
  //      def trigger = myRole.foo
  //      val myRole = obj
  //      role myRole {
  //        def foo = "FOO"
  //      }
  //    }
  //
  //    @context
  //    case class Context6(obj: Data) {
  //      def trigger = myRole.foo
  //      val myRole = obj
  //      role myRole {
  //        def foo = "FOO"
  //      }
  //    }
  //
  //
  //    // Disallowed
  //    Context1(new Sub).trigger === "baz"
  //  }
}













































