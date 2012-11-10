package scaladci
import scala.reflect.macros.{Context => MacroContext}

trait PlayingRole

trait Context {
  implicit protected def obj2binder[OBJ](obj: OBJ) = new Binder(obj)

  protected class Binder[OBJ](val obj: OBJ) {
    def as[ROLE] = macro Context.bind[OBJ, ROLE]
  }
}

object Context {
  type ROLEPLAYER = AnyRef

  def bind[OBJ: c.WeakTypeTag, ROLE: c.WeakTypeTag](c: MacroContext): c.Expr[ROLEPLAYER] = {
    import c.universe._

    // Constraint violations helper methods
    def stop(violators: List[String], msg: String) {
      if (!violators.isEmpty)
        c.abort(c.enclosingPosition, "Constraint violation:\n" + msg + "\n- " + violators.mkString("\n- ") +
          "\n### NOTE: IDE cursor will be where the macro call starts, not at the violation position!")
    }
    val ctxTree = c.enclosingClass
    val ctxMembers = ctxTree.symbol.asClass.typeSignature.members.sorted
    def contextBody = ctxTree match {case ClassDef(_, contextName, _, Template(_, _, contextBody)) => contextBody }

    // Enforce Context parameters to be immutable val's
    stop(ctxMembers.collect {
      case v: TermSymbol if v.isParamAccessor && v.isVar => v.name.toString
    }, "Please remove 'var' from the following Context parameters (only val's allowed):")

    /*
    * Recursively looping through the AST to find violating uses of incoming Data objects.
    *
    * The Data objects are supposed to be manipulated through the self reference in Roles
    * once the object has become a Role Player. If the Context also manipulates the Data
    * outside the Roles things can get messy and seems to be avoided.
    *
    * A first (naive) approach would be to try to stop any object manipulation and still
    * allow access to the data. But what types of manipulation can we expect? Is there a
    * way to "catch them all". It seems hacky. I kept this approach (in the code below)
    * just as an example of how we can recursively detect any tree structure at any
    * nested level in the source code.
    *
    * A better approach might be to allow incoming "custom objects" (AnyRef objects
    * created from classes - not Int, Boolean and other "constants") to be accessed only
    * once and only for a Role assignment...? TODO
    * */
    case class dataViolation(testTerm: TermName) extends Transformer {
      def abort(violator: String): Tree = {
        stop(List(violator),
          "Incoming Data objects are only allowed to be accessed or bound to Roles.\n" +
            "The following modifications to Data objects are not allowed:"
        )
        c.Expr[String](Literal(Constant("satisfying return type of transform method - we won't get here"))).tree
      }

      override def transform(tree: Tree): Tree = {
        tree match {
          case Assign(Select(x@Ident(obj), field), _) if obj == testTerm =>
            abort(s"Line ${x.pos.line}: Assigning a value to ${obj.toString}.${field.toString}")

          case Apply(Select(x@Ident(obj), method), _) if obj == testTerm =>
            abort(s"Line ${x.pos.line}: Calling ${obj.toString}.${method.toString}(...)")

          case Apply(TypeApply(Select(x@Ident(obj), method), _), _) if obj == testTerm =>
            abort(s"Line ${x.pos.line}: Calling ${obj.toString}.${method.toString}(...)")

          case ValDef(_, variable, TypeTree(), x@Ident(obj)) if obj == testTerm =>
            abort(s"Line ${x.pos.line}: Assigning Data objects to variables ($variable = ${obj.toString})")

          case _ => super.transform(tree) // recursive call into the "remaining" tree structure
        }
      }
    }
    ctxMembers.collect {
      case param: TermSymbol if param.isParamAccessor => dataViolation(newTermName(param.name.toString)).transform(ctxTree)
    }

    // Enforce val/var's in Context to be private
    stop(ctxMembers.collect {
      case v: TermSymbol if (v.isVal || v.isVar) && !v.isParamAccessor && (v.getter.isProtected || v.getter.isPublic) && v.getter.isTerm => v.name.toString
      //    }, "Please mark the following Context val/var's as private:")
    }, "Please mark the following Context val/var's as private:" + showRaw(ctxTree))

    // Enforce Role traits in Context to be private
    stop(ctxMembers.collect {
      case t: ClassSymbol if t.isTrait && !t.isPrivate => t.name.toString
    }, "Please mark the following Role traits as private:")

    // Prevent state in Roles
    stop(ctxMembers.collect {
      case t: ClassSymbol if {t.isTrait && !t.typeSignature.members.collect { case v: TermSymbol if (v.isVal || v.isVar) => v}.isEmpty} => t.name.toString
    }, "Please remove any state (val/var's) from the following Role traits:")

    // Prevent Role assignment in Role traits
    stop(contextBody.collect {
      case ClassDef(_, roleName, _, Template(_, _, roleBody)) if {
        """TypeApply\(Select\(Ident\(newTermName\(\"\w+\"\)\), newTermName\(\"as\"\)\)\, List\(Ident\(newTypeName\(\"\w+\"\)\)\)\)""".r.findFirstIn(showRaw(roleBody)).isDefined
      } => roleName.toString
    }, "Please move Role binding from the following Roles to the Context:")


    // Todo: Prevent that an object can play more than one Role at a time...

    // Bind Object to Role

    val objType = weakTypeOf[OBJ]
    val rolePlayerMarkerTrait = c.mirror.staticClass("scaladci.PlayingRole")
    val selfClasses = {
      if (objType.typeSymbol.asClass.isCaseClass) {
        // e.g. 'Account' (self)
        List(objType.typeSymbol.asClass)
      } else {
        val allClasses = objType.baseClasses.tail.takeWhile(_.name.toString != "Serializable").reverse
        if (allClasses.contains(rolePlayerMarkerTrait)) {
          // e.g. 'Account with Log with PlayingRole with Source' (Role Player)
          allClasses.takeWhile(_.name.toString != "PlayingRole")
        } else {
          // e.g. 'Account with Log' (self with traits)
          allClasses
        }
      }
    }
    val selfBaseClasses = objType.baseClasses.collect { case cl: ClassSymbol if cl.isClass => cl}
    if (selfBaseClasses.isEmpty) c.abort(c.enclosingPosition, s"Try compiling production classes first... ??")
    val selfBaseClass = selfBaseClasses.head

    val objParam = ValDef(NoMods, newTermName("obj"), TypeTree(selfBaseClass.toType), EmptyTree)
    val fieldNames = selfBaseClass.toType.members.sorted.collect { case x: TermSymbol if x.isCaseAccessor && x.isMethod => x.name}
    val superArgs = fieldNames map (fieldName => Select(Ident(objParam.name), fieldName))

    val selfIdentifiers = selfClasses map (selfClass => Ident(selfClass))
    val rolePlayerMarker = Ident(rolePlayerMarkerTrait)
    val roleIdentifier = Ident(weakTypeOf[ROLE].typeSymbol.asClass)
    val rolePlayerIdentifiers = selfIdentifiers ::: List(rolePlayerMarker, roleIdentifier)

    val anonymousClass = c.fresh("$anon")
    val rolePlayer = Block(
      ClassDef(Modifiers(Flag.FINAL), newTypeName(anonymousClass), List(), Template(rolePlayerIdentifiers, emptyValDef, List(
        DefDef(Modifiers(), nme.CONSTRUCTOR, List(), List(
          List(ValDef(Modifiers(), newTermName("obj"), TypeTree(selfBaseClass.toType), EmptyTree))), TypeTree(),
          Block(List(Apply(
            Select(Super(This(newTypeName("")), newTypeName("")), nme.CONSTRUCTOR), superArgs)),
            Literal(Constant(()))))))),
      New(Ident(newTypeName(anonymousClass)), List(List(Select(c.prefix.tree, newTermName("obj"))))))

//    c.abort(c.enclosingPosition, s"RESULT:\n${rolePlayer}\n${showRaw(rolePlayer)}")
    c.Expr(rolePlayer)
  }
}

