package scaladci
import scala.language.experimental.macros
import scala.reflect.macros.{Context => MacroContext}

object DciContext {

  def transform(c: MacroContext)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    val (mods, className, typeDefs, template) = {
      annottees.map(_.tree).toList match {
        case ClassDef(mods, name, typeDefs, templ) :: Nil => (mods, name, typeDefs, templ)
        case x                                            => c.abort(NoPosition, "Only classes can be transformed to DCI Contexts. Found:\n" + showRaw(x))
      }
    }

    object debug {
      def abort(t: Any) {
        c.abort(c.enclosingPosition, t.toString)
      }

      def comp(t1: Any, t2: Any = template.body) {
        abort(s"\n$t1\n-------------------\n$t2")
      }
      //      def comp(t1: List[Tree], t2: List[Tree], i: Int = -1, j: Int = -1) {
      def comp(t1: List[Tree], t2: List[Tree]) {
        val source = code(t1) + "\n\n================\n" + code(t2)
        val ast = raw(t1) + "\n\n================\n" + raw(t2)
        abort(s"\n$source\n\n#################################################\n\n$ast")
      }
      def comp(t1: Tree, t2: Tree) {
        val source = t1 + "\n\n================\n" + t2
        val ast = showRaw(t1) + "\n\n================\n" + showRaw(t2)
        abort(s"\n$source\n\n#################################################\n\n$ast")
      }

      def compareRaw(t1: Any, t2: Any = template.body) {
        abort(s"\n${showRaw(t1)}\n-------------------\n${showRaw(t2)}")
      }

      def l(ts: List[Tree]) {
        abort(code(ts))
      }

      def r(t: Any) {
        c.abort(c.enclosingPosition, showRaw(t))
      }

      def r(ts: List[Tree]) {
        abort(raw(ts))
      }

      def sep(ts: List[Tree]) = {
        code(ts) + "\n\n================\n" + raw(ts)
      }

      def lr(ts: List[Tree]) {
        abort(sep(ts))
      }

      def code(ts: List[Tree]) = ts.zipWithIndex.map { case (t, i) => s"\n-- $i -- " + t}

      def raw(ts: List[Tree]) = ts.zipWithIndex.map { case (t, i) => s"\n-- $i -- " + showRaw(t)}

      //      def raw(ts: List[Tree]) = ts.map("\n----- " + showRaw(_))

      def err(t: Tree, msg: String) = {
        abort(msg + t)
        t
      }
    }
    import debug._

    object ctx {
      val body           = template.body
      val roleCandidates = body.collect { case ValDef(modifiers, name, _, _) => name}
      val roleMethods    = (t: List[Tree]) => t collect { case DefDef(_, meth, _, _, _, _) if meth != nme.CONSTRUCTOR => meth.toString}
      val roles          = body.collect {
        case Apply(Select(Ident(kw), roleName), List(Block(roleBody, _))) if kw.toString == "role" =>
          // Check defined role name against available variables in the Context
          val verifiedRoleName = if (roleCandidates contains roleName) roleName.toString
          else abort(s"Defined role name `${roleName.toString}` has to match some of the available variables in the Context:\n" + roleCandidates.mkString("\n"))
          verifiedRoleName -> roleMethods(roleBody)

        case Apply(Apply(Ident(kw), List(Ident(roleName))), List(Block(roleBody, _))) if kw.toString == "role" =>
          roleName.toString -> roleMethods(roleBody)
      }.toMap

      def isRoleMethod(qualifier: String, methodName: String) =
        !roles.isEmpty && roles.contains(qualifier) && roles(qualifier).contains(methodName)
    }


    // AST transformers ====================================================================

