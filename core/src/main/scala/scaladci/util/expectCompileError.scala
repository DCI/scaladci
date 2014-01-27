package scaladci.util

import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.reflect.macros.TypecheckException

// Modified `illTyped` macro by Stefan Zeiger (@StefanZeiger)

object expectCompileError {
  def apply(code: String): Unit = macro applyImplNoExp

  def apply(code: String, expected: String): Unit = macro applyImpl

  def applyImplNoExp(c: Context)(code: c.Expr[String]) = applyImpl(c)(code, null)

  def applyImpl(c: Context)(code: c.Expr[String], expected: c.Expr[String]): c.Expr[Unit] = {
    import c.universe._

    val codeStr = code.tree match {
      case Literal(Constant(s: String)) => s.stripMargin.trim
      case x                            => c.abort(c.enclosingPosition, "Unknown code tree in compile check: " + showRaw(x))
    }

    val (expPat, expMsg) = expected match {
      case null                               => (null, "EXPECTED SOME ERROR!")
      case Expr(Literal(Constant(s: String))) => val exp = s.stripMargin.trim; (exp, "EXPECTED ERROR:\n" + exp)
    }



    try {
      c.typeCheck(c.parse("{ " + codeStr + " }"))
      c.abort(c.enclosingPosition,
        s"""Type-checking succeeded unexpectedly!!!
          |CODE:
          |$codeStr
          |$expMsg
          |CODE:
          |${show(c.typeCheck(c.parse("{ " + codeStr + " }")))}
          |--------------------
          |AST:
          |${showRaw(c.typeCheck(c.parse("{ " + codeStr + " }")))}
          |--------------------
         """.stripMargin)
    } catch {
      case e: TypecheckException =>
        val msg = e.getMessage.trim
        if ((expected ne null) && !msg.startsWith(expPat))
          c.abort(c.enclosingPosition,
            s"""Type-checking failed in an unexpected way.
                |CODE:
                |$codeStr
                |$expMsg
                |ACTUAL ERROR:
                |$msg
                |--------------------
              """.stripMargin)
//      case t: Throwable          => c.abort(c.enclosingPosition, s"A3 ###  AUCH: $codeStr \n ${t.getMessage}")
    }

//    c.abort(c.enclosingPosition, "### This passed:\nCODE:\n" + codeStr + "\n" + expMsg) // + "\n" + expPat + "\n" + msg.trim.startsWith(expPat.trim))

    reify(())
  }
}

