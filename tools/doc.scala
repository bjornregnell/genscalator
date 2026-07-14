//> using scala 3.8.4
//> using jvm 21
//> using file lib.scala

// doc — print a genscalator doc verbatim (PURE, read-only). `tt doc <name>` cats docs/<name> to stdout at
// NATIVE speed, so a gs command like `gs help` becomes an instant `tt doc gs-help` instead of the agent
// re-emitting the file token-by-token (LLM latency). Bare `tt doc` lists the available docs. Path-safe:
// <name> is a BARE filename resolved ONLY under the docs dir (no '/', no '..'); tries <name>, then
// <name>.txt, then <name>.md. The docs dir defaults to <tools>/../docs, located via -Dtt.tools (the `tt`
// launcher passes it) or a cwd walk-up; override with --docs <dir> (config-in-ARGS, e.g. for tests).
//   tt doc <name>            print docs/<name>(.txt|.md) verbatim
//   tt doc                   list the available docs
//   tt doc --docs <dir> ...  override the docs dir
import java.nio.file.{Files, Path}
import agenttools.Lib

private val DocHelp: String =
  """tt doc — print a genscalator doc verbatim (pure, read-only)
    |
    |Cats a doc under docs/ to stdout at native speed. Use it so a rendered command (e.g. gs help) is an
    |instant `tt doc gs-help`, not the agent re-typing the file. Path-safe: <name> is a bare filename
    |resolved only under the docs dir (no '/', no '..'); it tries <name>, then <name>.txt, then <name>.md.
    |
    |Usage:
    |  doc <name>            print docs/<name>(.txt|.md) verbatim
    |  doc                   list the available docs
    |  doc --docs <dir> ...  override the docs dir (default: <tools>/../docs, via -Dtt.tools or cwd walk-up)
    |
    |Examples:
    |  tt doc gs-help              # the gs command help
    |  tt doc statusline-manual    # the statusline manual
    |  tt doc                      # list docs
    |
    |Full reference: tools/README.md""".stripMargin

@main def doc(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(DocHelp); sys.exit(0) }
  val a = args.toList
  def optOf(flag: String): Option[String] =
    val i = a.indexOf(flag); if i >= 0 && i + 1 < a.size then Some(a(i + 1)) else None
  val docsDir: Path =
    optOf("--docs").map(Path.of(_))
      .orElse(Lib.toolsDir().map(_.getParent.resolve("docs")))
      .getOrElse:
        Console.err.println("doc: cannot locate docs/ (pass --docs <dir>, or set -Dtt.tools=<dir>)")
        sys.exit(2)
  if !Files.isDirectory(docsDir) then
    Console.err.println(s"doc: not a docs directory: $docsDir")
    sys.exit(2)
  // positional <name> = the first token that is neither a flag nor a flag's value
  val consumed = scala.collection.mutable.Set.empty[Int]
  a.zipWithIndex.foreach { case (t, i) => if t == "--docs" then { consumed += i; consumed += i + 1 } }
  val positionals = a.zipWithIndex.collect { case (t, i) if !consumed(i) && !t.startsWith("--") => t }
  positionals.headOption match
    case None =>
      val docs = Option(docsDir.toFile.listFiles).getOrElse(Array.empty[java.io.File])
        .filter(_.isFile).map(_.getName).filter(n => n.endsWith(".md") || n.endsWith(".txt")).sorted
      println(s"docs in $docsDir:")
      docs.foreach(n => println(s"  $n"))
    case Some(name) =>
      if !name.matches("[A-Za-z0-9._-]+") || name.contains("..") then
        Console.err.println(s"doc: invalid doc name '$name' (a bare filename only: no '/', no '..')")
        sys.exit(2)
      val candidates = List(name, s"$name.txt", s"$name.md")
      candidates.map(docsDir.resolve).find(Files.isRegularFile(_)) match
        case Some(p) => print(String(Files.readAllBytes(p), "UTF-8"))
        case None =>
          Console.err.println(s"doc: no such doc '$name' in $docsDir (tried: ${candidates.mkString(", ")})")
          sys.exit(2)
