package scaladci

import scala.reflect.macros.{Context => MacroContext}
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

object dci {

  // "Role Definition Method" - acting as keyword and placeholder for defining role methods. Will be discarded itself during transformation.
  def role(instance: Any)(roleMethods: => Unit) {}

  def transformDCIcontext(c: MacroContext)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    val (mods, className, typeDefs, template) = {
      annottees.map(_.tree).toList match {
        case ClassDef(mods, name, typeDefs, templ) :: Nil => (mods, name, typeDefs, templ)
        case x => c.abort(NoPosition, "Only classes can be transformed to DCI Contexts. Found:\n" + showRaw(x))
      }
    }

    object debug {
      def out(t: Any) {
        c.abort(c.enclosingPosition, t.toString)
      }

      def compare(t1: Any, t2: Any = template.body) {
        out(s"\n$t1\n-------------------\n$t2")
      }

      def compareRaw(t1: Any, t2: Any = template.body) {
        out(s"\n${showRaw(t1)}\n-------------------\n${showRaw(t2)}")
      }

      def l(ts: List[Tree]) {
        out(code(ts))
      }

      def r(t: Any) {
        c.abort(c.enclosingPosition, showRaw(t))
      }

      def r(ts: List[Tree]) {
        out(raw(ts))
      }

      def lr(ts: List[Tree]) {
        out(code(ts) + "\n\n================\n" + raw(ts))
      }

      def code(ts: List[Tree]) = ts.map("\n----- " + _)

      def raw(ts: List[Tree]) = ts.map("\n----- " + showRaw(_))

      def err(t: Tree, msg: String) = {
        out(msg + t);
        t
      }
    }
    import debug._

    object ctx {
      val body = template.body
      val params = body.collect {
        case param@ValDef(modifiers, name, _, _) if isParamAccessor(modifiers.flags) => param
      }
      val paramNames = params.collect {
        case ValDef(_, name, _, _) => name.toString
      }
      val fields = body.collect {
        case field@ValDef(modifiers, _, _, _) if !isParamAccessor(modifiers.flags) => field
      }
      val methods = body.collect {
        case method@DefDef(_, name, _, _, _, _) if name != nme.CONSTRUCTOR => method
      }
      val roleDefMethods = body.collect {
        case roleDefMethod@Apply(Apply(Ident(name), _), _) if name.toString == "role" => roleDefMethod
      }
      val roleNames = roleDefMethods.map {
        case Apply(Apply(_, List(Ident(roleName))), _) => roleName.toString
      }
      val roles = roleDefMethods.collect {
        case Apply(Apply(_, List(Ident(roleName))), List(Block(roleBody, _))) =>
          roleName.toString -> (roleBody collect {
            case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != nme.CONSTRUCTOR => roleMethodName.toString
          })
      }.toMap

      def isParamAccessor(flags: FlagSet) = (flags.asInstanceOf[Long] & 1L << 29) != 0

      def isRoleMethod(qualifier: String, methodName: String) =
        !roleDefMethods.isEmpty && roles.contains(qualifier) && roles(qualifier).contains(methodName)

      //      r(body)
    }


    // AST transformers ====================================================================

