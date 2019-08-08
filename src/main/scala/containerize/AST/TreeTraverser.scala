package containerize.AST

import containerize.main.Containerize
import containerize.options.Options
import loci.PeerType
import loci.container.ContainerEntryPointImpl

import scala.collection.mutable
import scala.reflect.io.AbstractFile
import scala.tools.nsc.Global

class TreeTraverser[G <: Global](val global : Global, val plugin : Containerize) {

  import global._
  import plugin._

  type TAbstractClassDef = AbstractClassDef[plugin.global.Type, plugin.global.TypeName, plugin.global.Symbol]

  //todo ousource
  private val PeerType = typeOf[loci.Peer]

  def traverse[T >: Tree](tree : T) : Unit = {
    TreeTraverser.traverse(tree.asInstanceOf[this.global.Tree])
  }
  implicit def pClassSymbolConvert(c : this.global.ClassSymbol) : plugin.global.ClassSymbol = c.asInstanceOf[plugin.global.ClassSymbol]
  val dependencies : () => Unit = TreeTraverser.dependencies

  def parentHasContainerizeAnnot(c : Symbol) : Boolean = {
    val parentClass = if(c.isClass) c.safeOwner.enclClass else c.enclClass

    parentClass != null && parentClass != NoSymbol &&
      (parentClass.baseClasses.exists(_.tpe =:= typeOf[loci.container.Containerized]) || parentHasContainerizeAnnot(parentClass))
  }/**
  def getEntryPointBaseClass(c : Symbol) : Symbol = {
    val parentClass = if(c.isClass) c.safeOwner.enclClass else c.enclClass

    if(parentClass != null && parentClass != NoSymbol)
      if(global.cleanup.getEntryPoints.contains(parentClass.fullNameString)) parentClass else getEntryPointBaseClass(parentClass)

    NoSymbol
  }*/
  def transitiveSelect(s : Tree) : Symbol = s match{
    case Apply(fun, _) => transitiveSelect(fun)
    case s @ Select(qual, name) => qual match{
      case This(_) => s.symbol.tpe.typeSymbol
      case s @ _ => transitiveSelect(s)
    }
    case _ => NoSymbol
  }
  def getEnclClass(t : Tree) : ClassSymbol = (if(t.symbol != null && t.symbol.enclClass != null) t.symbol.enclClass else NoSymbol).asClass
  def getEntryPointsKeyBySubclass(subClass : ClassSymbol): Symbol = {
    EntryPointsImpls.keys.collectFirst{case c if subClass.isSubClass(c) => c }.getOrElse(NoSymbol).asInstanceOf[global.Symbol]
  }
  def getOrUpdateEntryPointsImpl(className : ClassSymbol): TEntryPointImplDef = {
    //todo better
    EntryPointsImpls.getOrElseUpdate(className, new ContainerEntryPointImpl(global)())
  }
  def updateEntryPointsImpl(className : ClassSymbol, cep : TEntryPointImplDef) : Unit = {
    //todo better
    EntryPointsImpls.update(className, cep)
  }

  private object TreeTraverser extends Traverser{

    //todo dependencies in other packages are not included
    //todo parent peer merken ?
    // todo kann man peer defs schachteln? glaub nich...

