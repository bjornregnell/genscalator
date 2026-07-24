//> using scala 3.8.4
//> using jvm 21

// which — typed, read-only "WHAT IS THIS COMMAND?": the guard-clean composite of the whole bash reflex
// family `command -v` / `which` / `which -a` / `type` / `file` / `readlink -f` / `ls -l`, in ONE call.
// Born from a real guard stall (wr-data command-v-tt-raw-shell-slip-15min-after-digest-load-2026-07-24):
// the agent's actual question is never just the path — it is "does X exist, WHICH file actually runs,
// and is it a script or a native binary?" — so answering it raw takes three trippy calls. This answers
// it in one, purely: PATH walk + symlink chain + magic-byte kind + size/mode/mtime. It never EXECUTES
// the target (no `X --version` probing) — that is the line that keeps it allowlistable.
//   scala-cli run tools/which.scala -- <name> [<name> ...]
import java.nio.file.{Files, Path}

private val WhichHelp: String =
  """tt which — typed, read-only "what is this command?" (pure PATH+file inspection)
    |
    |For each <name>: every $PATH hit in order (the FIRST is what a shell runs; later ones are
    |shadowed), the symlink chain hop by hop, the file kind from magic bytes (ELF / script with
    |its shebang line / jar / text), plus size, mode and mtime. Knows the bash BUILTINS so
    |`tt which cd` answers honestly. Reach for it instead of `command -v` / `which` / `type` /
    |`file` / `readlink -f` — and note it never runs the target (no --version probing).
    |
    |Usage:
    |  which <name> [<name> ...]      report each name (a name with '/' is inspected as a path)
    |
    |Notes:
    |  Exit 0 when every name resolved (builtin or file), 2 when any was not found — scriptable.
    |  Builtin knowledge is a static bash list; a subprocess can NEVER see your interactive
    |  shell's aliases or functions, so those are invisible here (that part of `type` cannot
    |  be ported). Empty $PATH entries (= cwd in POSIX) are skipped, deliberately.
    |
    |Examples:
    |  tt which tt                    script or binary? symlinked from where?
    |  tt which cd echo               builtin honesty (echo is BOTH builtin and file)
    |  tt which scala-cli sbt java    batch-check a toolchain
    |
    |Full reference: tools/README.md""".stripMargin

