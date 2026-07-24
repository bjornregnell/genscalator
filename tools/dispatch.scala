// dispatch — the SINGLE DISPATCHER (SM178 tt-graalify; unparks the parked one-@main design; grew
// from and supersedes the SM146/T4 monolith-probe seed): ONE entry point mapping every tool verb
// (= tool FILE stem, the launcher's contract) to its entry function, so the whole toolbox can ship
// as ONE native image while `tt <tool> <args...>` behaviour stays unchanged.
//
// The `entries` table is the ONE HOME of the verb->entry mapping: usage derives from it, and
// DispatchSuite asserts it covers exactly the tools/*.scala files with a top-level @main, so the
// table cannot drift from the file set unnoticed.
//
// NB: this file compiles only as part of the WHOLE-toolbox unit (scala-cli compile/test/package
// tools) — it is not itself a `tt` verb. Native-image entry point: --main-class dispatchTypedTools.
// Tools that locate <tools>/.. via -Dtt.tools (lib.scala) keep working on a native image: graal
// binaries parse -D runtime args; without it the cwd walk-up fallback applies as today.
object Dispatch {

  type Verb = String

  /** verb -> entry; verbs are the tool FILE stems that `tt <tool>` accepts today. */
  val entries: Vector[(Verb, Seq[String] => Unit)] = Vector(
    "ascii"       -> (a => renderAsciiDiagram(a*)),
    "bloop"       -> (a => bloopServerCtl(a*)),
    "box"         -> (a => boxOps(a*)),
    "chrono"      -> (a => chronoStopwatch(a*)),
    "doc"         -> (a => doc(a*)),
    "files"       -> (a => files(a*)),
    "find"        -> (a => find(a*)),
    "forge"       -> (a => forgeClient(a*)),
    "git"         -> (a => gitCommitPush(a*)),
    "gitinfo"     -> (a => gitInfoOverview(a*)),
    "guardcheck"  -> (a => checkGuardPatterns(a*)),
    "gvdot"       -> (a => renderGraphvizDiagram(a*)),
    "hangover"    -> (a => hangoverDetect(a*)),
    "harden"      -> (a => hardenScan(a*)),
    "htmltext"    -> (a => htmltext(a*)),
    "log"         -> (a => logAnalyze(a*)),
    "md-fmt"      -> (a => formatMarkdown(a*)),
    "mode"        -> (a => mode(a*)),
    "newtool"     -> (a => newtoolEntry(a)),
    "parsereqt"   -> (a => requirementsMarkdownParser(a*)),
    "prd"         -> (a => prd(a*)),
    "scala"       -> (a => scalaProjectDriver(a*)),
    "serv"        -> (a => serveStaticFiles(a*)),
    "skillcheck"  -> (a => skillcheck(a*)),
    "skillgrants" -> (a => printSkillGrants(a*)),
    "ssg"         -> (a => staticSiteGen(a*)),
    "statusline"  -> (a => statusLine(a*)),
    "svg"         -> (a => renderSvgDiagram(a*)),
    "text"        -> (a => text(a*)),
    "typo"        -> (a => typoClassify(a*)),
    "update"      -> (a => checkGenscalatorUpdate(a*)),
    "verify"      -> (a => verifyCommand(a*)),
    "web"         -> (a => webFetch(a*)),
    "which"       -> (a => which(a*)),
    "wr"          -> (a => workflowResearch(a*)),
  )

  val verbs: Vector[Verb] = entries.map(_._1)

  // NB not `find` — that would shadow the top-level @main def find inside this object's scope.
  def entryFor(verb: Verb): Option[Seq[String] => Unit] =
    entries.collectFirst { case (v, run) if v == verb => run }

  def usage: String =
    s"usage: tt <tool> <args...>   (tools: ${verbs.mkString(" ")})"

  // scaffoldNewTypedTool takes ONE arg, not String* — adapt here so the table shape stays uniform.
  private def newtoolEntry(a: Seq[String]): Unit = a match {
    case Seq(name) => scaffoldNewTypedTool(name)
    case _         => Console.err.println("usage: tt newtool <name>"); sys.exit(2)
  }

  def dispatch(args: Seq[String]): Unit = args match {
    case verb +: rest =>
      entryFor(verb) match {
        case Some(run) => run(rest)
        case None      => Console.err.println(s"tt: no such tool '$verb'"); Console.err.println(usage); sys.exit(2)
      }
    case _ => Console.err.println(usage); sys.exit(2)
  }
}

@main def dispatchTypedTools(args: String*): Unit = Dispatch.dispatch(args)
