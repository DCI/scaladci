package scaladci
import scala.reflect.macros.{Context => MacroContext}
import scaladci.util.MacroHelper

trait ContextAnalyzer[C <: MacroContext] extends MacroHelper[C] {
  import c0.universe._, Flag._
  val ctxTemplate: Tree


  // Validate and organize Context AST

  abortRoleTemplate(ctxTemplate).transform(ctxTemplate)
  val body    = ctxTemplate.asInstanceOf[TemplateApi].body
  val objects = body.collect { case ValDef(_, name, _, _) => name}
  val roles   = roleDefinitions(body)

  case class abortRoleTemplate(tree0: Tree) extends Transformer {
    override def transform(tree: Tree): Tree = tree match {
      case Template(_, ValDef(_, TermName("role"), _, _), _) => abortRoleUse(tree0, "as a template name")
      case _                                                 => super.transform(tree)
    }
  }

  def isRoleMethod(roleName: String, methodName: String) =
    !roles.isEmpty && roles.contains(roleName) && roles(roleName).contains(methodName)

  def abortRoleUse(tree: Tree, msg: String, i: Int = 0) = {
    abort(s"Using `role` keyword $msg is not allowed.\nCODE: $tree\nAST: ${showRaw(tree)}", i)
  }

  case class noRoleKW(tree0: Tree) extends Transformer {
    def err(msg: String, i: Int = 0) = abortRoleUse(tree0, msg, i)
    override def transform(tree: Tree): Tree = tree match {
      case Apply(Select(Ident(TermName("role")), _), _) /* role Foo {...} */ => err("on a sub level of the Context", 1)
      case Apply(Ident(TermName("role")), _) /*            role(Foo)      */ => err("on a sub level of the Context", 2)
      case Apply(Apply(Ident(TermName("role")), _), _) /*  role(Foo){...} */ => err("on a sub level of the Context", 3)
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

    def roleMethods(roleName: Name, t: List[Tree]): List[String] = t collect {
      case DefDef(_, meth, _, _, _, _) if meth != nme.CONSTRUCTOR => meth.toString
      case tree                                                   => abort(s"Roles can only define methods. Found invalid code in the `$roleName` role:\n $tree")
    }

    val ctxObjects = ctxBody collect {
      case ValDef(_, objName, _, _) => objName
    }

    def verifiedRoleName(roleName: Name): String = {
      if (ctxObjects contains roleName) roleName.toString
      else abort(s"Defined role name `${roleName.toString}` has to match some object identifier in the Context. Available identifiers:\n" + ctxObjects.mkString("\n"))
    }
    lazy val missingRoleName = "`role` keyword without Role name is not allowed"

    val roles: List[Option[(String, List[String])]] = ctxBody map {

      // Rejected role definitions ----------------------------------------------------------------------

      ///////// `role` as keyword /////////

      // role Foo
      case t@Select(Ident(TermName("role")), roleName) =>
        abort(s"(1) To avoid postfix clashes, please write `role $roleName {}` instead of `role $roleName`")

      /*
        role Foo // two lines after each other ...
        role Bar // ... unintentionally becomes `role.Foo(role).Bar`
      */
      case t@Select(Apply(Select(Ident(TermName("role")), roleName), List(Ident(TermName("role")))), roleName2) =>
        abort(s"(2) To avoid postfix clashes, please write `role $roleName {}` instead of `role $roleName`")

      // role
      case Ident(TermName("role")) => abort(missingRoleName, 1)

      ///////// `role` as method /////////

      // role()
      // role{}
      case Apply(Ident(TermName("role")), List())                     => abort(missingRoleName, 2)
      case Apply(Ident(TermName("role")), List(Literal(Constant(_)))) => abort(missingRoleName, 3)

      // role()()
      // role(){}
      // role{}()
      // role{}{}
      case Apply(Apply(Ident(TermName("role")), List()), List())                                         => abort(missingRoleName, 4)
      case Apply(Apply(Ident(TermName("role")), List()), List(Literal(Constant(_))))                     => abort(missingRoleName, 5)
      case Apply(Apply(Ident(TermName("role")), List(Literal(Constant(_)))), List())                     => abort(missingRoleName, 6)
      case Apply(Apply(Ident(TermName("role")), List(Literal(Constant(_)))), List(Literal(Constant(_)))) => abort(missingRoleName, 7)


      // Approved role definitions ----------------------------------------------------------------------

      ///////// `role` as keyword /////////

      // role Foo {...}
      case t@Apply(Select(Ident(TermName("role")), roleName), List(Block(roleBody, _))) =>
        // Reject nested role definitions
        roleBody.foreach(t => new noRoleKW(t).transform(t))
        Some(verifiedRoleName(roleName) -> roleMethods(roleName, roleBody))

      // Methodless role
      // `role Foo()` or `role Foo {}`
      case t@Apply(Select(Ident(TermName("role")), roleName), _) =>
        Some(verifiedRoleName(roleName) -> Nil)


      ///////// `role` as method /////////

      // role(Foo) {...}
      case t@Apply(Apply(Ident(TermName("role")), List(Ident(roleName))), List(Block(roleBody, _))) =>
        // Reject nested role definitions
        roleBody.foreach(t => new noRoleKW(t).transform(t))
        Some(verifiedRoleName(roleName) -> roleMethods(roleName, roleBody))

      // Methodless roles
      // `role(Foo)()` or `role(Foo){}`
      case t@Apply(Apply(Ident(TermName("role")), List(Ident(roleName))), _) =>
        Some(verifiedRoleName(roleName) -> Nil)

      // role(Foo)
      case t@Apply(Ident(TermName("role")), List(Ident(roleName))) =>
        Some(verifiedRoleName(roleName) -> Nil)


      // Reject role definitions in remaining Context code -----------------------------------------------

      // No remaining uses of `role` keyword
      case tree => noRoleKW(tree).transform(tree); None
    }

    // Role method names for each unique role name
    roles.flatten.foldLeft(Map[String, List[String]]()) {
      (rs, r) => if (rs.keys.toList.contains(r._1)) abort(s"Can't define role `${r._1}` twice.") else rs + r
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