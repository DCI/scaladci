package scaladci

import scala.language.experimental.macros
import scala.reflect.macros.{Context => MacroContext}
import scaladci.util.MacroHelper
import scala.annotation.StaticAnnotation
import scala._

class context extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro ContextTransformer.transform
}

object ContextTransformer {

  def transformCaller(annottees: Any*): Any = macro transform
  val role = "role"

  def transform(c: MacroContext)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    val helper = new MacroHelper[c.type] {val c0: c.type = c}
    import c.universe._, Flag._, helper._
    val x = debug("ContextTransformer", 1)


    // Extract main building blocks of context class AST =======================================

    val (modifiers, className, typeDefs, template) = {
      annottees.map(_.tree).toList match {
        case ClassDef(mods, name, tpeDefs, templ) :: Nil => (mods, name, tpeDefs, templ)
        case x                                           => c.abort(NoPosition, "Only classes/case classes can be transformed to DCI Contexts. Found:\n" + showRaw(x))
      }
    }

    // Reflection helper to analyze context body content
    object ctx {
      val body           = template.body
      val roleCandidates = body.collect { case ValDef(_, name, _, _) => name}
      val roleMethods    = (t: List[Tree]) => t collect { case DefDef(_, meth, _, _, _, _) if meth != nme.CONSTRUCTOR => meth.toString}
      val roles          = body.collect {
        case Apply(Select(Ident(kw), roleName), List(Block(roleBody, _))) if kw.toString == role =>
          // Check defined role name against available variables in the Context
          val verifiedRoleName = if (roleCandidates contains roleName) roleName.toString
          else abort(s"Defined role name `${roleName.toString}` has to match some of the available variables in the Context: " + roleCandidates.mkString(", "))
          verifiedRoleName -> roleMethods(roleBody)

        case Apply(Apply(Ident(kw), List(Ident(roleName))), List(Block(roleBody, _))) if kw.toString == role =>
          roleName.toString -> roleMethods(roleBody)
      }.toMap

      def isRoleMethod(qualifier: String, methodName: String) =
        !roles.isEmpty && roles.contains(qualifier) && roles(qualifier).contains(methodName)
    }


    // AST transformers ====================================================================

    case class roleMethodTransformer(roleName: String) extends Transformer {
      override def transform(roleMethodTree: Tree): Tree = roleMethodTree match {

        // Disallow nested role definitions
        case nestedRoleDef@Apply(Select(Ident(kw), nestedRoleName), _) if kw.toString == role             =>
          abort(s"Nested role definitions are not allowed.\nPlease remove nested role '${nestedRoleName.toString}' inside the $roleName role.")
          EmptyTree
        case nestedRoleDef@Apply(Apply(Ident(kw), List(Ident(nestedRoleName))), _) if kw.toString == role =>
          abort(s"Nested role definitions are not allowed.\nPlease remove nested role '${nestedRoleName.toString}' inside the $roleName role.")
          EmptyTree

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
        case selfMethodRef@Apply(Select(Ident(kw), methodName), List(params))
          if kw.toString == "self" && ctx.isRoleMethod(roleName, methodName.toString) =>
          val newMethodRef = Apply(Ident(newTermName(roleName + "_" + methodName.toString)), List(params))
          //          comp(selfMethodRef, newMethodRef)
          newMethodRef

        // self.instanceMethod(params..) => RoleName.instanceMethod(params..)
        // self.instanceMethod() => RoleName.instanceMethod()
        // self.instanceMethod => RoleName.instanceMethod
        // someMethod(self) => someMethod(RoleName)
        // possibly other uses?...
        case Ident(kw) if kw.toString == "self" => Ident(newTermName(roleName))

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

        // Only allow role methods ("No state in Roles!")
        case otherCodeInRole =>
//          r(otherCodeInRole)
          //          abort(s"Roles are only allowed to define methods.\nx")
          abort(s"Roles are only allowed to define methods.\n" +
            s"Please remove the following code from `$roleName`:" +
            s"\n$otherCodeInRole\n----------------------")
          EmptyTree
      }
    }

