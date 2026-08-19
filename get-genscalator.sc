// get-genscalator.sc — the bootstrap installer. Downloads the release binary for THIS platform,
// verifies its published sha256, unpacks it into ~/.genscalator, and puts bin/ on your PATH.
//
//   scala-cli run get-genscalator.sc                 # install (preview of the PATH edit is printed)
//   scala-cli run get-genscalator.sc -- --dry-run    # show exactly what WOULD happen; write nothing
//   scala-cli run get-genscalator.sc -- --no-path    # install, but do not touch any shell config
//   scala-cli run get-genscalator.sc -- --tag v0.10.0 --home /opt/gs
//   scala-cli run get-genscalator.sc -- --uninstall           # PREVIEW what would be removed
//   scala-cli run get-genscalator.sc -- --uninstall --force   # actually remove it
//
// install -> test -> uninstall -> reinstall is the supported version-testing loop, and the reason the
// uninstaller lives HERE: this file is fetched fresh, so it can reverse an install whose own binary is
// gone or broken. See the manifest section below for the full argument (issue 039).
//
// ⚠ READ THIS SCRIPT BEFORE RUNNING IT. That is not a formality: genscalator argues against
// curl-into-shell precisely because it hides what it does, so this file has to be worth the read. It is
// deliberately ONE self-contained file with NO dependencies, and both properties are on purpose:
//
//   - SELF-CONTAINED because it is a BOOTSTRAP. It runs before genscalator exists, so it cannot include
//     `tools/ziplib.scala` — the repo is not there yet. Fetching a second file to run would double the
//     trust surface of the one artifact whose entire job is to be trustworthy.
//   - NO `//> using dep` and no `//> using scala`, so `scala-cli run` resolves NOTHING beyond a compiler
//     you may already have. Everything below is JDK-only: java.net.http, java.util.zip,
//     java.security.MessageDigest, java.nio.file. os-lib would be pleasanter to write and would cost the
//     reader a download and the auditor a dependency; the JDK is enough.
//
// ⚠⚠ VENDORED CODE, DECLARED RATHER THAN HIDDEN. `platformFor` and `resolveEntry` below are COPIES of
// `Lib.releasePlatform` and `ZipLib.resolveEntry` in the genscalator toolbox, which is the source of
// truth. A duplicated predicate that drifts is a real hazard (a zip-slip fix applied to one copy looks
// complete from inside that file), so the copies are marked, minimal, and — the part that actually
// prevents drift — checked against the SAME hostile-entry table as the originals in the toolbox suite.
// Structure rather than vigilance. If you change the guard here, change it there, and the shared test
// table will tell you if you did not.

import java.nio.file.{Files, Path, StandardCopyOption}
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}

val Repo = "bjornregnell/genscalator"

def die(msg: String): Nothing =
  System.err.println(s"get-genscalator: $msg")
  sys.exit(2)

// ---- platform -------------------------------------------------------------------------------------
// VENDORED from Lib.releasePlatform. None is a REAL answer and must never become a guess: Intel macOS
// and Windows-on-ARM have no published binary, and installing a nearby platform's build would leave you
// with something that cannot run, which is worse than being told to build from source.
def platformFor(osName: String, osArch: String): Option[String] =
  val os = osName.toLowerCase
  val arch = osArch.toLowerCase match
    case "amd64" | "x86_64" | "x64" => Some("x86_64")
    case "aarch64" | "arm64"        => Some("aarch64")
    case _                          => None
  val family =
    if os.contains("linux") then Some("linux")
    else if os.contains("mac") || os.contains("darwin") then Some("macos")
    else if os.contains("windows") then Some("windows")
    else None
  (family, arch) match
    case (Some("linux"), Some(a))          => Some(s"linux-$a")
    case (Some("macos"), Some("aarch64"))  => Some("macos-aarch64")
    case (Some("windows"), Some("x86_64")) => Some("windows-x86_64")
    case _                                 => None

// ---- download -------------------------------------------------------------------------------------
// Deliberately uses the release REDIRECT urls rather than the GitHub API, which is what keeps this file
// free of a JSON parser:
//     .../releases/latest/download/<asset>      -> newest PUBLISHED release
//     .../releases/download/<tag>/<asset>       -> a named tag
// ⚠ Consequence worth knowing: `latest` sees neither DRAFTS nor PRERELEASES. If the current alpha is
// published as a prerelease, pass --tag explicitly.
def assetUrl(tag: Option[String], name: String): String = tag match
  case Some(t) => s"https://github.com/$Repo/releases/download/$t/$name"
  case None    => s"https://github.com/$Repo/releases/latest/download/$name"