object WhichTool:
  /** bash builtins (static list, bash 5.x `enable -a` + `[`): the honest half of `type`. */
  val bashBuiltins: Set[String] = Set(
    ".", ":", "[", "alias", "bg", "bind", "break", "builtin", "caller", "cd", "command",
    "compgen", "complete", "compopt", "continue", "declare", "dirs", "disown", "echo",
    "enable", "eval", "exec", "exit", "export", "false", "fc", "fg", "getopts", "hash",
    "help", "history", "jobs", "kill", "let", "local", "logout", "mapfile", "popd",
    "printf", "pushd", "pwd", "read", "readarray", "readonly", "return", "set", "shift",
    "shopt", "source", "suspend", "test", "times", "trap", "true", "type", "typeset",
    "ulimit", "umask", "unalias", "unset", "wait")

  /** Symlink chain from p to its final target: p itself excluded, each hop resolved against its
    * parent (relative targets), capped + cycle-guarded. PURE except readSymbolicLink. */
  def chainOf(p: Path): Vector[Path] =
    var cur = p
    val hops = Vector.newBuilder[Path]
    val seen = scala.collection.mutable.Set(p.toAbsolutePath.normalize)
    var n = 0
    while Files.isSymbolicLink(cur) && n < 20 do
      val raw = Files.readSymbolicLink(cur)
      cur = (if raw.isAbsolute then raw else cur.getParent.resolve(raw)).normalize
      if !seen.add(cur) then n = 20 else { hops += cur; n += 1 }
    hops.result()

  /** File kind from leading magic bytes; for a script, includes its shebang line. PURE given the bytes. */
  def kindOf(bytes: Array[Byte]): String =
    def b(i: Int): Int = if i < bytes.length then bytes(i) & 0xff else -1
    if b(0) == 0x7f && b(1) == 'E' && b(2) == 'L' && b(3) == 'F' then
      s"ELF binary (${if b(4) == 2 then "64" else "32"}-bit)"
    else if b(0) == '#' && b(1) == '!' then
      val line = new String(bytes, "ISO-8859-1").takeWhile(c => c != '\n' && c != '\r').trim
      s"script  $line"
    else if b(0) == 'P' && b(1) == 'K' && b(2) == 3 && b(3) == 4 then "zip archive (jar?)"
    else if b(0) == 0xca && b(1) == 0xfe && b(2) == 0xba && b(3) == 0xbe then "java class / universal binary"
    else if bytes.nonEmpty && bytes.forall(x => x >= 0x20 || x == '\n' || x == '\r' || x == '\t') then
      "text (no shebang)"
    else if bytes.isEmpty then "empty file"
    else "data (unknown magic)"

  def human(n: Long): String =
    if n < 1024 then s"${n}B"
    else if n < 1024 * 1024 then f"${n / 1024.0}%.1fK"
    else f"${n / (1024.0 * 1024)}%.1fM"

  /** The one-line fact row for a resolved file: kind, size, mode, mtime (FileTime ISO, minute cut). */
  def factsOf(p: Path): String =
    val kind = kindOf:
      try
        val in = Files.newInputStream(p)
        try in.readNBytes(256) finally in.close()
      catch case _: Throwable => Array.emptyByteArray
    val size = try human(Files.size(p)) catch case _: Throwable => "?"
    val mode =
      try java.nio.file.attribute.PosixFilePermissions.toString(Files.getPosixFilePermissions(p))
      catch case _: Throwable => "?"
    val mtime = try Files.getLastModifiedTime(p).toString.take(16) catch case _: Throwable => "?"
    s"$kind  $size  $mode  $mtime"

  def pathDirs: Vector[Path] =
    Option(System.getenv("PATH")).getOrElse("").split(':').toVector
      .filter(_.nonEmpty).map(Path.of(_)) // empty entry = POSIX cwd; skipped on purpose (see help)

  def hitsFor(name: String, dirs: Vector[Path]): Vector[Path] =
    dirs.map(_.resolve(name)).filter(p => Files.isRegularFile(p) && Files.isExecutable(p)).distinct

  /** Report one file hit: absolute path, then the symlink chain, then the facts of the FINAL target. */
  def reportHit(p: Path, label: String): Unit =
    println(s"  $p$label")
    val chain = chainOf(p)
    chain.foreach(t => println(s"    -> $t"))
    println(s"    ${factsOf(chain.lastOption.getOrElse(p))}")

  /** Report one name; true iff it resolved to something (builtin counts). */
  def report(name: String, dirs: Vector[Path]): Boolean =
    if name.contains('/') then
      val p = Path.of(name).toAbsolutePath.normalize
      if Files.isRegularFile(p) then { println(s"$name:"); reportHit(p, ""); true }
      else { println(s"$name: no such file"); false }
    else
      val builtin = bashBuiltins(name)
      val hits = hitsFor(name, dirs)
      if builtin && hits.isEmpty then println(s"$name: bash builtin (no file; aliases/functions are invisible here)")
      else if builtin then println(s"$name: bash builtin AND ${hits.size} in PATH (interactive bash runs the BUILTIN)")
      else if hits.isEmpty then println(s"$name: not found in PATH (${dirs.size} dirs; not a bash builtin either)")
      else println(s"$name: ${hits.size} in PATH${if hits.size > 1 then " (first wins)" else ""}")
      hits.zipWithIndex.foreach: (p, i) =>
        reportHit(p, if i == 0 && !builtin then "" else " (shadowed)")
      builtin || hits.nonEmpty

@main def which(args: String*): Unit =
  if args.contains("--help") || args.contains("-h") then { println(WhichHelp); sys.exit(0) }
  val names = args.filterNot(_.startsWith("--")).toVector
  if names.isEmpty then
    println("usage: which <name> [<name> ...]   (what is this command: PATH hits, symlink chain, kind)")
    sys.exit(2)
  val dirs = WhichTool.pathDirs
  val allFound = names.map(WhichTool.report(_, dirs)).forall(identity)
  if !allFound then sys.exit(2)