    object roleTransformer extends Transformer {
      override def transform(contextTree: Tree): Tree = {

        def abortNestedRole(i: Int, role2: Name, code: Tree) = {
          abort(s"#$i  Role definition not allowed inside another role definition.\nPlease remove role definition `${role2.toString}`:\n$code")
          Nil
        }

        def getRoleBody(body: List[Tree]): List[Tree] = body match {
          case List(Block(validRoleBody, _))                                   => validRoleBody
          case noRoleMethod => noRoleMethod
//          case List(Apply(Select(Ident(TermName("role")), r2), _))             => abortNestedRole(1, r2, roleDef) // role RoleName {...}
//          case List(Select(Ident(TermName("role")), r2))                       => abortNestedRole(2, r2, roleDef) // role RoleName
//          case List(Apply(Apply(Ident(TermName("role")), List(Ident(r2))), _)) => abortNestedRole(3, r2, roleDef) // role(RoleName) {...}
//          case List(Apply(Ident(TermName("role")), List(Ident(r2))))           => abortNestedRole(4, r2, roleDef) // role(RoleName)
//          case List(Apply(Ident(TermName("role")), List()))                    => abortNestedRole(5, Name("Empty"), roleDef) // role()
//          case unexpected                                                      =>
//            abort(s"Unexpected code in role definition of `$roleName`:\n $unexpected\n${showRaw(unexpected)}"); Nil
        }

        contextTree match {

          // role RoleName {...}
          case roleDef@Apply(Select(Ident(TermName("role")), roleName), body) => {
//          case roleDef@Apply(Select(Ident(TermName("role")), roleName), List(body)) => {
            val roleBody = getRoleBody(body, roleDef, roleName)
            val newRoleBody = roleBodyTransformer(roleName.toString).transformTrees(roleBody)
            val newRoleDef = Apply(Select(Ident(newTermName(role)), roleName), List(Block(newRoleBody, Literal(Constant(())))))
            //          comp(roleDef, newRoleDef)
//            x(21, roleDef, newRoleDef)
            newRoleDef
          }

          // role(RoleName) {...}
          case roleDef@Apply(Apply(Ident(TermName("role")), List(Ident(roleName))), List(Block(body, Literal(Constant(()))))) => {
            val roleBody = getRoleBody(body, roleDef, roleName)
            val newRoleBody = roleBodyTransformer(roleName.toString).transformTrees(roleBody)

//            val newRoleBody = roleBodyTransformer(roleName.toString).transformTrees(body)
            val newRoleDef = Apply(Apply(Ident(newTermName(role)), List(Ident(roleName))), List(Block(newRoleBody, Literal(Constant(())))))
            //          comp(roleDef, newRoleDef)
//            x(23, roleDef, newRoleDef)
            newRoleDef
          }

          // role("RoleNameString") {...}
          case roleDef@Apply(Apply(Ident(kw), List(Literal(Constant(roleNameString)))), List(Block(roleBody, Literal(Constant(())))))
            if kw.toString == role =>
            x(30, roleDef, roleNameString)
            abort("Strings as role name identifiers are not allowed. Please use a variable instead. Found: \"" + roleNameString.toString + "\"")
            EmptyTree

          // Disallow return values from role definitions
          case roleDef@Apply(Apply(Ident(kw), List(Literal(Constant(roleName)))), List(Block(_, returnValue)))
            if kw.toString == role =>
            x(31, roleDef, roleName)
            abort(s"A role definition is not allowed to return a value." +
              s"\nPlease remove the following return code from the '$roleName' role:" +
              s"\n$returnValue\n------------\n${showRaw(roleDef)}\n------------\n$roleDef")
            EmptyTree

          case roleDef@Apply(Apply(Ident(kw), List(Ident(roleName))), List(Block(_, returnValue)))
            if kw.toString == role =>
            x(32, roleDef, roleName)
            abort(s"A role definition is not allowed to return a value." +
              s"\nPlease remove the following return code from the `${roleName.toString}` role definition body:" +
              s"\n$returnValue\n------------\n${showRaw(roleDef)}\n------------\n$roleDef")
            EmptyTree

          // Transform context tree recursively
          case _ => super.transform(contextTree)
        }
      }
    }


    //    Apply(
    //      Select(Ident(newTermName("role")), newTermName("Foo")), List(
    //        Block(List(
    //          Apply(Select(Ident(newTermName("role")), newTermName("Bar")), List(Literal(Constant(())))),
    //          DefDef(Modifiers(), newTermName("roleMethod"), List(), List(), TypeTree(), Literal(Constant(7)))),
    //          Literal(Constant(())))))

