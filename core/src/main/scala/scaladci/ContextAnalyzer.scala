package scaladci
import scala.reflect.macros.whitebox.{Context => MacroContext}
import scaladci.util.MacroHelper

trait ContextAnalyzer[C <: MacroContext] extends MacroHelper[C] {
  import c0.universe._
  import Flag._
  val ctxTemplate: Tree

  val x = debug("ContextAnalyzer", 1)

  // Validate and organize Context AST

  abortRoleTemplate(ctxTemplate).transform(ctxTemplate)
  case class abortRoleTemplate(tree0: Tree) extends Transformer {
    override def transform(tree: Tree): Tree = tree match {
      case Template(_, ValDef(_, TermName("role"), _, _), _) => abortRoleUse(tree0, "as a template name")
      case _                                                 => super.transform(tree)
    }
  }

  val body  = ctxTemplate.asInstanceOf[TemplateApi].body
  val roles = roleDefinitions(body)

  def hasNoOverride(roleName: String, roleMethod: String) = body.collectFirst {
    case aa@ValDef(_, refName, tpt, _) if refName.toString == roleName =>
      // object has no member with same name as role method
      // TODO: Works only if ref type is defined in same scope!! :-(
      c0.typecheck(tpt, c0.TYPEmode).tpe.member(TermName(roleMethod)) == NoSymbol
  } getOrElse abort("Role method is not allowed to override a data class method")

  def isRoleMethod(roleName: String, methodName: String) =
    roles.nonEmpty && roles.contains(roleName) && roles(roleName).contains(methodName)

  def abortRoleUse(tree: Tree, msg: String, i: Int = 0) =
    abort(s"Using `role` keyword $msg is not allowed.\nCODE: $tree\nAST: ${showRaw(tree)}", i)

  case class rejectNestedRoleDefinitions(tree0: Tree) extends Transformer {
    def err(msg: String, i: Int = 0) = abortRoleUse(tree0, msg, i)
    override def transform(tree: Tree): Tree = tree match {
      case Apply(Select(Ident(TermName("role")), _), _) /* role foo {...} */ => err("on a sub level of the Context", 1)
      case Select(Ident(TermName("role")), roleName)                         => err("on a sub level of the Context", 4)
      case Apply(Ident(TermName("role")), List())                            => err("without a Role name", 1)
      case Apply(Ident(TermName("role")), List(Literal(Constant(_))))        => err("without a Role name", 2)
      case ValDef(_, TermName("role"), _, _)                                 => err("as a variable name")
      case DefDef(_, TermName("role"), _, _, _, _)                           => err("as a method name")
      case t@ClassDef(_, TypeName("role"), _, _) if t.mods.hasFlag(TRAIT)    => err("as a trait name")
      case t@ClassDef(_, TypeName("role"), _, _) if t.mods.hasFlag(CASE)     => err("as a case class name")
      case ClassDef(_, TypeName("role"), _, _)                               => err("as a class name")
      case ModuleDef(_, TermName("role"), _)                                 => err("as an object name")
      case TypeDef(_, TypeName("role"), _, _)                                => err("as a type alias")
      case LabelDef(_, TermName("role"), _)                                  => err("as a label name")
      case Select(_, TermName("role"))                                       => err("as a selector name after a quantifier")
      case Ident(TermName("role"))                                           => err("as a return value")
      case _                                                                 => super.transform(tree)
    }
  }