/* Generates the following code for the Role Player 'Account with Source'
{
  final class $anon1 extends Account with PlayingRole with Source {
    def <init>(obj: scaladci.examples.MoneyTransfer2Test.Account) = {
      super.<init>(obj.acc, obj.balance);
      ()
    }
  };
  new $anon1(MoneyTransfer.this.obj2binder[scaladci.examples.MoneyTransfer2Test.Account](MoneyTransfer.this.acc1).obj)
}

AST:

Block(
  List(
    ClassDef(
      Modifiers(FINAL),
      newTypeName("$anon1"),
      List(),
      Template(
        List(
          Ident(scaladci.examples.MoneyTransfer2Test.Account),
          Ident(scaladci.PlayingRole),
          Ident(newTypeName("Source"))),
        emptyValDef,
        List(
          DefDef(
            Modifiers(),
            nme.CONSTRUCTOR,
            List(),
            List(List(ValDef(Modifiers(), newTermName("obj"), TypeTree(), EmptyTree))),
            TypeTree(),
            Block(
              List(Apply(
                Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR),
                List(Select(Ident(newTermName("obj")), newTermName("acc")), Select(Ident(newTermName("obj")), newTermName("balance"))))),
              Literal(Constant(())))))))),
  Apply(
    Select(New(Ident(newTypeName("$anon1"))), nme.CONSTRUCTOR),
    List(
      Select(
        Apply(
          TypeApply(Select(This(newTypeName("MoneyTransfer")), newTermName("obj2binder")), List(TypeTree())),
          List(Select(This(newTypeName("MoneyTransfer")), newTermName("acc1")))),
        newTermName("obj")))))

*/