    // Transform qualified Role method calls in the Context scope
    object roleMethodCallsInContext extends Transformer {
      //    object roleMethodCallsInContext extends Transformer {
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



    //    Block(
    //      List(
    //        ClassDef(Modifiers(), newTypeName("Context"), List(), Template(List(TypeTree()), emptyValDef,
    //          List(ValDef(Modifiers(PRIVATE | LOCAL | PARAMACCESSOR), newTermName("Foo"), TypeTree().setOriginal(Select(Ident(scala), scala.Int)), EmptyTree),
    //            DefDef(Modifiers(), nme.CONSTRUCTOR, List(),
    //              List(List(ValDef(Modifiers(PARAM | PARAMACCESSOR), newTermName("Foo"), TypeTree().setOriginal(Select(Ident(scala), scala.Int)), EmptyTree))),
    //              TypeTree(),
    //              Block(List(Apply(Select(Super(This(newTypeName("Context")), tpnme.EMPTY), nme.CONSTRUCTOR), List())), Literal(Constant(())))),
    //            Apply(
    //              Apply(Select(Select(Select(This(newTypeName("scaladci")), scaladci.package), newTermName("role")), newTermName("applyDynamic")), List(Literal(Constant("Foo")))),
    //    List(
    //      Apply(
    //        Apply(Select(Select(Select(This(newTypeName("scaladci")), scaladci.package), newTermName("role")), newTermName("applyDynamic")), List(Literal(Constant("Bar")))),
    //    List(
    //      Literal(Constant(()))
    //    )
    //    ) ) ) ) )
    //    ) ),
    //    Literal(Constant(()))
    //    )


    // "Globalize" role methods (Role.roleMethod => Role_roleMethod)
    def globalizeRoleMethodsToContext(contextBody: List[Tree]) = contextBody.flatMap {
      case roleDef@Apply(Select(_, roleName), List(Block(roleBody, _)))             => roleBody.collect {
        case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != nme.CONSTRUCTOR => roleMethod
      }
      case roleDef@Apply(Apply(_, List(Ident(roleName))), List(Block(roleBody, _))) => roleBody.collect {
        case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != nme.CONSTRUCTOR => roleMethod
      }
      case otherContextElement                                                      => List(otherContextElement)
    }

    // Transform Context ====================================================================

    // AST transformation phases
    var contextTree: List[Tree] = template.body
    contextTree = roleMethodCallsInContext.transformTrees(contextTree)
    //    comp(ctx.body, contextTree)
    contextTree = roleTransformer.transformTrees(contextTree)
//    r(contextTree.last)
//    x(101, ctx.body, contextTree)
    contextTree = globalizeRoleMethodsToContext(contextTree)


    // Return transformed context
    c.Expr[Any](ClassDef(modifiers, className, typeDefs, Template(Nil, emptyValDef, contextTree)))
  }
}


//    Block(
//      List(
//        ClassDef(
//          Modifiers(),
//          newTypeName("Context"),
//          List(),
//          Template(
//            List(Select(Ident(scala), newTypeName("AnyRef"))),
//            emptyValDef,
//            List(
//              ValDef(Modifiers(PRIVATE | LOCAL | PARAMACCESSOR), newTermName("Foo"), TypeTree().setOriginal(Select(This(newTypeName("RoleBody")), newTypeName("Data"))), EmptyTree),
//              DefDef(Modifiers(), nme.CONSTRUCTOR, List(), List(List(
//                ValDef(Modifiers(PARAM | PARAMACCESSOR), newTermName("Foo"), TypeTree().setOriginal(Select(This(newTypeName("RoleBody")), newTypeName("Data"))), EmptyTree))),
//                TypeTree(),
//                Block(List(Apply(Select(Super(This(newTypeName("Context")), tpnme.EMPTY), nme.CONSTRUCTOR), List())), Literal(Constant(())))),
//              Apply(Apply(Select(Select(Select(This(newTypeName("scaladci")), scaladci.package), newTermName("role")), newTermName("applyDynamic")), List(Literal(Constant("Foo")))
//    ), List(Apply(Apply(Select(Select(Select(This(newTypeName("scaladci")), scaladci.package), newTermName("role")), newTermName("applyDynamic")), List(Literal(Constant("Bar")))),
//    List(Literal(Constant(())))) ) ) ) ) ) ),
//    Literal(Constant(()))) )