  def roleDefinitions(ctxBody: List[Tree]) = {

    lazy val valRefs = ctxBody collect { case ValDef(_, valRef, _, _) => valRef }

    def roleMethods(roleName: Name, t: List[Tree]): List[String] = t collect {
      case DefDef(_, meth, _, _, _, _) if meth != termNames.CONSTRUCTOR => meth.toString
      case tree                                                         => abort(
        s"Roles are only allowed to define methods.\nPlease remove the following code from `$roleName`:\nCODE: $tree")
    }

    def verifiedRoleName(roleName: Name, i: Int = 0, t: Tree = EmptyTree): String = {
      // Too hard a limitation...
      //      if (roleName.toString.head.isUpper)
      //        abort(s"Defined role name `${roleName.toString}` should start with lower case to resemble an object")
      //      else
      if (valRefs contains roleName) {
        roleName.toString
      } else abort(s"($i) Defined role name `${roleName.toString}` has to match some object identifier in the Context. " +
        s"Available identifiers:\n" + valRefs.mkString("\n")) // + "\n" + showRaw(t))
    }

    def rejectReturnValue(roleName: Name, returnValue: Tree, roleDef: Tree) {
      if (!returnValue.equalsStructure(Literal(Constant(())))) abort(s"A role definition is not allowed to return a value." +
        s"\nPlease remove the following return code from the `${roleName.toString}` role definition body:" +
        s"\nRETURN CODE: $returnValue\nRETURN AST:  ${showRaw(returnValue)}\n" +
        s"------------\nROLE CODE$roleDef\nROLE AST:  ${showRaw(roleDef)}")
    }

    def rejectRoleBodyAssignment(roleName: Name, i: Int = 0) = abort(
      s"($i) Can't assign a Role body to `$roleName`. Please remove `=` before the body definition")

    def rejectConstantAsRoleName(roleType: String, roleName: String, i: Int = 0) = abort(
      s"($i) $roleType as role name identifier is not allowed. Please use a variable instead. Found: $roleName")

    lazy val missingRoleName = "`role` keyword without Role name is not allowed"


    val roles: List[Option[(String, List[String])]] = ctxBody map {

      // Rejected role definitions ----------------------------------------------------------------------

      // role foo
      case t@Select(Ident(TermName("role")), roleName) =>
        abort(s"(1) To avoid postfix clashes, please write `role $roleName {}` instead of `role $roleName`")

      /*
        role foo // two lines after each other ...
        role bar // ... unintentionally becomes `role.foo(role).bar`
      */
      case t@Select(Apply(Select(Ident(TermName("role")), roleName), List(Ident(TermName("role")))), roleName2) =>
        abort(s"(2) To avoid postfix clashes, please write `role $roleName {}` instead of `role $roleName`")

      // role foo = {...}
      case t@Assign(Select(Ident(TermName("role")), roleName), _) => rejectRoleBodyAssignment(roleName, 1)

      // role
      case Ident(TermName("role")) => abort(missingRoleName, 1)


      // Approved role definitions ----------------------------------------------------------------------

      // role foo {...}
      case t@Apply(Select(Ident(TermName("role")), roleName), List(Block(roleBody, returnValue))) =>
        roleBody.foreach(t => rejectNestedRoleDefinitions(t).transform(t))
        rejectReturnValue(roleName, returnValue, t)
        Some(verifiedRoleName(roleName, 1) -> roleMethods(roleName, roleBody))

      // Methodless role
      // `role foo()` or `role foo {}`
      case t@Apply(Select(Ident(TermName("role")), roleName), _) =>
        Some(verifiedRoleName(roleName, 2) -> Nil)


      // Reject role definitions in remaining Context code -----------------------------------------------

      // No remaining uses of `role` keyword
      case tree => rejectNestedRoleDefinitions(tree).transform(tree); None
    }

    // Map(unique role name -> list(role methods))
    roles.flatten.foldLeft(Map[String, List[String]]()) {
      (rs, r) => if (rs.keys.toList.contains(r._1)) abort(s"Can't define role `${r._1}` twice") else rs + r
    }
  }
}

object ContextAnalyzer {
  def inst(c: MacroContext)(ct: c.universe.Tree) = new {
    val c0: c.type  = c
    val ctxTemplate = ct
  } with ContextAnalyzer[c.type]

  def apply(c: MacroContext)(ct: c.universe.Tree) = inst(c)(ct)
}