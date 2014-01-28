package scaladci

import scala.language.experimental.macros
import scala.reflect.macros.{Context => MacroContext}
import scala.annotation.StaticAnnotation
import scaladci.util.MacroHelper
import scala._

class context extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro ContextTransformer.transform
}

object ContextTransformer {

  def transform(c: MacroContext)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    val helper = new MacroHelper[c.type] {val c0: c.type = c}
    import c.universe._, Flag._, helper._
    val x = debug("ContextTransformer", 1)


    // Extract main building blocks of context class AST =======================================

    case class abortNestedContextDefinitions(ctxName: String) extends Transformer {
      override def transform(tree: Tree): Tree = tree match {
        case ClassDef(mods, TypeName(nestedCtxName), _, tmpl) if mods.hasCtxAnnotation => abort(
          s"Can't define nested DCI context `$nestedCtxName` inside DCI context `$ctxName`\nCODE: $tree\nAST: ${showRaw(tree)}")
        case _ => super.transform(tree)
      }
    }

    val (ctxModifiers, ctxClassName, ctxTypeDefs, ctxTemplate) = annottees.head.tree match {
      case t@ClassDef(_, _, _, _) if t.mods.hasFlag(TRAIT)    => abort("Using a trait as a DCI context is not allowed")
      case t@ClassDef(_, _, _, _) if t.mods.hasFlag(ABSTRACT) => abort("Using abstract class as a DCI context is not allowed")
      case ClassDef(mods, ctxName, tpeDefs, tmpl)             => {
        abortNestedContextDefinitions(ctxName.toString).transform(tmpl)
        (mods, ctxName, tpeDefs, tmpl)
      }
      case tree                                               => abort("Only classes/case classes can be transformed to DCI Contexts. Found:\n" + showRaw(tree))
    }

    // Analyze and check context AST before transforming
    val ctx = ContextAnalyzer(c)(ctxTemplate)


    // AST transformers ====================================================================

    def removeRoleKeywords(contextBody: List[Tree]) = contextBody.flatMap {
      case roleDef@Apply(Select(_, roleName), List(Block(roleBody, _)))             => roleBody.collect {
        case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != nme.CONSTRUCTOR => roleMethod
      }
      case roleDef@Apply(Apply(_, List(Ident(roleName))), List(Block(roleBody, _))) => roleBody.collect {
        case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != nme.CONSTRUCTOR => roleMethod
      }
      case otherContextElement                                                      => List(otherContextElement)
    }

    case class roleMethodTransformer(roleName: String) extends Transformer {
      override def transform(roleMethodTree: Tree): Tree = roleMethodTree match {

        //        // Disallow nested role definitions
        //        case nestedRoleDef@Apply(Select(Ident(TermName("role")), nestedRoleName), _)             =>
        //          abort(s"Nested role definitions are not allowed.\nPlease remove nested role '${nestedRoleName.toString}' inside the $roleName role.")
        //          EmptyTree
        //        case nestedRoleDef@Apply(Apply(Ident(TermName("role")), List(Ident(nestedRoleName))), _) =>
        //          abort(s"Nested role definitions are not allowed.\nPlease remove nested role '${nestedRoleName.toString}' inside the $roleName role.")
        //          EmptyTree

        // Transform internal role method calls
        // roleMethod(..) => Role_roleMethod(..)
        case roleMethodRef@Ident(methodName) if ctx.roles(roleName).contains(methodName.toString) =>
          val newRoleMethodRef = Ident(newTermName(roleName + "_" + methodName.toString))
          //          comp(roleMethodRef, newRoleMethodRef)
          newRoleMethodRef

        // Uncomment if you want to disallow using `this` as a Role identifier
        //        Disallow `this` in role method body
        //        case thisRoleMethodRef@Select(This(tpnme.EMPTY), TermName(methodName)) =>
        //          out(s"`this` in a role method points to the Context and is not allowed in this DCI Context. " +
        //            s"\nPlease access Context members directly if needed or use 'self' to reference the Role Player.")
        //          EmptyTree

        // Allow `this` in role method body
        case thisRoleMethodRef@Apply(Select(This(tpnme.EMPTY), methodName), List(params))
          if ctx.isRoleMethod(roleName, methodName.toString) =>
          val newMethodRef = Apply(Ident(newTermName(roleName + "_" + methodName.toString)), List(params))
          //          comp(thisRoleMethodRef, newMethodRef)
          newMethodRef

        // this.instanceMethod(params..) => RoleName.instanceMethod(params..)
        // this.instanceMethod() => RoleName.instanceMethod()
        // this.instanceMethod => RoleName.instanceMethod
        // someMethod(this) => someMethod(RoleName)
        // possibly other uses?...
        case This(tpnme.EMPTY) => Ident(newTermName(roleName))


        // self.roleMethod(params..) => RoleName_roleMethod(params..)
        // Role methods take precedence over instance methods!
        case selfMethodRef@Apply(Select(Ident(TermName("self")), methodName), List(params))
          if ctx.isRoleMethod(roleName, methodName.toString) =>
          val newMethodRef = Apply(Ident(newTermName(roleName + "_" + methodName.toString)), List(params))
          //          comp(selfMethodRef, newMethodRef)
          newMethodRef

        // self.instanceMethod(params..) => RoleName.instanceMethod(params..)
        // self.instanceMethod() => RoleName.instanceMethod()
        // self.instanceMethod => RoleName.instanceMethod
        // someMethod(self) => someMethod(RoleName)
        // possibly other uses?...
        case Ident(TermName("self")) => Ident(newTermName(roleName))

        // Transform role method tree recursively
        case x => super.transform(roleMethodTree)
      }
    }

