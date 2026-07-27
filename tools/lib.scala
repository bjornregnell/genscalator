//> using jvm 21
// (no version include: mainless helper — inherits it from its includer; see project.scala)

// Shared helpers for the Scala agent toolbox. PURE (no I/O effects except the explicitly-named file
// readers). Project-agnostic — destined for an open Codeberg repo. Uses only the JDK (no deps) so pure
// text tools compile fast. Effectful drivers (running sbt/pdflatex) live in separate files that add os-lib.
package agenttools

object Lib:
  // --- file readers (the only I/O here; named so impurity is explicit) ---
  /** Read a file lenient as Latin-1: every byte maps to a char, so it never throws on non-UTF-8 logs
    * (LaTeX logs aren't valid UTF-8). ASCII pattern matching ("! ", etc.) is unaffected. */
  def readLatin1(path: String): String =
    String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)), "ISO-8859-1")

  /** Read a file as UTF-8 (for prose/source where åäö etc. matter). */
  def readUtf8(path: String): String =
    String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)), "UTF-8")

  // --- paths ---
  /** Absolute in the POSIX sense ("/..."), the Windows drive sense ("C:\..." or "C:/"), or UNC
    * ("\\server\share"). PURE: it inspects the STRING, touching no filesystem.
    *
    * Deliberately NOT `java.nio.Paths.get(_).isAbsolute`, whose answer is host-dependent in BOTH
    * directions — on Windows "/x" is not absolute, on Linux "C:\x" is not — while the callers are pure
    * functions whose tests must give the same answer on every host.
    *
    * ⚠ It lives HERE, in one place, because it did not. `tt sbt` and `tt bloop` each had their own
    * `startsWith("/")`, so both refused every path a Windows user could type. `tt sbt` was fixed first
    * and `tt bloop` kept failing, because a fix applied to one instance of a duplicated predicate looks
    * complete from inside that file. Same reason `toolsDir` below is shared.
    */
  def isAbsolutePath(p: String): Boolean =
    p.startsWith("/") || p.startsWith("""\\""") || p.matches("""^[A-Za-z]:[/\\].*""")

  /** Map an os/arch pair to the platform token in a release asset name (`genscalator-<token>.zip`), or
    * None when this project publishes no binary for that combination.
    *
    * Takes its inputs as PARAMETERS rather than reading `os.name`/`os.arch`, so the whole distribution
    * matrix is unit-testable from any host — the same reason `isAbsolutePath` is a string predicate.
    *
    * ⚠ None is a real answer and must not be turned into a guess. It encodes BR's distribution decision
    * of 2026-07-27: assets for the four PROVEN platforms, source build documented for the rest. Intel
    * macOS and Windows-on-ARM deliberately return None — the first has never produced an artifact, and
    * the second is published EXPERIMENTAL and currently fails because VirtusLab ships no
    * `aarch64-pc-win32` scala-cli build. Guessing a nearby token there would download a binary that
    * cannot run, which is a worse outcome than being told to build from source.
    */
  def releasePlatform(osName: String, osArch: String): Option[String] =
    val os = osName.toLowerCase
    val arch = osArch.toLowerCase match
      case "amd64" | "x86_64" | "x64"      => Some("x86_64")
      case "aarch64" | "arm64"             => Some("aarch64")
      case _                               => None
    val family =
      if os.contains("linux") then Some("linux")
      else if os.contains("mac") || os.contains("darwin") then Some("macos")
      else if os.contains("windows") then Some("windows")
      else None
    (family, arch) match
      case (Some("linux"), Some(a))           => Some(s"linux-$a")       // both linux arches are published
      case (Some("macos"), Some("aarch64"))   => Some("macos-aarch64")   // Apple Silicon
      case (Some("windows"), Some("x86_64"))  => Some("windows-x86_64")
      case _                                  => None                    // incl. Intel mac + win-aarch64

  /** Match a name against a `*`-only glob. `*` matches any run of characters; every other character is
    * literal, so a name containing regex metacharacters cannot smuggle a pattern in.
    *
    * ⚠ It lives HERE for the same reason `isAbsolutePath` does. It was written private inside `tt forge`
    * for `release-download --pattern`, and `tt zip extract --exec` needed exactly the same predicate
    * hours later — the moment at which a second copy usually appears and then drifts. Deliberately
    * `*`-only and documented as such: anything richer invites a caller to assume full shell globbing and
    * get a silently-empty result instead of an error.
    */
  def globMatches(glob: String, name: String): Boolean =
    name.matches(glob.split("\\*", -1).map(java.util.regex.Pattern.quote).mkString(".*"))

  // --- toolbox location ---
  /** Locate the tools dir (cwd-independent): the -Dtt.tools property the `tt` launcher passes, else walk up
    * from the cwd for a `tools/tt`. The ONE shared definition — doc / prd / skillcheck / skillgrants all use
    * this (previously each had an identical top-level copy, which collided at a whole-`tools` compile). */
  def toolsDir(): Option[java.nio.file.Path] =
    import java.nio.file.{Files, Path}
    sys.props.get("tt.tools").map(Path.of(_)).filter(d => Files.exists(d.resolve("tt")))
      .orElse:
        Iterator.iterate(Path.of("").toAbsolutePath)(p => p.getParent).takeWhile(_ != null).take(8)
          .find(d => Files.exists(d.resolve("tools").resolve("tt"))).map(_.resolve("tools"))

  /** Locate the genscalator ROOT — the directory holding `docs/`, `skills/`, `reqts/`. This, not
    * `toolsDir`, is what the runtime verbs actually want: `doc`, `prd`, `skillcheck`, `skillgrants`
    * and `update` each read a SIBLING of `tools/`, never `tools/` itself.
    *
    * Two installations must both resolve:
    *   - a CONTRIBUTOR's git clone, found via `-Dtt.tools` or the cwd walk-up (both land on `tools/`,
    *     whose parent is the root);
    *   - a USER's binary install under `GENSCALATOR_HOME` (default `~/.genscalator`), which has NO
    *     `tools/` at all — it ships the data those verbs read, not the sources.
    *
    * Order matters: an explicit `GENSCALATOR_HOME` wins, then the clone (so a contributor working in
    * a checkout gets THAT checkout, not a stale installed copy), then the default install location.
    * Ordering the default install ahead of the walk-up would silently shadow the repo you are editing. */
  def rootDir(): Option[java.nio.file.Path] =
    import java.nio.file.{Files, Path}
    def dir(p: Path): Option[Path] = Option.when(Files.isDirectory(p))(p)
    sys.env.get("GENSCALATOR_HOME").map(Path.of(_)).flatMap(dir)
      .orElse(toolsDir().map(_.getParent))
      .orElse(dir(Path.of(sys.props.getOrElse("user.home", "."), ".genscalator")))

  // --- JSON ---
  /** Encode a string as a JSON string literal, quotes included, per RFC 8259. Pure, dependency-free.
    * Escapes the mandatory set (" \ and the C0 controls via \b \f \n \r \t or \uXXXX); passes other
    * characters (incl. UTF-8 åäö) through unchanged. Use to emit valid JSON from Scala tools without jq
    * (e.g. hook additionalContext) — in-process, so hot-path callers pay no external-process cost. */
  def jsonStr(s: String): String =
    val sb = StringBuilder("\"")
    s.foreach:
      case '"'  => sb ++= "\\\""
      case '\\' => sb ++= "\\\\"
      case '\b' => sb ++= "\\b"
      case '\f' => sb ++= "\\f"
      case '\n' => sb ++= "\\n"
      case '\r' => sb ++= "\\r"
      case '\t' => sb ++= "\\t"
      case c if c < 0x20 => sb ++= f"\\u${c.toInt}%04x"
      case c    => sb += c
    (sb += '"').toString

  // --- pure formatting/aggregation ---
  /** Frequency map → sorted bar histogram (descending), top N. */
  def histogram(counts: Map[String, Int], top: Int = 40): String =
    val sorted = counts.toVector.sortBy(-_._2).take(top)
    val w = sorted.map(_._1.length).maxOption.getOrElse(0)
    sorted.map((k, c) => s"  ${k.padTo(w, ' ')} ${"%6d".format(c)} ${"#" * math.min(50, c)}").mkString("\n")

  /** Levenshtein-ish: true iff a and b differ by exactly one edit (sub/ins/del). */
  def edit1(a: String, b: String): Boolean =
    if math.abs(a.length - b.length) > 1 then false
    else if a.length == b.length then a.zip(b).count(_ != _) == 1
    else
      val (s, l) = if a.length < b.length then (a, b) else (b, a)
      var i = 0; var j = 0; var diff = 0
      while i < s.length && diff <= 1 do
        if s(i) == l(j) then { i += 1; j += 1 } else { diff += 1; j += 1 }
      diff + (l.length - j) <= 1
