package scaladci
import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.{Context => MacroContext}
import scaladci.util.MacroHelper


// Annotation alternatives //////////////////////////////////////////////////

// `@context class Context` ...
class context extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ContextTransformer.transform
}


// AST transformation ///////////////////////////////////////////////////////

object ContextTransformer {

  def transform(c: MacroContext)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    val helper = new MacroHelper[c.type] {val c0: c.type = c}
    import c.universe._
    import Flag._
    import helper._
    val x = debug("ContextTransformer", 1)


    // Extract main building blocks of context class AST =======================================

    val (ctxModifiers, ctxName, ctxTypeDefs, ctxTemplate) = annottees.head.tree match {
      //      case t@ClassDef(_, _, _, _) if t.mods.hasFlag(TRAIT)    => abort("Using a trait as a DCI context is not allowed")
      //      case t@ClassDef(_, _, _, _) if t.mods.hasFlag(ABSTRACT) => abort("Using abstract class as a DCI context is not allowed")
      case t@ClassDef(mods, name, tpeDefs, tmpl) => (mods, name, tpeDefs, tmpl)
      case t@ModuleDef(mods, name, tmpl)         => (mods, name, Nil, tmpl)
      case tree                                  => abort("Only classes/case classes/traits/objects can be transformed to DCI Contexts. Found:\n" + tree)
    }

    case class abortNestedContextDefinitions(ctxName: NameApi) extends Transformer {
      override def transform(tree: Tree): Tree = tree match {
        case ClassDef(mods, TypeName(nestedCtxName), _, tmpl) if mods.hasCtxAnnotation => abort(
          s"Can't define nested DCI context `$nestedCtxName` inside DCI context `$ctxName`\nCODE: $tree\nAST: ${showRaw(tree)}")
        case _                                                                         => super.transform(tree)
      }
    }
    abortNestedContextDefinitions(ctxName).transform(ctxTemplate)

    // Analyze and check Context AST before transforming
    val ctx = ContextAnalyzer(c)(ctxTemplate)


    // AST transformers ====================================================================

    def removeRoleKeywords(contextBody: List[Tree]) = contextBody.flatMap {
      case roleDef@Apply(Select(_, roleName), List(Block(roleBody, _)))             => roleBody.collect {
        case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != termNames.CONSTRUCTOR => roleMethod
      }
      case roleDef@Apply(Apply(_, List(Ident(roleName))), List(Block(roleBody, _))) => roleBody.collect {
        case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != termNames.CONSTRUCTOR => roleMethod
      }
      case otherContextElement                                                      => List(otherContextElement)
    }

    case class roleMethodTransformer(roleName: String) extends Transformer {
      override def transform(roleMethodTree: Tree): Tree = roleMethodTree match {

        // Transform internal role method calls
        // roleMethod(..) => Role_roleMethod(..)
        case roleMethodRef@Ident(methodName) if ctx.roles(roleName).contains(methodName.toString) =>
          val newRoleMethodRef = Ident(TermName(roleName + "_" + methodName.toString))
          //                    comp(roleMethodRef, newRoleMethodRef)
          newRoleMethodRef


        //                // className.classMethod => className.classMethod
        //                case classMethodRef@Select(Ident(TermName(className)), TermName(methodName)) => classMethodRef // compiler will expose non-valid refs
        //
        //                // classMethod => RoleName.classMethod
        //                case classMethodRef@Ident(methodName) => Select(Ident(TermName(roleName)), methodName) // compiler will expose non-valid refs


        //        // Disallow `this` in role method body
        //        case thisRoleMethodRef@Select(This(typeNames.EMPTY), TermName(methodName)) =>
        //          abort("`this` in a role method points to the Context which is unintentional from a DCI perspective (where it would normally point to the RolePlayer).\n" +
        //            //          abort("`this` in a role method points to the Context and is not allowed in a DCI Context.\n" +
        //            "Please access Context members directly if needed or use `self` to reference the Role Player.")
        //          EmptyTree

        // this.method()
        // this.method(params..)
        case thisRoleMethodRef@Apply(Select(This(typeNames.EMPTY), methodName), List(params)) =>
          val newMethodRef = if (ctx.isRoleMethod(roleName, methodName.toString)) {
            // Role method takes precedence over instance method
            // this.roleMethod(params..) => RoleName_roleMethod(params..)
            Apply(Ident(TermName(roleName + "_" + methodName.toString)), List(params))
          } else {
            // this.instMethod(params..) => RoleName.instMethod(params..)
            Apply(Select(Ident(TermName(roleName)), methodName), List(params))
          }

          //          val newMethodRef = Apply(Ident(TermName(roleName + "_" + methodName.toString)), List(params))
          //          comp(thisRoleMethodRef, newMethodRef)
          newMethodRef


        // this.method
        case selfMethodRef@Select(This(typeNames.EMPTY), methodName) =>
          val newMethodRef = if (ctx.isRoleMethod(roleName, methodName.toString)) {
            // Role method takes precedence over instance method
            // this.roleMethod => RoleName_roleMethod
            Ident(TermName(roleName + "_" + methodName.toString))
          } else {
            // this.instMethod => RoleName.instMethod
            Select(Ident(TermName(roleName)), methodName)
          }
          //          comp(selfMethodRef, newMethodRef)
          newMethodRef


        // this => RoleName
        case This(typeNames.EMPTY) => Ident(TermName(roleName))



        // self.method()
        // self.method(params..)
        case selfMethodRef@Apply(Select(Ident(TermName("self")), methodName), List(params)) =>
          val newMethodRef = if (ctx.isRoleMethod(roleName, methodName.toString)) {
            // Role method takes precedence over instance method
            // self.roleMethod(params..) => RoleName_roleMethod(params..)
            Apply(Ident(TermName(roleName + "_" + methodName.toString)), List(params))
          } else {
            // self.instMethod(params..) => RoleName.instMethod(params..)
            Apply(Select(Ident(TermName(roleName)), methodName), List(params))
          }
          //          comp(selfMethodRef, newMethodRef)
          newMethodRef


        // self.method
        case selfMethodRef@Select(Ident(TermName("self")), methodName) =>
          val newMethodRef = if (ctx.isRoleMethod(roleName, methodName.toString)) {
            // Role method takes precedence over instance method
            // self.roleMethod => RoleName_roleMethod
            Ident(TermName(roleName + "_" + methodName.toString))
          } else {
            // self.instMethod => RoleName.instMethod
            Select(Ident(TermName(roleName)), methodName)
          }
          //          comp(selfMethodRef, newMethodRef)
          newMethodRef


        // self => RoleName
        case Ident(TermName("self")) => Ident(TermName(roleName))

        // Transform role method tree recursively
        case _ => super.transform(roleMethodTree)
      }
    }