    // Transform role method body
    case class roleMethodTransformer(roleName: String) extends Transformer {
      override def transform(roleMethodTree: Tree): Tree = roleMethodTree match {

        // Disallow nested role definitions
        case nestedRoleDef@Apply(Apply(Ident(name), List(Ident(nestedRoleName))), _) if name.toString == "role" =>
          out(s"Nested role definitions are not allowed.\nPlease remove nested role '${nestedRoleName.toString}' inside the $roleName role.")
          EmptyTree

        // Transform internal role method calls
        // roleMethod(..) => Role_roleMethod(..)
        case roleMethodRef@Ident(methodName) if ctx.roles(roleName).contains(methodName.toString) =>
          val newRoleMethodRef = Ident(newTermName(roleName + "_" + methodName.toString))
          //          compare(roleMethodRef, newRoleMethodRef)
          newRoleMethodRef

        //        Disallow `this` in role method body
        //        case thisRoleMethodRef@Select(This(tpnme.EMPTY), TermName(methodName)) =>
        //          out(s"`this` in a role method points to the Context and is not allowed in this DCI Context. " +
        //            s"\nPlease access Context members directly if needed or use 'self' to reference the Role Player.")
        //          EmptyTree

        // Allow `this` in role method body
        case thisRoleMethodRef@Apply(Select(This(tpnme.EMPTY), methodName), List(params))
          if ctx.isRoleMethod(roleName, methodName.toString) =>
          val newMethodRef = Apply(Ident(newTermName(roleName + "_" + methodName.toString)), List(params))
          //          compare(thisRoleMethodRef, newMethodRef)
          newMethodRef

        // this.instanceMethod(params..) => RoleName.instanceMethod(params..)
        // this.instanceMethod() => RoleName.instanceMethod()
        // this.instanceMethod => RoleName.instanceMethod
        // someMethod(this) => someMethod(RoleName)
        // possibly other uses...
        case This(tpnme.EMPTY) => Ident(newTermName(roleName))


        // self.roleMethod(params..) => RoleName_roleMethod(params..)
        // Role methods take precedence over instance methods!
        case selfMethodRef@Apply(Select(Ident(name), methodName), List(params))
          if name.toString == "self" && ctx.isRoleMethod(roleName, methodName.toString) =>
          val newMethodRef = Apply(Ident(newTermName(roleName + "_" + methodName.toString)), List(params))
          //          compare(selfMethodRef, newMethodRef)
          newMethodRef

        // self.instanceMethod(params..) => RoleName.instanceMethod(params..)
        // self.instanceMethod() => RoleName.instanceMethod()
        // self.instanceMethod => RoleName.instanceMethod
        // someMethod(self) => someMethod(RoleName)
        // possibly other uses...
        case Ident(name) if name.toString == "self" => Ident(newTermName(roleName))

        // Transform role method tree recursively
        case x => super.transform(roleMethodTree)
      }
    }


    // Transform role body
    case class roleTransformer(roleName: String) extends Transformer {
      override def transform(roleTree: Tree): Tree = roleTree match {

        // Transform role method
        case roleMethod@DefDef(x1, roleMethodName, x2, x3, x4, roleMethodBody) => {

          // Prefix role method name
          // roleMethod => RoleName_roleMethod
          val newRoleMethodName = newTermName(roleName + "_" + roleMethodName.toString)
          //          compare(roleMethodName, newRoleMethodName)

          // Transform role method body
          val newRoleMethodBody = roleMethodTransformer(roleName).transform(roleMethodBody)
          //          compare(roleMethodBody, newRoleMethodBody)
          //          compareRaw(roleMethodBody, newRoleMethodBody)

          // Build role method AST
          val newRoleMethod = DefDef(Modifiers(PRIVATE), newRoleMethodName, x2, x3, x4, newRoleMethodBody)
          //          compare(roleMethod, newRoleMethod)
          newRoleMethod
        }

        // Disallow any other code (only role methods allowed on top level)
        case otherCodeInRole =>
          //          r(otherCodeInRole)
          out(s"Roles are only allowed to define methods.\n" +
            s"Please remove the following code from the $roleName role:" +
            s"\n$otherCodeInRole\n----------------------")
          EmptyTree
      }
    }

