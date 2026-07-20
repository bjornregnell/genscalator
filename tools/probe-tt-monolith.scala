// DISPATCHER SEED (SM178 tt-graalify; born as the SM146/T4 monolith probe, 2026-07-20, kept on
// BR's call): ONE @main that reaches every tool's entry point, so a single native-image build
// makes the WHOLE toolbox reachable. Probe results: the full toolbox builds as one 42MB
// --no-fallback image on graalvm-community:25.0.2 with zero reflection config; https needs
// --enable-url-protocols=https at build time (see PB SM146/SM178). This file seeds the real
// single dispatcher (the parked one-@main-pure-fns design) — supersede it there, don't fork it.
@main def ttAllToolsProbe(args: String*): Unit =
  val rest = args.drop(1)
  args.headOption match
    case Some("text")       => text(rest*)
    case Some("files")      => files(rest*)
    case Some("chrono")     => chronoStopwatch(rest*)
    case Some("mode")       => mode(rest*)
    case Some("git")        => gitCommitPush(rest*)
    case Some("gitinfo")    => gitInfoOverview(rest*)
    case Some("web")        => webFetch(rest*)
    case Some("bloop")      => bloopServerCtl(rest*)
    case Some("statusline") => statusLine(rest*)
    case Some("forge")      => forgeClient(rest*)
    case Some("md-fmt")     => formatMarkdown(rest*)
    case Some("log")        => logAnalyze(rest*)
    case Some("htmltext")   => htmltext(rest*)
    case Some("ssg")        => staticSiteGen(rest*)
    case Some("box")        => boxRemoteOps(rest*)
    case Some("hangover")   => hangoverDetect(rest*)
    case Some("ascii")      => renderAsciiDiagram(rest*)
    case Some("verify")     => verifyCommand(rest*)
    case Some("typo")       => typoClassify(rest*)
    case Some("skillcheck") => skillcheck(rest*)
    case Some("gvdot")      => renderGraphvizDiagram(rest*)
    case Some("skillgrants") => printSkillGrants(rest*)
    case Some("svg")        => renderSvgDiagram(rest*)
    case Some("update")     => checkGenscalatorUpdate(rest*)
    case Some("harden")     => hardenScan(rest*)
    case Some("wr")         => workflowResearch(rest*)
    case Some("parsereqt")  => requirementsMarkdownParser(rest*)
    case Some("guardcheck") => checkGuardPatterns(rest*)
    case Some("serv")       => serveStaticFiles(rest*)
    case Some("find")       => find(rest*)
    case Some("prd")        => prd(rest*)
    case Some("doc")        => doc(rest*)
    case Some("newtool")    =>
      rest.headOption match
        case Some(n) => scaffoldNewTypedTool(n)
        case None    => println("usage: newtool <name>"); sys.exit(2)
    case other =>
      println(s"ttAllToolsProbe: unknown tool $other")
      sys.exit(2)