val http = HttpClient.newBuilder.followRedirects(HttpClient.Redirect.NORMAL).build

def fetch(url: String): Array[Byte] =
  val req = HttpRequest.newBuilder(URI.create(url)).GET.build
  val res = Try(http.send(req, HttpResponse.BodyHandlers.ofByteArray))
    .getOrElse(die(s"could not reach $url (offline? proxy?)"))
  if res.statusCode != 200 then
    die(s"GET $url -> ${res.statusCode}. If this is a 404 the platform asset may not be published for " +
      "this release; build from source instead (see the repo README).")
  res.body

def sha256Hex(bytes: Array[Byte]): String =
  java.security.MessageDigest.getInstance("SHA-256")
    .digest(bytes).map(b => String.format("%02x", Byte.box(b))).mkString

// ---- the containment guard ------------------------------------------------------------------------
// VENDORED from ZipLib.resolveEntry. PURE: string and Path arithmetic only, no filesystem access, so it
// gives the same answer on every host.
//
// THE THREAT: a zip entry name is attacker-controlled text. A naive extractor resolves it against the
// destination and writes, so a name full of parent-directory hops lands nowhere near it. That is
// zip-slip. Here the archive is our own release asset, fetched over TLS and sha256-checked BEFORE this
// runs, so the guard is defence in depth rather than the primary control — but a guard you only need
// when something else has already failed is exactly the one worth keeping.
def resolveEntry(destRoot: Path, entryName: String): Either[String, Path] =
  if entryName.isEmpty then Left("empty entry name")
  else if entryName.exists(_.isControl) then Left(s"control character in entry name")
  else if entryName.startsWith("/") || entryName.startsWith("""\\""")
    || entryName.matches("""^[A-Za-z]:[/\\].*""") then Left(s"absolute path: $entryName")
  else
    // Normalise the entry's OWN separators first: the zip spec says forward slash, so a backslash is
    // already non-conformant, and on POSIX `Path.of` would treat a backslash-laden name as one innocent
    // filename that `normalize` cannot collapse while Windows would read it as an escape.
    val root    = destRoot.toAbsolutePath.normalize
    val cleaned = entryName.replace('\\', '/')
    Try(root.resolve(cleaned).normalize).toOption match
      case None                           => Left(s"unusable entry name: $entryName")
      case Some(t) if t == root           => Left(s"entry resolves to the destination itself")
      case Some(t) if !t.startsWith(root) => Left(s"escapes destination: $entryName")
      case Some(t)                        => Right(t)

/** Adjudicate EVERY entry before writing ANY of them, so a hostile archive cannot leave a half-extracted
  * tree behind: files on disk AND an error is the worst state to hand a user.
  *
  * Returns the entry names actually planned (relative, forward-slash), because those names ARE the
  * install manifest: the set of paths this script created is exactly what `--uninstall` must remove. */
def unzip(archive: Path, into: Path, dryRun: Boolean): Vector[String] =
  Using.resource(ZipFile(archive.toFile)) { zf =>
    val entries  = zf.entries.asScala.toVector.filterNot(_.isDirectory)
    val verdicts = entries.map(e => (e, resolveEntry(into, e.getName)))
    val rejected = verdicts.collect { case (e, Left(why)) => s"  REJECTED ${e.getName}  ($why)" }
    if rejected.nonEmpty then
      die(s"the archive failed the containment guard; NOTHING was written:\n${rejected.mkString("\n")}")
    val planned = verdicts.collect { case (e, Right(t)) => (e, t) }
    if dryRun then println(s"  would unpack ${planned.size} file(s) into $into")
    else
      planned.foreach { (e, target) =>
        Files.createDirectories(target.getParent)
        Using.resource(zf.getInputStream(e))(
          in => Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING))
        // java.util.zip restores NO permission bits: the zip format carries them but ZipEntry exposes no
        // way to read them. Without this, bin/tt lands non-executable and the very first command a new
        // user runs fails with exit 126. One line, and it is the difference between working and not.
        if e.getName.startsWith("bin/") then target.toFile.setExecutable(true, true)
      }
    planned.map((e, _) => e.getName.replace('\\', '/'))
  }

// ---- PATH -----------------------------------------------------------------------------------------
// A process CANNOT change the PATH of the shell that launched it, so this always ends by telling you to
// open a new terminal. Skipping that line would make the install appear to succeed and the next command
// fail with `tt: command not found`, which is a worse first impression than one extra instruction.
val Marker = ">>> genscalator >>>"

