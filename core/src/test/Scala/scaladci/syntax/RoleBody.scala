//package scaladci
//package syntax
//
//import org.specs2.mutable._
//
//class RoleBody extends Specification {
//
//  case class Data(i: Int)
//
//
//  "other role" >> {
//    //    expectCompileError(
//    //      """
//    @context
//    case class Context(Foo: Data, Bar: Data) {
//      //          Bar.roleMethod
//      //          Foo.Bar
//      //          role Foo {
//      //      Foo.xxx
//
//      role Foo
//
//      role Foo 1
//
//      role Foo()
//
//      role Foo (2)
//
//      role.Foo(1)
//
//      role.Foo {3}
//
//      role Foo
//        role Bar {
//
//      }
//
//      role Foo
//        role Bar
//
//
//
//      role Foo {
//        def aaaaa = 1
//      }
//
//      role Foo()
//
//
//      role(Bar)
//
//      role(Bar) {}
//
//      role(Foo) {
//        Foo.xxx
//        def roleMethod1 = {
//          Foo.xxx
//
//        }
//        def xxx = 7
//        //            def roleMethod2 = 7
//        //            role Bar {}
//        //            role Bar
//        //            role.Bar
//        role(Bar) {
//          Foo.xxx
//
//        }
//        //            role(Bar)()
//        //            role(Bar)
//        //            role()
//        //            role()()
//      }
//    }
//
//
//    //    val cc = ContextTransformer.transformCaller{
//    //    class Context(Foo: Data) { role Foo { role Bar {}}}
//    //  }
//    //      ClassDef(Modifiers(), newTypeName("Context"), List(), Template(List(Select(Ident(scala), newTypeName("AnyRef"))), emptyValDef, List(ValDef(Modifiers(PRIVATE | LOCAL | PARAMACCESSOR), newTermName("Foo"), Ident(newTypeName("Data")), EmptyTree), DefDef(Modifiers(), nme.CONSTRUCTOR, List(),                                                         List(List(ValDef(Modifiers(PARAM | PARAMACCESSOR), newTermName("Foo"), Ident(newTypeName("Data")), EmptyTree))), TypeTree(), Block(List(Apply(Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR), List())), Literal(Constant(())))), Apply(Select(Ident(newTermName("role")), newTermName("Foo")), List(Apply(Select(Ident(newTermName("role")), newTermName("Bar")), List(Literal(Constant(()))))))))))
//
//
//    //    cc === 8
//    //      """,
//    //      """
//    //        |Roles are only allowed to define methods.
//    //        |Please remove the following code from `Foo`:
//    //        |object NoGoObject extends scala.AnyRef {
//    //        |  def <init>() = {
//    //        |    super.<init>();
//    //        |    ()
//    //        |  }
//    //        |}
//    //        |----------------------
//    //      """)
//    success
//  }
//
//
//  //  "Can only define role methods" >> {
//  //
//  //    @context
//  //    case class Context(Foo: Data) {
//  //      def trigger = Foo.bar
//  //
//  //      role Foo {
//  //        def bar = 2 * baz
//  //        def baz = 3 * buz
//  //        def buz = 4 * Foo.number
//  //      }
//  //    }
//  //    Context(Data(5)).trigger === 2 * 3 * 4 * 5
//  //  }
//  //
//  //  "Can't define" >> {
//  //
//  //    "val" >> {
//  //      expectCompileError(
//  //        """
//  //          @context
//  //          class Context(Foo: Int) {
//  //            role Foo {
//  //              val notAllowed = 42   // <-- no state in Roles!
//  //            }
//  //          }
//  //        """,
//  //        """
//  //          |Roles are only allowed to define methods.
//  //          |Please remove the following code from `Foo`:
//  //          |val notAllowed = 42
//  //          |----------------------
//  //        """)
//  //      success
//  //    }
//  //
//  //    "var" >> {
//  //      expectCompileError(
//  //        """
//  //          @context
//  //          class Context(Foo: Int) {
//  //            role Foo {
//  //              var notAllowed = 42   // <-- no state in Roles!
//  //            }
//  //          }
//  //        """,
//  //        """
//  //          |Roles are only allowed to define methods.
//  //          |Please remove the following code from `Foo`:
//  //          |var notAllowed = 42
//  //          |----------------------
//  //        """)
//  //      success
//  //    }
//  //
//  //    "type" >> {
//  //      expectCompileError(
//  //        """
//  //          @context
//  //          class Context(Foo: Int) {
//  //            role Foo {
//  //              type notAllowed = String   // <-- no type definitions in Roles ...?!
//  //            }
//  //          }
//  //        """,
//  //        """
//  //          |Roles are only allowed to define methods.
//  //          |Please remove the following code from `Foo`:
//  //          |type notAllowed = String
//  //          |----------------------
//  //        """)
//  //      success
//  //    }
//  //
//  //    "class" >> {
//  //      expectCompileError(
//  //        """
//  //          @context
//  //          class Context(Foo: Int) {
//  //            role Foo {
//  //              class NoGoClass  // <-- no class definitions in Roles!
//  //            }
//  //          }
//  //        """,
//  //        """
//  //          |Roles are only allowed to define methods.
//  //          |Please remove the following code from `Foo`:
//  //          |class NoGoClass extends scala.AnyRef {
//  //          |  def <init>() = {
//  //          |    super.<init>();
//  //          |    ()
//  //          |  }
//  //          |}
//  //          |----------------------
//  //        """)
//  //      success
//  //    }
//  //
//  //    "object" >> {
//  //      expectCompileError(
//  //        """
//  //          @context
//  //          class Context(Foo: Int) {
//  //            role Foo {
//  //              object NoGoObject    // <-- no object definitions in Roles!
//  //            }
//  //          }
//  //        """,
//  //        """
//  //          |Roles are only allowed to define methods.
//  //          |Please remove the following code from `Foo`:
//  //          |object NoGoObject extends scala.AnyRef {
//  //          |  def <init>() = {
//  //          |    super.<init>();
//  //          |    ()
//  //          |  }
//  //          |}
//  //          |----------------------
//  //        """)
//  //      success
//  //    }
//  //
//  //    "other role" >> {
//  //      expectCompileError(
//  //        """
//  //          @context
//  //          class Context(Foo: Int) {
//  //            role Foo {
//  ////                      var notAllowed = 42   // <-- no state in Roles!
//  //              role Bar {}   // <-- no nested role definitions inside role definitions!
//  //            }
//  //          }
//  //        """,
//  //        """
//  //          |Roles are only allowed to define methods.
//  //          |Please remove the following code from `Foo`:
//  //          |object NoGoObject extends scala.AnyRef {
//  //          |  def <init>() = {
//  //          |    super.<init>();
//  //          |    ()
//  //          |  }
//  //          |}
//  //          |----------------------
//  //        """)
//  //      success
//  //    }
//  //
//  //
//  //
//  //
//  //
//  //    "etc..." >> {
//  //      ok
//  //    }
//  //  }
//}
