package scaladci
package test

object Overriding2Test extends App {

  case class A() {
    def foo() { print("A.foo") }                  // A.foo
    def bar() { print("A.bar -> "); foo() }       // A.bar -> A.foo
    def baz() { print("A.baz -> "); this.bar() }  // A.baz -> A.bar -> A.foo
  }

  case class Ctx(a1: A, a2: A, a3: A) extends Context {
    private val x = a1.as[X]
    private val y = a2.as[Y]
    private val z = a3.as[Z]

    private trait X // Methodless Role (RolePlayer will only have access to instance methods)

    private trait Y {self: A =>
      // can't compile ambiguous method signature (IDE will complain if you uncomment the next line)
      // def foo { print("Y.foo") }

      // override modifier allows same method signature
      override def foo() { print("Y.foo") }             // Y.foo

      // overriden instance method foo in A is never called
      override def bar() { print("Y.bar -> "); foo() }  // Y.bar -> Y.foo

      def yy1() { print("Y.yy1 -> "); self.foo() }      // Y.yy1 -> Y.foo
      def yy2() { print("Y.yy2 -> "); this.bar() }      // Y.yy2 -> Y.bar -> Y.foo
      def ya1() { print("Y.ya1 -> "); baz() }           // Y.ya1 -> A.baz -> Y.bar -> Y.foo
    }

    private trait Z {self: A =>
      // overriding methods in other Role never call overriden instance methods
      def zy1() { print("Z.zy1 -> "); y.foo } // Z.zy1 -> Y.foo [calling overriding method in other Role]
      def za1() { print("Z.za1 -> "); foo() } // Z.za1 -> A.foo [calling instance method directly]
      def zy2() { print("Z.zy2 -> "); y.bar } // Z.zy2 -> Y.bar -> Y.foo
      def zy3() { print("Z.zy3 -> "); y.baz } // Z.zy3 -> A.baz -> Y.bar -> Y.foo
      def za2() { print("Z.za2 -> "); baz() } // Z.za2 -> A.baz -> A.bar -> A.foo
    }

    def test() {
      x.foo; println("")
      x.bar; println("")
      x.baz; println("\n--------------------------------")

      y.foo; println("")
      y.bar; println("")
      y.yy1; println("")
      y.yy2; println("")
      y.ya1; println("\n--------------------------------")

      z.zy1; println("")
      z.za1; println("")
      z.zy2; println("")
      z.zy3; println("")
      z.za2; println("\n--------------------------------")
    }
  }

  val a = A()
  Ctx(a, a, a).test()
  /* expected:
  A.foo
  A.bar -> A.foo
  A.baz -> A.bar -> A.foo
  --------------------------------
  Y.foo
  Y.bar -> Y.foo
  Y.yy1 -> Y.foo
  Y.yy2 -> Y.bar -> Y.foo
  Y.ya1 -> A.baz -> Y.bar -> Y.foo
  --------------------------------
  Z.zy1 -> Y.foo
  Z.za1 -> A.foo
  Z.zy2 -> Y.bar -> Y.foo
  Z.zy3 -> A.baz -> Y.bar -> Y.foo
  Z.za2 -> A.baz -> A.bar -> A.foo
  --------------------------------
  */
}