    case class roleBodyTransformer(roleName: String) extends Transformer {
      override def transform(roleTree: Tree): Tree = roleTree match {

        // Transform role method
        case roleMethod@DefDef(x1, roleMethodName, x2, x3, x4, roleMethodBody) => {

          // Prefix role method name
          // roleMethod => RoleName_roleMethod
          val newRoleMethodName = newTermName(roleName + "_" + roleMethodName.toString)
          //                    comp(roleMethodName, newRoleMethodName)

          // Transform role method body
          val newRoleMethodBody = roleMethodTransformer(roleName).transform(roleMethodBody)
          //                    comp(roleMethodBody, newRoleMethodBody)

          // Build role method AST
          val newRoleMethod = DefDef(Modifiers(PRIVATE), newRoleMethodName, x2, x3, x4, newRoleMethodBody)
          //                    comp(roleMethod, newRoleMethod)
          newRoleMethod
        }

        // Empty role definition (role "stub" or "methodless role")
        case Literal(Constant(())) => super.transform(roleTree)

        // Only allow role methods ("No state in Roles!")
        case otherCodeInRole =>
          //          r(otherCodeInRole)
          //          abort(s"Roles are only allowed to define methods.\nx")
          abort(s"Roles are only allowed to define methods.\n" +
            s"Please remove the following code from `$roleName`:" +
            s"\nCODE: $otherCodeInRole\nAST: ${showRaw(otherCodeInRole)}")
          EmptyTree
      }
    }