    // Transform role method body
    case class roleMethodTransformer(roleName: String) extends Transformer {
      override def transform(roleMethodTree: Tree): Tree = roleMethodTree match {

        // Disallow nested role definitions
        case nestedRoleDef@Apply(Select(Ident(kw), nestedRoleName), _) if kw.toString == "role"             =>
          abort(s"Nested role definitions are not allowed.\nPlease remove nested role '${nestedRoleName.toString}' inside the $roleName role.")
          EmptyTree
        case nestedRoleDef@Apply(Apply(Ident(kw), List(Ident(nestedRoleName))), _) if kw.toString == "role" =>
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

    // Transform Role body
    case class roleTransformer(roleName: String) extends Transformer {
      override def transform(roleTree: Tree): Tree = roleTree match {

        // Transform role method
        case roleMethod@DefDef(x1, roleMethodName, x2, x3, x4, roleMethodBody) => {

          // Prefix role method name
          // roleMethod => RoleName_roleMethod
          val newRoleMethodName = newTermName(roleName + "_" + roleMethodName.toString)
          //          comp(roleMethodName, newRoleMethodName)

          // Transform role method body
          val newRoleMethodBody = roleMethodTransformer(roleName).transform(roleMethodBody)
          //          comp(roleMethodBody, newRoleMethodBody)

          // Build role method AST
          val newRoleMethod = DefDef(Modifiers(PRIVATE), newRoleMethodName, x2, x3, x4, newRoleMethodBody)
          //          comp(roleMethod, newRoleMethod)
          newRoleMethod
        }

        // Only allow role methods ("No state in Roles!")
        case otherCodeInRole =>
          //          r(otherCodeInRole)
          abort(s"Roles are only allowed to define methods.\n" +
            s"Please remove the following code from the $roleName role:" +
            s"\n$otherCodeInRole\n----------------------")
          EmptyTree
      }
    }

    // Transform Context body
    object contextTransformer extends Transformer {
      override def transform(contextTree: Tree): Tree = contextTree match {

        // role RoleName {...}
        case roleDef@Apply(Select(Ident(kw), roleName), List(Block(roleBody, Literal(Constant(())))))
          if kw.toString == "role" => //if (ctx.isRoleMethod(kw.toString)) {
          val newRoleBody = roleTransformer(roleName.toString).transformTrees(roleBody)
          val newRoleDef = Apply(Select(Ident(newTermName("role")), roleName), List(Block(newRoleBody, Literal(Constant(())))))
          //                    comp(roleDef, newRoleDef)
          newRoleDef
        //        } else abort(s"")

        // role RoleName {...}
        case roleDef@Apply(Apply(Ident(kw), List(Ident(roleName))), List(Block(roleBody, Literal(Constant(())))))
          if kw.toString == "role" =>
          val newRoleBody = roleTransformer(roleName.toString).transformTrees(roleBody)
          val newRoleDef = Apply(Apply(Ident(newTermName("role")), List(Ident(roleName))), List(Block(newRoleBody, Literal(Constant(())))))
          //                    comp(roleDef, newRoleDef)
          newRoleDef

        // role("someString") {...}
        case roleDef@Apply(Apply(Ident(kw), List(Literal(Constant(roleNameString)))), List(Block(roleBody, Literal(Constant(())))))
          if kw.toString == "role" =>
          abort("Strings as role name identifiers are not allowed. Please use a variable instead. Found: \"" + roleNameString.toString + "\"")
          EmptyTree

        // Disallow return values from role definitions
        case roleDef@Apply(Apply(Ident(kw), List(Literal(Constant(roleName)))), List(Block(_, returnValue)))
          if kw.toString == "role" =>
          abort(s"A role definition is not allowed to return a value." +
            s"\nPlease remove the following return code from the '$roleName' role:" +
            s"\n$returnValue\n------------\n${showRaw(roleDef)}\n------------\n$roleDef")
          EmptyTree
        case roleDef@Apply(Apply(Ident(kw), List(Ident(roleName))), List(Block(_, returnValue)))
          if kw.toString == "role" =>
          abort(s"A role definition is not allowed to return a value." +
            s"\nPlease remove the following return code from the ${roleName.toString} role:" +
            s"\n$returnValue\n------------\n${showRaw(roleDef)}\n------------\n$roleDef")
          EmptyTree

        // Transform context tree recursively
        case _ => super.transform(contextTree)
      }
    }

    // Transform qualified Role method calls
    object roleMethodCalls extends Transformer {
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
    contextTree = roleMethodCalls.transformTrees(contextTree)
    contextTree = contextTransformer.transformTrees(contextTree)
    contextTree = globalizeRoleMethodsToContext(contextTree)
//        comp(ctx.body, contextTree)

    // Return transformed context
    c.Expr[Any](ClassDef(mods, className, typeDefs, Template(Nil, emptyValDef, contextTree)))
  }
}