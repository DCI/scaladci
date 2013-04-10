package scaladci
import language.dynamics
import reflect.macros.{Context => MacroContext}

object DCI {

  // "Role Definition Method" - acting as keyword and placeholder for defining role methods. Will be discarded itself during transformation.
  def role(instance: Any)(roleMethods: => Unit) {}

  // Type macro that transforms the DCI Context
  def transformContext(c: MacroContext): c.Tree = {
    import c.universe._
    import Flag._

    object debug {
      def out(t: Any) { c.abort(c.enclosingPosition, t.toString) }
      def compare(t1: Any, t2: Any = c.enclosingTemplate.body) { out(s"\n$t1\n-------------------\n$t2") }
      def compareRaw(t1: Any, t2: Any = c.enclosingTemplate.body) { out(s"\n${showRaw(t1)}\n-------------------\n${showRaw(t2)}") }
      def l(ts: List[Tree]) { out(code(ts)) }
      def r(t: Any) { c.abort(c.enclosingPosition, showRaw(t)) }
      def r(ts: List[Tree]) { out(raw(ts)) }
      def lr(ts: List[Tree]) { out(code(ts) + "\n\n================\n" + raw(ts)) }
      def code(ts: List[Tree]) = ts.map("\n----- " + _)
      def raw(ts: List[Tree]) = ts.map("\n----- " + showRaw(_))
      def err(t: Tree, msg: String) = { out(msg + t); t }
    }
    import debug._

    object ctx {
      val template       = c.enclosingTemplate
      val body           = template.body
      val params         = body.collect { case param@ValDef(Modifiers(flags, _, _), name, _, _) if isParamAccessor(flags) => param }
      val paramNames     = params.collect { case ValDef(_, name, _, _) => name.toString }
      val fields         = body.collect { case field@ValDef(Modifiers(flags, _, _), _, _, _) if !isParamAccessor(flags) => field }
      val methods        = body.collect { case method@DefDef(_, name, _, _, _, _) if name != nme.CONSTRUCTOR => method }
      val roleDefMethods = body.collect { case roleDefMethod@Apply(Apply(Ident(TermName("role")), List(Ident(TermName(roleName)))), _) => roleDefMethod }
      val roleNames      = roleDefMethods.map { case Apply(Apply(_, List(Ident(TermName(roleName)))), _) => roleName }
      val roles          = roleDefMethods.collect {
        case Apply(Apply(_, List(Ident(TermName(roleName)))), List(Block(roleBody, _))) =>
          (roleName -> (roleBody collect {
            case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != nme.CONSTRUCTOR => roleMethodName.toString
          }))
      }.toMap
      def isParamAccessor(flags: FlagSet) = (flags.asInstanceOf[Long] & 1L << 29) != 0

      //      r(body)
    }

    // AST transformers ====================================================================

    // Context scope
    object contextTransformer extends Transformer {
      override def transform(contextTree: Tree): Tree = contextTree match {

        // Transform roles

        // role(paramIdentifier) {...}
        case roleDef@Apply(Apply(Ident(TermName("role")), List(Ident(TermName(roleName)))), List(Block(roleBody, Literal(Constant(())))))
          if ctx.paramNames.contains(roleName) =>
          val newRoleBody = roleTransformer(roleName).transformTrees(roleBody)
          val newRoleDef = Apply(Apply(Ident(TermName("role")), List(Ident(TermName(roleName)))), List(Block(newRoleBody, Literal(Constant(())))))
//                    compare(roleDef, newRoleDef)
          newRoleDef


        // role(fieldIdentifier) {...}
        case roleDef@Apply(Apply(Ident(TermName("role")), List(Ident(TermName(roleName)))), List(Block(roleBody, Literal(Constant(()))))) =>
          val newRoleBody = roleTransformer(roleName).transformTrees(roleBody)
          val newRoleDef = Apply(Apply(Ident(TermName("role")), List(Ident(TermName(roleName)))), List(Block(newRoleBody, Literal(Constant(())))))
//                    compare(roleDef, newRoleDef)
          newRoleDef

        // role("someString") {...}
        case roleDef@Apply(Apply(Ident(TermName("role")), List(Literal(Constant(roleNameString)))), List(Block(roleBody, Literal(Constant(()))))) =>
          out("Strings as role name identifiers are not allowed. Please use variable instead. Found: \"" + roleNameString.toString + "\"")
          EmptyTree

        // Disallow return values from role definitions
        case roleDef@Apply(Apply(Ident(TermName("role")), List(Literal(Constant(roleName)))), List(Block(_, returnValue))) =>
          out(s"A role definition is not allowed to return a value." +
            s"\nPlease remove the following return code from the '$roleName' role:" +
            s"\n$returnValue\n------------\n${showRaw(roleDef)}\n------------\n$roleDef")
          EmptyTree

        case roleDef@Apply(Apply(Ident(TermName("role")), List(Ident(TermName(roleName)))), List(Block(x, returnValue))) =>
          out(s"A role definition is not allowed to return a value." +
            s"\nPlease remove the following return code from the $roleName role:" +
            s"\n$returnValue\n------------\n${showRaw(roleDef)}\n------------\n$roleDef")
          EmptyTree

        // Transform context tree recursively
        case _ => super.transform(contextTree)
      }
    }

    // Role scope
    case class roleTransformer(roleName: String) extends Transformer {
      override def transform(roleTree: Tree): Tree = roleTree match {

        // Transform role method
        case roleMethod@DefDef(x1, roleMethodName, x2, x3, x4, roleMethodBody) => {

          // Prefix role method name
          // roleMethod => RoleName_roleMethod
          val newRoleMethodName = TermName(roleName + "_" + roleMethodName.toString)
          //          compare(roleMethodName, newRoleMethodName)

          // Transform role method body
          val newRoleMethodBody = roleMethodTransformer(roleName).transform(roleMethodBody)
          //          compare(roleMethodBody, newRoleMethodBody)
          //          compareRaw(roleMethodBody, newRoleMethodBody)

          // Build role method AST
          val newRoleMethod = DefDef(Modifiers(PRIVATE), newRoleMethodName, x2, x3, x4, newRoleMethodBody)
//                    compare(roleMethod, newRoleMethod)

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

    // Role method scope
    case class roleMethodTransformer(roleName: String) extends Transformer {
      override def transform(roleMethodTree: Tree): Tree = roleMethodTree match {

        // Disallow nested role definitions
        case nestedRoleDef@Apply(Apply(Ident(TermName("role")), List(Ident(TermName(nestedRoleName)))), _) =>
          out(s"Nested role definitions are not allowed.\nPlease remove nested role '$nestedRoleName' inside the $roleName role.")
          EmptyTree

        // Transform internal role method calls
        // roleMethod(..) => Role_roleMethod(..)
        case roleMethodRef@Ident(TermName(methodName)) if ctx.roles(roleName).contains(methodName) =>
          val newRoleMethodRef = Ident(TermName(roleName + "_" + methodName))
          //          compare(roleMethodRef, newRoleMethodRef)
          newRoleMethodRef

        // Disallow `this` in role methods
        case thisRoleMethodRef@Select(This(tpnme.EMPTY), TermName(methodName)) =>
          out(s"`this` in a role method points to the Context and is not allowed. Please access Context members directly if needed.")
          EmptyTree

        // Transform role method tree recursively
        case x => super.transform(roleMethodTree)
      }
    }

    object roleMethodCalls extends Transformer {
      def isRoleMethod(qualifier: String, identifier: String) = {
        !ctx.roleDefMethods.isEmpty &&
          ctx.roles.contains(qualifier) &&
          ctx.roles(qualifier).contains(identifier)
      }

      override def transform(contextTree: Tree): Tree = contextTree match {

        // Transform role method calls

        // RoleName.roleMethod(params..) => RoleName_roleMethod(params..)
        case methodRef@Apply(Select(Ident(TermName(qualifier)), TermName(identifier)), List(params))
          if isRoleMethod(qualifier, identifier) =>
          val newMethodRef = Apply(Ident(TermName(qualifier + "_" + identifier)), List(params))
          //          compare(methodRef, newMethodRef)
          newMethodRef

        // RoleName.roleMethod() => RoleName_roleMethod()
        case methodRef@Apply(Select(Ident(TermName(qualifier)), TermName(identifier)), List())
          if isRoleMethod(qualifier, identifier) =>
          val newMethodRef = Apply(Ident(TermName(qualifier + "_" + identifier)), List())
          //          compare(methodRef, newMethodRef)
          newMethodRef

        // RoleName.roleMethod => RoleName_roleMethod
        case methodRef@Select(Ident(TermName(qualifier)), TermName(identifier))
          if isRoleMethod(qualifier, identifier) =>
          val newMethodRef = Ident(TermName(qualifier + "_" + identifier))
          //          compare(methodRef, newMethodRef)
          newMethodRef

        // Transform tree recursively
        case _ => super.transform(contextTree)
      }
    }

    // "Globalize" role methods (Role.roleMethod => Role_roleMethod)
    def globalizeRoleMethodsToContext(contextBody: List[Tree]) = contextBody.flatMap {
      case roleDef@Apply(Apply(_, List(Ident(TermName(roleName)))), List(Block(roleBody, _))) => roleBody.collect {
        case roleMethod@DefDef(_, roleMethodName, _, _, _, _) if roleMethodName != nme.CONSTRUCTOR => roleMethod
      }
      case otherContextElement => List(otherContextElement)
    }

    // Transform Context ====================================================================

    // AST transformation phases
    var contextTree: List[Tree] = c.enclosingTemplate.body
    contextTree = roleMethodCalls.transformTrees(contextTree)
    contextTree = contextTransformer.transformTrees(contextTree)
    contextTree = globalizeRoleMethodsToContext(contextTree)
//    lr(contextTree)

    // Uncomment any of these to see the developing new AST (optionally place them somewhere between the different phases above).
//        out(contextTree)
    //    compare(ctx.body, contextTree)
    //    compareRaw(ctx.body, contextTree)

    // Return new context template
    Template(Nil, emptyValDef, contextTree)
  }

  // DCI Context
  type Context =
  macro transformContext
}