    object roleTransformer extends Transformer {
      override def transform(contextTree: Tree): Tree = {

        def getRoleBody(body: List[Tree]): List[Tree] = body match {
          case List(Block(validRoleBody, _)) => validRoleBody
          case noRoleMethod                  => noRoleMethod
        }

        contextTree match {

          // role RoleName {...}
          case roleDef@Apply(Select(Ident(TermName("role")), roleName), body) => {
            val newRoleBody = roleBodyTransformer(roleName.toString).transformTrees(getRoleBody(body))
            val newRoleDef = Apply(Select(Ident(newTermName("role")), roleName), List(Block(newRoleBody, Literal(Constant(())))))
            //            r(body)
            //            comp(roleDef, newRoleDef)
            //            x(21, roleDef, newRoleDef)
            newRoleDef
          }

          // role(RoleName) {...}
          case roleDef@Apply(Apply(Ident(TermName("role")), List(Ident(roleName))), List(Block(body, Literal(Constant(()))))) => {
            val newRoleBody = roleBodyTransformer(roleName.toString).transformTrees(getRoleBody(body))
            val newRoleDef = Apply(Apply(Ident(newTermName("role")), List(Ident(roleName))), List(Block(newRoleBody, Literal(Constant(())))))
            //            r(body)
            //          comp(roleDef, newRoleDef)
            //            x(23, roleDef, newRoleDef)
            newRoleDef
          }

          // role("RoleNameString") {...}
          case roleDef@Apply(Apply(Ident(TermName("role")), List(Literal(Constant(roleNameString)))), List(Block(roleBody, Literal(Constant(()))))) =>
            x(30, roleDef, roleNameString)
            abort("Strings as role name identifiers are not allowed. Please use a variable instead. Found: \"" + roleNameString.toString + "\"")
            EmptyTree

          // Disallow return values from role definitions
          case roleDef@Apply(Apply(Ident(TermName("role")), List(Literal(Constant(roleName)))), List(Block(_, returnValue))) =>
            x(31, roleDef, roleName)
            abort(s"A role definition is not allowed to return a value." +
              s"\nPlease remove the following return code from the '$roleName' role:" +
              s"\nCODE: $returnValue\n------------\nAST: ${showRaw(roleDef)}\n------------\n$roleDef")
            EmptyTree

          case roleDef@Apply(Apply(Ident(TermName("role")), List(Ident(roleName))), List(Block(_, returnValue))) =>
            x(32, roleDef, roleName)
            abort(s"A role definition is not allowed to return a value." +
              s"\nPlease remove the following return code from the `${roleName.toString}` role definition body:" +
              s"\nCODE: $returnValue\n------------\nAST: ${showRaw(roleDef)}\n------------\n$roleDef")
            EmptyTree

          // Transform context tree recursively
          case _ => super.transform(contextTree)
        }
      }
    }

    case class roleMethodCalls(roles: Map[String, List[String]]) extends Transformer {
      override def transform(contextTree: Tree): Tree = contextTree match {

        // RoleName.roleMethod(params..) => RoleName_roleMethod(params..)
        case methodRef@Apply(Select(Ident(qualifier), methodName), List(params))
          if ctx.isRoleMethod(qualifier.toString, methodName.toString) =>
          val newMethodRef = Apply(Ident(newTermName(qualifier.toString + "_" + methodName.toString)), List(params))
          //          comp(methodRef, newMethodRef)
          newMethodRef


        // RoleName.roleMethod() => RoleName_roleMethod()
        case methodRef@Apply(Select(Ident(qualifier), methodName), List())
          if ctx.isRoleMethod(qualifier.toString, methodName.toString) =>
          val newMethodRef = Apply(Ident(newTermName(qualifier.toString + "_" + methodName.toString)), List())
          //          comp(methodRef, newMethodRef)
          newMethodRef

        // RoleName.roleMethod => RoleName_roleMethod
        case methodRef@Select(Ident(qualifier), methodName)
          if ctx.isRoleMethod(qualifier.toString, methodName.toString) =>
          val newMethodRef = Ident(newTermName(qualifier.toString + "_" + methodName.toString))
          //          comp(methodRef, newMethodRef)
          newMethodRef

        // Transform tree recursively
        case _ => super.transform(contextTree)
      }
    }


    // Transformation phases ====================================================================

    // Context class
    if (ctxClassName.toString == "role") abort("Context class can't be named `role`")

    // Original Context AST
    var contextTree: List[Tree] = ctxTemplate.body

    // Transformation phases --------------------------------------------------------------------

    // RoleName.roleMethod => RoleName_roleMethod
    contextTree = roleMethodCalls(ctx.roles).transformTrees(contextTree)

    // Recursively transform role definitions
    // roleTransformer -> roleBodyTransformer -> roleMethodTransformer
    contextTree = roleTransformer.transformTrees(contextTree)

    // Clean up now obsolete role keywords
    contextTree = removeRoleKeywords(contextTree)

    // Compare original and transformed AST
    //    comp(ctx.body, contextTree)

    // Return transformed context
    c.Expr[Any](ClassDef(ctxModifiers, ctxClassName, ctxTypeDefs, Template(Nil, emptyValDef, contextTree)))
  }
}
