    case class roleBodyTransformer(roleName: String) extends Transformer {
      override def transform(roleTree: Tree): Tree = roleTree match {

        // Transform role method
        case roleMethod@DefDef(x1, roleMethodName, x2, x3, x4, roleMethodBody) => {

          // Prefix role method name
          // roleMethod => RoleName_roleMethod
          val newRoleMethodName = TermName(roleName + "_" + roleMethodName.toString)
          //          comp(roleMethodName, newRoleMethodName)

          // Transform role method body
          val newRoleMethodBody = roleMethodTransformer(roleName).transform(roleMethodBody)
          //          comp(roleMethodBody, newRoleMethodBody)

          // Build role method AST
          val newRoleMethod = DefDef(Modifiers(PRIVATE), newRoleMethodName, x2, x3, x4, newRoleMethodBody)
          //          comp(roleMethod, newRoleMethod)
          newRoleMethod
        }

        // Empty role definition (role "stub" or "methodless role")
        case Literal(Constant(())) => super.transform(roleTree)

        // Only allow role methods ("No state in Roles!")
        case otherCodeInRole =>
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

          // role roleName {...}
          case roleDef@Apply(Select(Ident(TermName("role")), roleName), body) => {
            val newRoleBody = roleBodyTransformer(roleName.toString).transformTrees(getRoleBody(body))
            val newRoleDef = Apply(Select(Ident(TermName("role")), roleName), List(Block(newRoleBody, Literal(Constant(())))))
            //            comp(roleDef, newRoleDef)
            newRoleDef
          }

          // role(roleName) {...}
          case roleDef@Apply(Apply(Ident(TermName("role")), List(Ident(roleName))), List(Block(body, Literal(Constant(()))))) => {
            val newRoleBody = roleBodyTransformer(roleName.toString).transformTrees(getRoleBody(body))
            val newRoleDef = Apply(Apply(Ident(TermName("role")), List(Ident(roleName))), List(Block(newRoleBody, Literal(Constant(())))))
            //          comp(roleDef, newRoleDef)
            newRoleDef
          }

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
          val newMethodRef = Apply(Ident(TermName(qualifier.toString + "_" + methodName.toString)), List(params))
          //          comp(methodRef, newMethodRef)
          newMethodRef


        // RoleName.roleMethod() => RoleName_roleMethod()
        case methodRef@Apply(Select(Ident(qualifier), methodName), List())
          if ctx.isRoleMethod(qualifier.toString, methodName.toString) =>
          val newMethodRef = Apply(Ident(TermName(qualifier.toString + "_" + methodName.toString)), List())
          //          comp(methodRef, newMethodRef)
          newMethodRef

        // RoleName.roleMethod => RoleName_roleMethod
        case methodRef@Select(Ident(qualifier), methodName)
          if ctx.isRoleMethod(qualifier.toString, methodName.toString) =>
          val newMethodRef = Ident(TermName(qualifier.toString + "_" + methodName.toString))
          //          comp(methodRef, newMethodRef)
          newMethodRef

        // Transform tree recursively
        case _ => super.transform(contextTree)
      }
    }


    // Transformation phases ====================================================================

    // Context class
    if (ctxName.toString == "role") abort("Context class can't be named `role`")

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

    // Uncomment to compare original and transformed AST
    //    comp(ctx.body, contextTree)

    // Return transformed context (as class or object)
    if (ctxName.isTypeName)
      c.Expr[Any](ClassDef(ctxModifiers, ctxName.toTypeName, ctxTypeDefs, Template(Nil, noSelfType, contextTree)))
    else
      c.Expr[Any](ModuleDef(ctxModifiers, ctxName.toTermName, Template(Nil, noSelfType, contextTree)))
  }
}