    // Transform Context body
    object contextTransformer extends Transformer {
      override def transform(contextTree: Tree): Tree = contextTree match {

        // role(paramIdentifier) {...}
        case roleDef@Apply(Apply(Ident(name), List(Ident(roleName))), List(Block(roleBody, Literal(Constant(())))))
          if name.toString == "role" && ctx.paramNames.contains(roleName.toString) =>
          val newRoleBody = roleTransformer(roleName.toString).transformTrees(roleBody)
          val newRoleDef = Apply(Apply(Ident(newTermName("role")), List(Ident(roleName))), List(Block(newRoleBody, Literal(Constant(())))))
          //          compare(roleDef, newRoleDef)
          newRoleDef

        // role(fieldIdentifier) {...}
        case roleDef@Apply(Apply(Ident(name), List(Ident(roleName))), List(Block(roleBody, Literal(Constant(()))))) if name.toString == "role" =>
          val newRoleBody = roleTransformer(roleName.toString).transformTrees(roleBody)
          val newRoleDef = Apply(Apply(Ident(newTermName("role")), List(Ident(roleName))), List(Block(newRoleBody, Literal(Constant(())))))
          //                    compare(roleDef, newRoleDef)
          newRoleDef

        // role("someString") {...}
        case roleDef@Apply(Apply(Ident(name), List(Literal(Constant(roleNameString)))), List(Block(roleBody, Literal(Constant(()))))) if name.toString == "role" =>
          out("Strings as role name identifiers are not allowed. Please use variable instead. Found: \"" + roleNameString.toString + "\"")
          EmptyTree

        // Disallow return values from role definitions
        case roleDef@Apply(Apply(Ident(name), List(Literal(Constant(roleName)))), List(Block(_, returnValue))) if name.toString == "role" =>
          out(s"A role definition is not allowed to return a value." +
            s"\nPlease remove the following return code from the '$roleName' role:" +
            s"\n$returnValue\n------------\n${showRaw(roleDef)}\n------------\n$roleDef")
          EmptyTree

        case roleDef@Apply(Apply(Ident(name), List(Ident(roleName))), List(Block(x, returnValue))) if name.toString == "role" =>
          out(s"A role definition is not allowed to return a value." +
            s"\nPlease remove the following return code from the ${roleName.toString} role:" +
            s"\n$returnValue\n------------\n${showRaw(roleDef)}\n------------\n$roleDef")
          EmptyTree

        // Transform context tree recursively
        case _ => super.transform(contextTree)
      }
    }

    // Transform role method calls
    object roleMethodCalls extends Transformer {
      override def transform(contextTree: Tree): Tree = contextTree match {

        // RoleName.roleMethod(params..) => RoleName_roleMethod(params..)
        case methodRef@Apply(Select(Ident(qualifier), methodName), List(params))
          if ctx.isRoleMethod(qualifier.toString, methodName.toString) =>
          val newMethodRef = Apply(Ident(newTermName(qualifier.toString + "_" + methodName.toString)), List(params))
          //          compare(methodRef, newMethodRef)
          newMethodRef


        // RoleName.roleMethod() => RoleName_roleMethod()
        case methodRef@Apply(Select(Ident(qualifier), methodName), List())
          if ctx.isRoleMethod(qualifier.toString, methodName.toString) =>
          val newMethodRef = Apply(Ident(newTermName(qualifier.toString + "_" + methodName.toString)), List())
          //          compare(methodRef, newMethodRef)
          newMethodRef

        // RoleName.roleMethod => RoleName_roleMethod
        case methodRef@Select(Ident(qualifier), methodName)
          if ctx.isRoleMethod(qualifier.toString, methodName.toString) =>
          val newMethodRef = Ident(newTermName(qualifier.toString + "_" + methodName.toString))
          //          compare(methodRef, newMethodRef)
          newMethodRef

        // Transform tree recursively
        case _ => super.transform(contextTree)
      }
    }

    // "Globalize" role methods (Role.roleMethod => Role_roleMethod)
    def globalizeRoleMethodsToContext(contextBody: List[Tree]) = contextBody.flatMap {
      case roleDef@Apply(Apply(_, List(Ident(roleName))), List(Block(roleBody, _))) => roleBody.collect {
        case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != nme.CONSTRUCTOR => roleMethod
      }
      case otherContextElement => List(otherContextElement)
    }

    // Transform Context ====================================================================

    // AST transformation phases
    var contextTree: List[Tree] = template.body
    contextTree = roleMethodCalls.transformTrees(contextTree)
    contextTree = contextTransformer.transformTrees(contextTree)
    contextTree = globalizeRoleMethodsToContext(contextTree)
    //    lr(contextTree)

    // Uncomment any of these to see the developing new AST (optionally place them somewhere between the different phases above).
    //        out(contextTree)
    //    compare(ctx.body, contextTree)
    //    compareRaw(ctx.body, contextTree)

    // Return transformed context
    c.Expr[Any](ClassDef(mods, className, typeDefs, Template(Nil, emptyValDef, contextTree)))
  }
}

class context extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro dci.transformDCIcontext
}