    override def traverse(tree: Tree): Unit = tree match {

      case _ : ModuleDef => //


/**
      case t @ Typed(Block(_, a), _) =>
        a match{
        case Apply(fun, args) =>
          fun match{
          case se @ Select(_, TermName(n)) =>
            if(n =="run"){
              var peer = args.map(x => x.find({
                case Select(_, TermName(n)) => n == "peerTypeTag"
                case _ => false
              }).getOrElse(NoSymbol)).collectFirst[Symbol]({ case s : Select => s.qualifier.symbol }).getOrElse(NoSymbol)

              if(peer != NoSymbol){
                val cep = getOrUpdateEntryPointsImpl(t)
                cep._containerPeerClass = peer.asInstanceOf[cep.global.Symbol]
                cep._containerEntryClass = getEnclClass(t).asInstanceOf[cep.global.ClassSymbol]
                updateEntryPointsImpl(t, cep)
              }
            }
          case _ =>
        }
        case _ =>
      }
*/
        /**
      case a @ DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
//todo support multiple listen/connects inside peer
        if(name.toTermName.startsWith("connect")){
          val (connectionPeer : Symbol, peerHost : String, peerPort : Integer) =
            rhs match{
            case Apply(fun, args) =>
              fun match{
                case Select(_, funName) =>
                  args match{
                    case (a: Apply) :: (b: Apply) :: Nil =>
                      val (host : String, port : Integer) = a match{
                        case Apply(_, args) =>
                          (args.collectFirst[String]({ case Literal(Constant(c : String)) => c }).getOrElse(Options.defaultContainerHost),
                            args.collectFirst[Integer]({ case Literal(Constant(c : Integer)) => c }).getOrElse(Options.defaultContainerPort))
                        case _ => (Options.defaultContainerHost, Options.defaultContainerPort)
                      }
                      b match{
                        case Apply(fun, _) => fun match{
                          case Select(s : Select, _) => (s.symbol.tpe.typeSymbol, host, port)
                          case _ => reporter.error(null, "XXX")
                        }
                        case _ => reporter.error(null, "XXX")
                      }
                    case _ => (NoSymbol, Options.defaultContainerHost, Options.defaultContainerPort)
                  }
              }
          }

          val cep = getOrUpdateEntryPointsImpl(getEnclClass(a))
          cep.addEndPoint(cep.ConnectionEndPoint(connectionPeer.asInstanceOf[cep.global.Symbol], peerPort, ""))
          updateEntryPointsImpl(getEnclClass(a), cep)
        }
          */
        /**
        Apply(Select(This(TypeName("$anon$1")), TermName("listen")),
          List(
            Apply(Select(Select(Select(Select(Ident(loci), loci.communicator), loci.communicator.tcp), loci.communicator.tcp.TCP), TermName("apply")), List(Literal(Constant(43053)))),
            Apply(Select(Select(This(TypeName("TimeService")), interactive.timeservicesimple.TimeService.Client), TermName("peerTypeTag")), List())
          ))
        Apply(Select(This(TypeName("$anon$1")), TermName("connect")),
          List(
            Apply(Select(Select(Select(Select(Ident(loci), loci.communicator), loci.communicator.tcp), loci.communicator.tcp.TCP), TermName("apply")), List(Literal(Constant("0.0.0.0")), Literal(Constant(43053)))),
            Apply(Select(Select(This(TypeName("TimeService")), interactive.timeservicesimple.TimeService.Client), TermName("peerTypeTag")), List())
          ))
          */
/**
        Typed(
          Block(
            List(ValDef(Modifiers(), TermName("$$loci$peer"), TypeTree(), Block(List(), Apply(Select(New(TypeTree()), termNames.CONSTRUCTOR), List())))),
          Apply(
            Select(Select(Select(Ident(loci), loci.impl), loci.impl.Runtime), TermName("run")),
            List(
              Apply(Select(Ident(TermName("$$loci$peer")), TermName("Tie")), List()),
              Ident(TermName("$$loci$peer")),
              Apply(
                Select(
                  Apply(
                    Select(
                      Select(
                        Ident(interactive.timeservicesimple.TimeService), interactive.timeservicesimple.TimeService.Server),
                      TermName("peerTypeTag")),
                    List()),
                  TermName("peerType")),
                List()
              )
          )))*/
        /**
        List(
          Apply(Select(Ident(TermName("$$loci$peer")), TermName("Tie")), List()),
          Ident(TermName("$$loci$peer")),
          Apply(Select(Apply(Select(Select(Ident(interactive.timeservicesimple.TimeService), interactive.timeservicesimple.TimeService.Server), TermName("peerTypeTag")), List()), TermName("peerType")), List()),
          */
          /*
        val enclosingClass : ClassSymbol = (if(a.symbol != null && a.symbol.enclClass != null) a.symbol.enclClass else NoSymbol).asClass
        //or use owner, effective owner, safeowner, etc?

        reporter.warning(null, "obj:" + a.symbol.nameString + "class : " + a.symbol.enclClass.baseClasses + ":" +a.symbol.isOverride+":"+
          (if(a.symbol.overrides.nonEmpty) (a.symbol.overrides.head.safeOwner.tpe =:= typeOf[loci.container.ContainerEntryPoint]) else null))

        if(enclosingClass.baseClasses.exists(
            _.tpe =:= typeOf[loci.container.ContainerEntryPoint])
            && a.symbol.isOverridingSymbol
            && a.symbol.overrides.exists(o => o.enclClass != null
            && o.enclClass.tpe =:= typeOf[loci.container.ContainerEntryPoint])
          ){

          def transitiveSelect(s : Tree) : Symbol = s match{
            case Apply(fun, _) => transitiveSelect(fun)
            case s @ Select(qual, name) => qual match{
              case This(_) => s.symbol.tpe.typeSymbol
              case s @ _ => transitiveSelect(s)
            }
            case _ => NoSymbol
          }

          name match{
            case TermName(n) =>
              n.trim match{
                case "containerPort" => reporter.warning(null, "match on : " + "containerPort")
                  rhs match{
                    case Apply(_, args) =>
                      args.head match {
                        case Literal(lit) => lit match {
                          case Constant(c : Integer) =>
                            cep._containerPort =
                              if (0 >= c && c <= 65535)
                                c
                              else {
                                reporter.warning(null, s"invalid port declaration (<0 or >65535), reset to default port ${Options.defaultContainerPort}.")
                                Options.defaultContainerPort
                              }
                          //todo how to safe cast + test if indirect set via obj
                          case _ => reporter.warning(null, s"invalid port declaration (try setting port using constant instead of ref), reset to default port ${Options.defaultContainerPort}.")
                        }
                      }
                  }
                case "containerPeer" => reporter.info(null, showRaw(rhs), true)
                  rhs match{
                    case Apply(_, args) => cep._containerPeer = transitiveSelect(args.head).asInstanceOf[cep.global.Symbol]
                  }
                case "containerVersion" =>
                  rhs match{
                    case Literal(lit) => lit match{
                      case Constant(c) =>
                        cep._containerVersion = c.asInstanceOf[String] //todo how to safe cast
                    }
                  }

                case _ => reporter.warning(null, "unknown")
              }
          }


          EntryPointsImpls.update(enclosingClass, cep)
          //todo


        }*/


      case c @ ClassDef(_, className, _, impl: Template) => impl match {
        case Template(parents, _, body) =>

          val pars = parents.map(_.tpe)
          if(parentHasContainerizeAnnot(c.symbol) && pars.exists(_ =:= PeerType)){
            PeerDefs += new TAbstractClassDef(
              c.symbol.enclosingPackage.javaClassName,
              className.asInstanceOf[plugin.global.TypeName],
              c.symbol.asInstanceOf[plugin.global.Symbol],
              pars.map(_.asInstanceOf[plugin.global.Type])
            )
          }
          //todo

          /** this will be detected atuom. by start use etc listen connect
          if(pars.contains(typeOf[loci.container.ContainerEntryPoint])){

            reporter.warning(null, "EP MEMS: " + pars.find(_ =:= typeOf[loci.container.ContainerEntryPoint]).orNull.decls.filter(x => x.isOverridableMember).map(x => x.overridingSymbol(c.symbol)).toString)
            EntryPointsClasses.put(aClassDef.classSymbol.asClass, null)
            reporter.warning(null, "ENTRY P CODE : " + t.toString)

          }*/

          //todo subclasses are traversed multiple times
          //todo find characteristic to identify entry points loci: ClassDef (Modifiers (MODULE), interactive.timeservice.Server, List (), Template (List (TypeTree (), TypeTree () ), noSelfType, List (DefDef (Modifiers (OVERRIDE | METHOD | STABLE | ACCESSOR), TermName ("executionStart"), List (), List (List () ), TypeTree (), Typed (Select (This (TypeName ("Server") ), TermName ("executionStart ") ), TypeTree () ) ), ValDef (Modifiers (PRIVATE | LOCAL | TRIEDCOOKING), TermName ("executionStart "), TypeTree (), EmptyTree), DefDef (Modifiers (OVERRIDE | METHOD | ACCESSOR | EXPANDEDNAME | DEFAULTINIT | NOTPRIVATE), TermName ("scala$App$$_args"), List (), List (List () ), TypeTree (), Typed (Select (This (TypeName ("Server") ), TermName ("scala$App$$_args ") ), TypeTree () ) ), ValDef (Modifiers (PRIVATE | MUTABLE | LOCAL | DEFAULTINIT | TRIEDCOOKING), TermName ("scala$App$$_args "), TypeTree (), EmptyTree), DefDef (Modifiers (OVERRIDE | METHOD | ACCESSOR | EXPANDEDNAME | DEFAULTINIT | NOTPRIVATE), TermName ("scala$App$$_args_$eq"), List (), List (List (ValDef (Modifiers (PARAM | SYNTHETIC), TermName ("x$1"), TypeTree (), EmptyTree) ) ), TypeTree (), Assign (Select (This (TypeName ("Server") ), TermName ("scala$App$$_args ") ), Typed (Ident (TermName ("x$1") ), TypeTree () ) ) ), DefDef (Modifiers (OVERRIDE | METHOD | STABLE | ACCESSOR | EXPANDEDNAME | NOTPRIVATE), TermName ("scala$App$$initCode"), List (), List (List () ), TypeTree (), Typed (Select (This (TypeName ("Server") ), TermName ("scala$App$$initCode ") ), TypeTree () ) ), ValDef (Modifiers (PRIVATE | LOCAL | TRIEDCOOKING), TermName ("scala$App$$initCode "), TypeTree (), EmptyTree), DefDef (Modifiers (PROTECTED | OVERRIDE | METHOD | MUTABLE | LOCAL | ACCESSOR), TermName ("scala$App$_setter_$executionStart_$eq"), List (), List (List (ValDef (Modifiers (PARAM | SYNTHETIC), TermName ("x$1"), TypeTree (), EmptyTree) ) ), TypeTree (), Assign (Select (This (TypeName ("Server") ), TermName ("executionStart ") ), Typed (Ident (TermName ("x$1") ), TypeTree () ) ) ), DefDef (Modifiers (PROTECTED | OVERRIDE | FINAL | METHOD | MUTABLE | LOCAL | ACCESSOR | EXPANDEDNAME | NOTPRIVATE), TermName ("scala$App$_setter_$scala$App$$initCode_$eq"), List (), List (List (ValDef (Modifiers (PARAM | SYNTHETIC), TermName ("x$1"), TypeTree (), EmptyTree) ) ), TypeTree (), Assign (Select (This (TypeName ("Server") ), TermName ("scala$App$$initCode ") ), Typed (Ident (TermName ("x$1") ), TypeTree () ) ) ), DefDef (Modifiers (), termNames.CONSTRUCTOR, List (), List (List () ), TypeTree (), Block (List (Apply (Select (Super (This (TypeName ("Server") ), typeNames.EMPTY), termNames.CONSTRUCTOR), List () ), Apply (Select (Super (This (TypeName ("Server") ), typeNames.EMPTY), TermName ("$init$") ), List () ) ), Literal (Constant (() ) ) ) ), Typed (Block (List (ValDef (Modifiers (), TermName ("$$loci$peer"), TypeTree (), Block (List (), Apply (Select (New (TypeTree () ), termNames.CONSTRUCTOR), List () ) ) ) ), Apply (Select (Select (Select (Ident (loci), loci.impl), loci.impl.Runtime), TermName ("run") ), List (Apply (Select (Ident (TermName ("$$loci$peer") ), TermName ("Tie") ), List () ), Ident (TermName ("$$loci$peer") ), Apply (Select (Apply (Select (Select (Ident (interactive.timeservice.TimeService), interactive.timeservice.TimeService.Server), TermName ("peerTypeTag") ), List () ), TermName ("peerType") ), List () ), Block (List (), Function (List (ValDef (Modifiers (PARAM), TermName ("context"), TypeTree (), EmptyTree), ValDef (Modifiers (PARAM), TermName ("connections"), TypeTree (), EmptyTree), ValDef (Modifiers (PARAM), TermName ("connected"), TypeTree (), EmptyTree), ValDef (Modifiers (PARAM), TermName ("connecting"), TypeTree (), EmptyTree) ), Apply (Select (This (TypeName ("Server") ), TermName ("$anonfun$new$1") ), List (Ident (TermName ("$$loci$peer") ), Ident (TermName ("context") ), Ident (TermName ("connections") ), Ident (TermName ("connected") ), Ident (TermName ("connecting") ) ) ) ) ) ) ) ), TypeTree () ), ClassDef (Modifiers (FINAL), interactive.timeservice.Server.$anon$1, List (), Template (List (TypeTree (), TypeTree () ), noSelfType, List (ValDef (Modifiers (PRIVATE | FINAL | MUTABLE | LOCAL | SYNTHETIC | MODULEVAR), TermName ("Default$module"), TypeTree (), EmptyTree), DefDef (Modifiers (METHOD | MODULE | STABLE), TermName ("Default"), List (), List (List () ), TypeTree (), Block (List (If (Apply (Select (Select (This (TypeName ("$anon$1") ), TermName ("Default$module") ), TermName ("eq") ), List (Literal (Constant (null) ) ) ), Apply (Select (This (TypeName ("$anon$1") ), TermName ("Default$lzycompute$1") ), List () ), EmptyTree) ), Typed (Select (This (TypeName ("$anon$1") ), TermName ("Default$module") ), TypeTree () ) ) ), ValDef (Modifiers (PRIVATE | FINAL | MUTABLE | LOCAL | SYNTHETIC | MODULEVAR), TermName ("ConnectionSetup$module"), TypeTree (), EmptyTree), DefDef (Modifiers (METHOD | MODULE | STABLE), TermName ("ConnectionSetup"), List (), List (List () ), TypeTree (), Block (List (If (Apply (Select (Select (This (TypeName ("$anon$1") ), TermName ("ConnectionSetup$module") ), TermName ("eq") ), List (Literal (Constant (null) ) ) ), Apply (Select (This (TypeName ("$anon$1") ), TermName ("ConnectionSetup$lzycompute$1") ), List () ), EmptyTree) ), Typed (Select (This (TypeName ("$anon$1") ), TermName ("ConnectionSetup$module") ), TypeTree () ) ) ), ValDef (Modifiers (PRIVATE | FINAL | MUTABLE | LOCAL | SYNTHETIC | MODULEVAR), TermName ("FactorySetup$module"), TypeTree (), EmptyTree), DefDef (Modifiers (METHOD | MODULE | STABLE), TermName ("FactorySetup"), List (), List (List () ), TypeTree (), Block (List (If (Apply (Select (Select (This (TypeName ("$anon$1") ), TermName ("FactorySetup$module") ), TermName ("eq") ), List (Literal (Constant (null) ) ) ), Apply (Select (This (TypeName ("$anon$1") ), TermName ("FactorySetup$lzycompute$1") ), List () ), EmptyTree) ), Typed (Select (This (TypeName ("$anon$1") ), TermName ("FactorySetup$module") ), TypeTree () ) ) ), DefDef (Modifiers (), termNames.CONSTRUCTOR, List (), List (List () ), TypeTree (), Block (List (Apply (Select (Super (This (TypeName ("$anon$1") ), typeNames.EMPTY), termNames.CONSTRUCTOR), List () ), Apply (Select (Super (This (TypeName ("$anon$1") ), typeNames.EMPTY), TermName ("$init$") ), List () ), Apply (Select (Super (This (TypeName ("$anon$1") ), typeNames.EMPTY), TermName ("$init$") ), List () ) ), Literal (Constant (() ) ) ) ), DefDef (Modifiers (), TermName ("connect"), List (), List (List () ), TypeTree (), Apply (Select (This (TypeName ("$anon$1") ), TermName ("listen") ), List (Apply (Select (Select (Select (Select (Ident (loci), loci.communicator), loci.communicator.tcp), loci.communicator.tcp.TCP), TermName ("apply") ), List (Literal (Constant (806) ) ) ), Apply (Select (Select (This (TypeName ("TimeService") ), interactive.timeservice.TimeService.Client), TermName ("peerTypeTag") ), List () ) ) ) ), DefDef (Modifiers (), TermName ("Default$lzycompute$1"), List (), List (List () ), TypeTree (), Apply (TypeApply (Select (This (TypeName ("$anon$1") ), TermName ("synchronized") ), List (TypeTree () ) ), List (If (Apply (Select (Select (This (TypeName ("$anon$1") ), TermName ("Default$module") ), TermName ("eq") ), List (Literal (Constant (null) ) ) ), Assign (Select (This (TypeName ("$anon$1") ), TermName ("Default$module") ), Apply (Select (New (TypeTree () ), termNames.CONSTRUCTOR), List (This (TypeName ("$anon$1") ) ) ) ), EmptyTree) ) ) ), DefDef (Modifiers (), TermName ("ConnectionSetup$lzycompute$1"), List (), List (List () ), TypeTree (), Apply (TypeApply (Select (This (TypeName ("$anon$1") ), TermName ("synchronized") ), List (TypeTree () ) ), List (If (Apply (Select (Select (This (TypeName ("$anon$1") ), TermName ("ConnectionSetup$module") ), TermName ("eq") ), List (Literal (Constant (null) ) ) ), Assign (Select (This (TypeName ("$anon$1") ), TermName ("ConnectionSetup$module") ), Apply (Select (New (TypeTree () ), termNames.CONSTRUCTOR), List (This (TypeName ("$anon$1") ) ) ) ), EmptyTree) ) ) ), DefDef (Modifiers (), TermName ("FactorySetup$lzycompute$1"), List (), List (List () ), TypeTree (), Apply (TypeApply (Select (This (TypeName ("$anon$1") ), TermName ("synchronized") ), List (TypeTree () ) ), List (If (Apply (Select (Select (This (TypeName ("$anon$1") ), TermName ("FactorySetup$module") ), TermName ("eq") ), List (Literal (Constant (null) ) ) ), Assign (Select (This (TypeName ("$anon$1") ), TermName ("FactorySetup$module") ), Apply (Select (New (TypeTree () ), termNames.CONSTRUCTOR), List (This (TypeName ("$anon$1") ) ) ) ), EmptyTree) ) ) ) ) ) ), ClassDef (Modifiers (FINAL), interactive.timeservice.Server.$anon$2, List (), Template (List (TypeTree (), TypeTree () ), noSelfType, List (ValDef (Modifiers (PRIVATE | FINAL | MUTABLE | LOCAL | SYNTHETIC | LAZY), TermName ("$$loci$system "), TypeTree (), EmptyTree), DefDef (Modifiers (OVERRIDE | METHOD | STABLE | ACCESSOR | TRIEDCOOKING), TermName ("time"), List (), List (List () ), TypeTree (), Typed (Select (This (TypeName ("$anon$2") ), TermName ("time ") ), TypeTree () ) ), ValDef (Modifiers (PRIVATE | LOCAL | TRIEDCOOKING), TermName ("time "), TypeTree (), EmptyTree), DefDef (Modifiers (PROTECTED | OVERRIDE | METHOD | MUTABLE | LOCAL | ACCESSOR | TRIEDCOOKING), TermName ("interactive$timeservice$TimeService$Server$$$loci$peer$_setter_$time_$eq"), List (), List (List (ValDef (Modifiers (PARAM | SYNTHETIC), TermName ("x$1"), TypeTree (), EmptyTree) ) ), TypeTree (), Assign (Select (This (TypeName ("$anon$2") ), TermName ("time ") ), Typed (Ident (TermName ("x$1") ), TypeTree () ) ) ), ValDef (Modifiers (PRIVATE | MUTABLE | LOCAL), TermName ("bitmap$0"), TypeTree (), EmptyTree), DefDef (Modifiers (), termNames.CONSTRUCTOR, List (), List (List (ValDef (Modifiers (PRIVATE | PARAM | LOCAL | SYNTHETIC), TermName ("$$loci$peer$1"), TypeTree (), EmptyTree), ValDef (Modifiers (PRIVATE | PARAM | LOCAL | SYNTHETIC), TermName ("context$1"), TypeTree (), EmptyTree), ValDef (Modifiers (PRIVATE | PARAM | LOCAL | SYNTHETIC), TermName ("connections$1"), TypeTree (), EmptyTree), ValDef (Modifiers (PRIVATE | PARAM | LOCAL | SYNTHETIC), TermName ("connected$1"), TypeTree (), EmptyTree), ValDef (Modifiers (PRIVATE | PARAM | LOCAL | SYNTHETIC), TermName ("connecting$1"), TypeTree (), EmptyTree) ) ), TypeTree (), Block (List (Apply (Select (Super (This (TypeName ("$anon$2") ), typeNames.EMPTY), termNames.CONSTRUCTOR), List () ), Apply (Select (Super (This (TypeName ("$anon$2") ), typeNames.EMPTY), TermName ("$init$") ), List () ), Apply (Select (Super (This (TypeName ("$anon$2") ), typeNames.EMPTY), TermName ("$init$") ), List () ) ), Literal (Constant (() ) ) ) ), DefDef (Modifiers (OVERRIDE), TermName ("$$loci$metapeer"), List (), List (List () ), TypeTree (), Select (This (TypeName ("$anon$2") ), TermName ("$$loci$peer$1") ) ), DefDef (Modifiers (PRIVATE | METHOD), TermName ("$$loci$system$lzycompute"), List (), List (List () ), TypeTree (), Block (List (Apply (TypeApply (Select (This (TypeName ("$anon$2") ), TermName ("synchronized") ), List (TypeTree () ) ), List (If (Apply (Select (Select (This (TypeName ("$anon$2") ), TermName ("bitmap$0") ), TermName ("unary_$bang") ), List () ), Block (List (Assign (Select (This (TypeName ("$anon$2") ), TermName ("$$loci$system ") ), Typed (Apply (Select (New (TypeTree () ), termNames.CONSTRUCTOR), List (Select (This (TypeName ("$anon$2") ), TermName ("context$1") ), Select (This (TypeName ("$anon$2") ), TermName ("connections$1") ), Select (This (TypeName ("$anon$2") ), TermName ("connected$1") ), Select (This (TypeName ("$anon$2") ), TermName ("connecting$1") ), Apply (Select (Select (This (TypeName ("impl") ), loci.impl.PeerImpl), TermName ("Ops") ), List (This (TypeName ("$anon$2") ) ) ) ) ), TypeTree () ) ) ), Assign (Select (This (TypeName ("$anon$2") ), TermName ("bitmap$0") ), Literal (Constant (true) ) ) ), EmptyTree) ) ) ), Select (This (TypeName ("$anon$2") ), TermName ("$$loci$system ") ) ) ), DefDef (Modifiers (OVERRIDE | METHOD | STABLE | ACCESSOR | LAZY | TRIEDCOOKING), TermName ("$$loci$system"), List (), List (List () ), TypeTree (), If (Apply (Select (Select (This (TypeName ("$anon$2") ), TermName ("bitmap$0") ), TermName ("unary_$bang") ), List () ), Apply (Select (This (TypeName ("$anon$2") ), TermName ("$$loci$system$lzycompute") ), List () ), Select (This (TypeName ("$anon$2") ), TermName ("$$loci$system ") ) ) ), DefDef (Modifiers (OVERRIDE | METHOD | BRIDGE | ARTIFACT), TermName ("$$loci$metapeer"), List (), List (List () ), TypeTree (), Apply (Select (This (TypeName ("$anon$2") ), TermName ("$$loci$metapeer") ), List () ) ), DefDef (Modifiers (OVERRIDE | METHOD | BRIDGE | ARTIFACT), TermName ("$$loci$metapeer"), List (), List (List () ), TypeTree (), Apply (Select (This (TypeName ("$anon$2") ), TermName ("$$loci$metapeer") ), List () ) ), ValDef (Modifiers (PRIVATE | LOCAL | SYNTHETIC | PARAMACCESSOR), TermName ("$$loci$peer$1"), TypeTree (), EmptyTree), ValDef (Modifiers (PRIVATE | LOCAL | SYNTHETIC | PARAMACCESSOR), TermName ("context$1"), TypeTree (), EmptyTree), ValDef (Modifiers (PRIVATE | LOCAL | SYNTHETIC | PARAMACCESSOR), TermName ("connections$1"), TypeTree (), EmptyTree), ValDef (Modifiers (PRIVATE | LOCAL | SYNTHETIC | PARAMACCESSOR), TermName ("connected$1"), TypeTree (), EmptyTree), ValDef (Modifiers (PRIVATE | LOCAL | SYNTHETIC | PARAMACCESSOR), TermName ("connecting$1"), TypeTree (), EmptyTree) ) ) ), DefDef (Modifiers (FINAL | METHOD | ARTIFACT), TermName ("$anonfun$new$1"), List (), List (List (ValDef (Modifiers (PRIVATE | PARAM | LOCAL | SYNTHETIC), TermName ("$$loci$peer$1"), TypeTree (), EmptyTree), ValDef (Modifiers (PARAM | SYNTHETIC), TermName ("context"), TypeTree (), EmptyTree), ValDef (Modifiers (PARAM | SYNTHETIC), TermName ("connections"), TypeTree (), EmptyTree), ValDef (Modifiers (PARAM | SYNTHETIC), TermName ("connected"), TypeTree (), EmptyTree), ValDef (Modifiers (PARAM | SYNTHETIC), TermName ("connecting"), TypeTree (), EmptyTree) ) ), TypeTree (), Apply (Select (Apply (Select (Block (List (), Apply (Select (New (TypeTree () ), termNames.CONSTRUCTOR), List (Ident (TermName ("$$loci$peer$1") ), Ident (TermName ("context") ), Ident (TermName ("connections") ), Ident (TermName ("connected") ), Ident (TermName ("connecting") ) ) ) ), TermName ("$$loci$system") ), List () ), TermName ("main") ), List () ) ) ) ) )
          //todo make sure only in entrypoints (sonst werden au nested classes erkannt, etc)

          //todo simple examples work, batch examples not!!
          //todo this check is probably ok, but not sufficient. test anyhow

          if(!c.symbol.isSynthetic && !c.symbol.isAnonymousClass /**global.cleanup.getEntryPoints.contains(c.symbol.fullNameString)*/) {

            val cep = getOrUpdateEntryPointsImpl(c.symbol.asClass)

            val trees =
              body.map(b => b.filter({
                case Apply(fun, _) => fun match {
                  case Select(_, TermName(n)) => n == "run";
                  case _ => false
                }
                case _ => false
              })).flatten.map(a => a.filter({
                case Select(_, TermName(n)) => n == "peerTypeTag"
                case _ => false
              })).flatten.map({
                case s@Select(qual, _) => qual
              })
            trees.foreach({
              case s: Select =>
                cep._containerPeerClass = s.symbol.asInstanceOf[cep.global.Symbol]
                cep._containerEntryClass = c.symbol.asInstanceOf[cep.global.Symbol]
            })

            val trees2 =
              body.map(b => b.filter({
                case _ : ClassDef => true
                case _ => false
              })).flatten.map({ case c : ClassDef => c.impl.body }).map(a => a.filter({
                case d : DefDef => d.name.toString.startsWith("connect") || d.name.toString.startsWith("listen")
                case _ => false
              })).flatten

            trees2.foreach({
              case d : DefDef => d.rhs match{
                case Apply(conFun, args) =>
                  args match{
                    case (a: Apply) :: (b: Apply) :: Nil =>
                      val (host : String, port : Integer) =
                        a match{
                          case Apply(_, args) =>
                            (args.collectFirst[String]({ case Literal(Constant(c : String)) => c }).getOrElse(Options.defaultContainerHost),
                              args.collectFirst[Integer]({ case Literal(Constant(c : Integer)) => c }).getOrElse(Options.defaultContainerPort))
                          case _ => (Options.defaultContainerHost, Options.defaultContainerPort)
                        }
                      val connectionPeer : Symbol =
                        b match{
                          case Apply(fun, _) => fun match{
                            case Select(s : Select, _) => s.symbol.tpe.typeSymbol
                            case _ => reporter.error(null, "XXX"); NoSymbol
                          }
                          case _ => reporter.error(null, "XXX"); NoSymbol
                        }
                      cep.addEndPoint(cep.ConnectionEndPoint(connectionPeer.asInstanceOf[cep.global.TypeSymbol], port, host, conFun.symbol.simpleName.toString))
                    case _ =>
                  }
                case _ =>
              }
              case _ =>
            })

            updateEntryPointsImpl(c.symbol.asClass, cep)
          }
          body.foreach(traverse)
      }
      case _ : Import => //
      case _ => super.traverse(tree)
    }