/** The right file per shell — this is where "just works" is won or lost. bash on macOS reads
  * ~/.bash_profile because Terminal runs LOGIN shells, so the Linux answer is silently wrong there. */
def shellConfig(shell: String, home: Path, isMac: Boolean): Option[(Path, String)] =
  val bin = home.resolve("bin")
  if shell.contains("zsh") then Some((Path.of(sys.props("user.home"), ".zshrc"), s"""export PATH="$bin:$$PATH""""))
  else if shell.contains("fish") then
    Some((Path.of(sys.props("user.home"), ".config", "fish", "config.fish"), s"fish_add_path $bin"))
  else if shell.contains("bash") then
    val rc = if isMac then ".bash_profile" else ".bashrc"
    Some((Path.of(sys.props("user.home"), rc), s"""export PATH="$bin:$$PATH""""))
  else None

/** Append a MARKED block, and only if the marker is absent — so re-running does not stack duplicates and
  * an uninstall has something unambiguous to remove. */
def addToPath(file: Path, line: String, dryRun: Boolean): Unit =
  val existing = if Files.exists(file) then Files.readString(file) else ""
  if existing.contains(Marker) then println(s"  PATH: already configured in $file (marker present)")
  else
    val block = s"\n# $Marker\n$line\n# <<< genscalator <<<\n"
    println(s"  PATH: ${if dryRun then "would append" else "appending"} to $file:")
    println(s"        $line")
    if !dryRun then Files.writeString(file, existing + block)

// ---- the install manifest, and uninstall ------------------------------------------------------------
// Issue 039: nothing removed what the installer put on a box, so "test from a clean box" was not reachable
// TWICE. Every alpha tester who compares versions runs install -> test -> uninstall -> reinstall, and the
// third step did not exist. Hand-reversing an install from memory leaves dirt that is INVISIBLE, and the
// second install then mostly works — which is worse than failing, because it tests a state no real
// newcomer has.
//
// WHY a flag on this script rather than `tt uninstall` or a second script (decided, do not re-litigate):
//   - ONE carrier for the layout knowledge. The artifact that installed the files is the one that knows
//     where they are; a separate uninstaller duplicates that knowledge into a carrier that rots alone.
//   - No added download weight: it travels inside the file every installer-user already fetches.
//   - The bootstrap works in BOTH directions. This script is fetched fresh, so uninstall stays available
//     even after the install dir is gone or broken — which a `tt uninstall` verb could never manage,
//     since it would be the very binary being removed.
//   - `tt` excludes destructive verbs BY DESIGN; that is what makes it allowlistable. The lifecycle
//     script is the right home for the one destructive operation this project needs.
//
// The fetched-fresh property carries an obligation: a NEWER script must be able to uninstall an OLDER
// install. Hence the manifest AND a well-known-paths fallback that says out loud when it is being used.
val ManifestName = "INSTALL-MANIFEST.txt"

/** Plain text, no JSON parser, because this file resolves NO dependencies. `key: value` header lines, then
  * a `files:` line after which EVERY line is one install-root-relative path. */
def writeManifest(home: Path, files: Vector[String], tag: Option[String], pathFile: Option[Path]): Unit =
  val header = Vector(
    s"# genscalator install manifest — written by get-genscalator.sc",
    s"# `scala-cli run get-genscalator.sc -- --uninstall --home $home` removes exactly what is listed here.",
    s"# Safe to read. Deleting this file only costs the uninstaller its precision (it falls back to",
    s"# well-known paths and says so).",
    s"manifest-version: 1",
    s"installed-tag: ${tag.getOrElse("latest")}",
    s"install-root: $home",
    s"path-file: ${pathFile.map(_.toString).getOrElse("none")}",
    s"path-marker: $Marker",
    s"files:",
  )
  Files.writeString(home.resolve(ManifestName), (header ++ files.sorted).mkString("", "\n", "\n"))

final case class Manifest(files: Vector[String], pathFile: Option[Path], tag: String, fallback: Boolean)

/** Read the manifest, or fall back to well-known paths for a PRE-MANIFEST install. The fallback is
  * deliberately narrow: it only claims the directories this installer has ever created, never the whole
  * install root, so a user who put something of their own in there does not lose it. */
def readManifest(home: Path): Manifest =
  val f = home.resolve(ManifestName)
  if !Files.exists(f) then
    val wellKnown = Vector("bin", "docs", "skills", "tools", "plugins", "VERSION.txt")
      .map(home.resolve)
      .filter(Files.exists(_))
      .flatMap: p =>
        if Files.isDirectory(p) then
          Using.resource(Files.walk(p))(_.iterator.asScala.toVector)
            .filter(Files.isRegularFile(_))
            .map(q => home.relativize(q).toString.replace('\\', '/'))
        else Vector(home.relativize(p).toString.replace('\\', '/'))
    Manifest(wellKnown, None, "unknown", fallback = true)
  else
    val lines = Files.readString(f).linesIterator.toVector
    def header(k: String): Option[String] =
      lines.find(_.startsWith(s"$k: ")).map(_.stripPrefix(s"$k: ").trim).filter(_.nonEmpty)
    val idx = lines.indexWhere(_.trim == "files:")
    val files = if idx < 0 then Vector.empty else lines.drop(idx + 1).map(_.trim).filter(_.nonEmpty)
    // the manifest itself is ours too, and listing it keeps the "remove exactly what is listed" rule true
    Manifest(
      (files :+ ManifestName).distinct,
      header("path-file").filterNot(_ == "none").map(Path.of(_)),
      header("installed-tag").getOrElse("unknown"),
      fallback = false,
    )

/** PREVIEW BY DEFAULT, like every destructive shape in this project: printing costs one extra keystroke,
  * and the cheapest way to delete the wrong tree is a mistyped --home.
  *
  * NEVER edits a human-owned file. The PATH block in a shell rc and any hook entry in settings.json are
  * REPORTED with exact instructions instead. The installer may have added the PATH line, but the file
  * belongs to the human and may have been hand-tuned since — and a script that silently rewrites a
  * shell rc is precisely the behaviour this project argues against. */
def uninstall(home: Path, force: Boolean): Unit =
  if !Files.isDirectory(home) then die(s"nothing to uninstall: $home does not exist")
  val m       = readManifest(home)
  val present = m.files.map(home.resolve).filter(Files.exists(_))
  val missing = m.files.size - present.size

  println(s"genscalator uninstall${if force then "" else "  (PREVIEW: nothing will be removed)"}")
  println(s"  install:  $home")
  println(s"  version:  ${m.tag}")
  if m.fallback then
    println(s"  ⚠ NO $ManifestName found — this install predates manifests, so the list below comes from")
    println(s"    WELL-KNOWN PATHS (bin, docs, skills, tools, plugins, VERSION.txt) rather than a record of")
    println(s"    what was actually written. Anything you put in $home yourself is NOT listed and NOT touched.")
  println(s"  ${if force then "removing" else "would remove"}: ${present.size} file(s)" +
    (if missing > 0 then s"  ($missing listed file(s) already gone)" else ""))
  present.take(12).foreach(p => println(s"    ${home.relativize(p)}"))
  if present.size > 12 then println(s"    … and ${present.size - 12} more")

  if force then
    present.foreach(p => Try(Files.deleteIfExists(p)))
    // prune the directories we emptied, deepest first; a non-empty dir simply survives, which is the
    // correct outcome for anything the user put there
    Using.resource(Files.walk(home))(_.iterator.asScala.toVector)
      .filter(Files.isDirectory(_))
      .sortBy(p => -p.toString.length)
      .foreach(d => Try(Files.deleteIfExists(d)))
    if Files.exists(home) then
      println(s"  kept:     $home still exists — it holds files this uninstaller did not put there")
    else println(s"  removed:  $home")

  println()
  println("  These are YOURS to remove, and are printed rather than edited:")
  m.pathFile match
    case Some(rc) =>
      println(s"    1. the PATH block in $rc — delete the lines from")
      println(s"       `# $Marker` through `# <<< genscalator <<<` (inclusive)")
    case None =>
      println(s"    1. a PATH entry, if you added one, pointing at ${home.resolve("bin")}")
      println(s"       (in a shell rc it sits between `# $Marker` and `# <<< genscalator <<<`;")
      println(s"        on Windows it is a User Path entry set from PowerShell)")
  println(s"    2. any genscalator hook or permission entries you added to a Claude settings.json")
  println(s"       (this installer never wrote them, so it will not guess at removing them)")
  println()
  if force then
    println("Uninstalled. OPEN A NEW TERMINAL so the old PATH entry stops being inherited,")
    println("then verify the box is naked:  command -v tt   (should print nothing)")
  else
    println(s"PREVIEW complete — nothing was removed. Re-run with --force to apply:")
    println(s"  scala-cli run get-genscalator.sc -- --uninstall --force --home $home")

// ---- main -----------------------------------------------------------------------------------------
val argv      = args.toList
val dryRun    = argv.contains("--dry-run")
val noPath    = argv.contains("--no-path")
val uninstall_ = argv.contains("--uninstall")
// --dry-run wins over --force if both are given: between two readings of an ambiguous command line, the
// one that writes nothing is the right guess.
val force     = argv.contains("--force") && !dryRun
def flag(n: String): Option[String] =
  val i = argv.indexOf(n); if i >= 0 && i + 1 < argv.size then Some(argv(i + 1)) else None

val osName = sys.props.getOrElse("os.name", "")
val isMac  = osName.toLowerCase.contains("mac")

val home = flag("--home").map(Path.of(_)).getOrElse(Path.of(sys.props("user.home"), ".genscalator"))
if Files.exists(home.resolve(".git")) then die(
  s"refusing: $home is a git checkout, not a binary install. Pass --home <dir> for a real install.")

// Uninstall runs BEFORE the platform check on purpose: a box whose platform has no published binary can
// still be carrying an install (from --home, or from a build), and refusing to clean it would be absurd.
if uninstall_ then
  uninstall(home, force)
  sys.exit(0)

val plat = platformFor(osName, sys.props.getOrElse("os.arch", "")).getOrElse(die(
  s"no published binary for this platform ($osName ${sys.props.getOrElse("os.arch", "?")}).\n" +
    "  Intel macOS and Windows-on-ARM are supported by BUILDING FROM SOURCE, which is documented\n" +
    "  rather than a workaround: see the repo README. Refusing to install a binary that cannot run."))

val tag  = flag("--tag")
val zipN = s"genscalator-$plat.zip"

println(s"genscalator bootstrap${if dryRun then "  (DRY RUN: nothing will be written)" else ""}")
println(s"  platform: $plat")
println(s"  release:  ${tag.getOrElse("latest published")}")
println(s"  install:  $home")

val zipBytes = fetch(assetUrl(tag, zipN))
val shaText  = String(fetch(assetUrl(tag, zipN + ".sha256")), "UTF-8")
val expected = shaText.trim.split("\\s+").headOption.getOrElse("").toLowerCase
val actual   = sha256Hex(zipBytes)
// ABORT on mismatch, and never continue "just this once": the whole point of publishing a checksum is
// that the bytes are refused when it does not match.
if expected.isEmpty then die("the published .sha256 was empty or unreadable; refusing to install unverified bytes")
if expected != actual then die(
  s"CHECKSUM MISMATCH -- refusing to install.\n  expected $expected\n  actual   $actual")
println(s"  verified: sha256 $actual  (${zipBytes.length} B)")

val tmpZip = Files.createTempFile("genscalator-", ".zip")
Files.write(tmpZip, zipBytes)
if !dryRun then Files.createDirectories(home)
val installed = unzip(tmpZip, home, dryRun)
val n = installed.size
Files.deleteIfExists(tmpZip)
if !dryRun then
  // The archive carries the CI-stamped tag in VERSION.txt and unzip REPLACE_EXISTING has already
  // written it; overwriting with the REQUESTED ref would turn "latest" into a stamp that can never
  // equal a real tag, so `tt update --native` would re-install forever instead of saying up to date.
  // Found by the v0.10.0 post-publish smoke, 2026-07-28. Write a fallback only if the zip had none.
  val versionFile = home.resolve("VERSION.txt")
  if !Files.exists(versionFile) then Files.writeString(versionFile, tag.getOrElse("latest") + "\n")
  println(s"  unpacked: $n file(s) into $home")

val pathFileUsed: Option[Path] =
  if noPath then
    println(s"  PATH: skipped (--no-path). Add $home/bin to your PATH yourself.")
    None
  else
    val shell = sys.env.getOrElse("SHELL", "")
    shellConfig(shell, home, isMac) match
      case Some((file, line)) => addToPath(file, line, dryRun); Some(file)
      case None =>
        println(s"  PATH: unrecognised shell '$shell' -- add this directory yourself: ${home.resolve("bin")}")
        if osName.toLowerCase.contains("windows") then
          println("        PowerShell: [Environment]::SetEnvironmentVariable('Path',")
          println(s"          [Environment]::GetEnvironmentVariable('Path','User') + ';' + '${home.resolve("bin")}', 'User')")
        None

// LAST, so the manifest records the PATH file too and only ever describes a completed install. A manifest
// written before the work would describe an install that may not exist (issue 039's carrier-staleness
// argument, the same class as issues 034/036).
if !dryRun then
  writeManifest(home, installed, tag, pathFileUsed)
  println(s"  manifest: ${home.resolve(ManifestName)}  (uninstall reads this)")

println()
if dryRun then println("DRY RUN complete -- nothing was written. Re-run without --dry-run to install.")
else
  println("Done. OPEN A NEW TERMINAL (this script cannot change the PATH of the shell that started it),")
  println("then run:  tt help")