    def dependencies() : Unit = {

      EntryPointsImpls = EntryPointsImpls.filter(e => e._2.entryClassDefined && e._2.peerClassDefined)

      EntryPointsImpls = EntryPointsImpls.filter(e => PeerDefs.exists(p => toolbox.weakSymbolCompare(e._2._containerPeerClass.asInstanceOf[plugin.global.Symbol], p.classSymbol) ))

      EntryPointsImpls.toList.foreach(x => reporter.info(null, "ENTRYSS: " + x._1.fullNameString + " - " + x._2._containerEntryClass.fullNameString + " - " + x._2._containerPeerClass.fullNameString + " :: " + x._2._containerEndPoints.foldLeft("")((e,d) => e + "||" + d.connectionPeer + "&" + d.port + "&" + d.version + "&" + d.way).toString, true))

      /**
      EntryPoints = PeerDefs.map{
        p => {
          val entryPoint = EntryPointsImpls.find(k => k._2. ).orNull
          if(entryPoint == null || entryPoint._containerPeer == NoSymbol)
            reporter.error(null, s"no associated peer found for entry point: ${k._1.fullNameString}, object must at least override loci.container.ContainerEntryPoint.containerPeer")
          entryPoint._containerEntryClass = k._1.asInstanceOf[entryPoint.global.Symbol]
          (k._1 -> entryPoint)
        }
      }*/

      /**
      EntryPointsClasses.foreach{ e =>
        if(!PeerClassDefs.exists(_.classSymbol.fullName == e._2._containerPeer.fullName))
          reporter.error(null, s"Couldn't find associated peer class ${ e._2._containerPeer } for entry point ${ e._1.fullName }.")
      }
      PeerClassDefs.foreach{ e =>
        if(!EntryPointsClasses.exists(_._2._containerPeer.fullName == e.classSymbol.fullName))
          reporter.error(null, s"No entry point found for peer class ${ e.classSymbol.fullName }, every defined Peer must have at least one entry point inheriting from trait loci.container.ContainerEntryPoint.")
      }
      */

      /**EntryPoints = EntryPointsClasses.map(k => k._2 -> PeerClassDefs.find(_.classSymbol.fullName == k._2._containerPeer.fullName).orNull)*/

      /*ClassDefs.filter(_.isPeer).map(p => {

        val fileDependencyList : List[TAbstractClassDef] = ClassDefs.toList
        /*
          ClassDefs.filter(x => x.isPeer && !x.equals(p)).foldLeft(ClassDefs)((list, x) =>
            list.filterNot(c => c.equals(x) || c.classSymbol.javaClassName.startsWith(x.classSymbol.javaClassName))
          ).toList
         */
        //.map(c => Paths.get(c.filePath.path).normalize)
        //todo ok? we could also do outputpath + c.symbol.javaBinaryNameString.toString
        // global.reporter.warning(null, fileList.map(_.toAbsolutePath).toString)

        p.copy(classFiles = fileDependencyList)
      })*/
    }
  }
}
