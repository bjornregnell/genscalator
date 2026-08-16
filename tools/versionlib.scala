// (no version include: mainless helper — inherits it from its includer; see project.scala)

// versionlib — the answer to `tt --version` / `tt version` / `tt -v` (issue 028): ONE identifying
// line with enough to name the artifact in a bug report — version, carrier kind, resolved root,
// engine, platform. Deliberately NOT a `tt` verb file, per the issue-028 triage: no @main here, so
// DispatchSuite's coverage test ignores it and the launcher never advertises a `version` tool. The
// HANDLERS live where issue 020's help handling lives: dispatch.scala calls `Version.line()`, and
// the bash launcher `tools/tt` restates the cheap subset in bash — a native install has no
// `tools/`, so the two code paths cannot read the same file, let alone share code (the same trade
// as VersionSuite restating the locate logic: independence over DRY).
//
// Display NORMALISATION, not carrier settlement (the triage's framing): the carriers keep their
// decided, tested shapes — bare semver in-repo (VersionSuite pins it: the `v` belongs to the git
// tag) and `vX.Y.Z` as CI stamps an install (native-release.yml; update.scala compares it
// tag-to-tag) — only the PRINTED form is unified to the tag spelling. And NO network call here,
// ever (issue 036 declines a per-invocation staleness check): staleness questions stay with
// `tt update`, which owns the fetch, the throttle and the manual-steps advice.
package agenttools

object Version:

  /** Printed form of a raw `VERSION.txt` value. Four shapes ship (issue-028 triage): bare semver
    * (in-repo source), `vX.Y.Z` (CI-stamped install), `latest` (the installer fallback in
    * get-genscalator.sc) and `dev` (native-release.yml). Bare gains the tag spelling's `v`; the
    * rest already self-describe and pass through unchanged. Pure. */
  def display(raw: String): String =
    val t = raw.trim
    if t.isEmpty then "unknown"
    else if t.head.isDigit then s"v$t"
    else t

  /** The one identifying line. Takes its inputs as PARAMETERS rather than reading the filesystem
    * or `os.name`, so the shape is unit-testable from any host — the same reason
    * `Lib.releasePlatform` is a pure function of its arguments. */
  def render(raw: Option[String], kind: String, root: String,
             engine: String, osName: String, osArch: String): String =
    s"genscalator ${display(raw.getOrElse(""))} ($kind at $root; $engine; $osName $osArch)"

  /** What dispatch.scala's `--version` branch prints: stdout and exit 0, the help contract.
    * Effectful reads only (the resolved root's VERSION.txt, platform properties) — never the
    * network. The resolved root already discriminates the carrier kinds: a root carrying
    * `tools/tt` is a source tree (a git checkout when `.git` is present, otherwise likely a
    * plugin-cache copy), anything else is a binary install. */
  def line(): String =
    import java.nio.file.Files
    val engine =
      if sys.props.contains("org.graalvm.nativeimage.imagecode") then "native dispatcher"
      else "jvm dispatcher"
    val osName = sys.props.getOrElse("os.name", "?")
    val osArch = sys.props.getOrElse("os.arch", "?")
    Lib.rootDir() match
      case None =>
        s"genscalator unknown (no install or checkout found — set GENSCALATOR_HOME or run from a clone; $engine; $osName $osArch)"
      case Some(root) =>
        val raw = scala.util.Try(Files.readString(root.resolve("VERSION.txt")).trim)
          .toOption.filter(_.nonEmpty)
        val kind =
          if Files.exists(root.resolve("tools").resolve("tt")) then
            if Files.exists(root.resolve(".git")) then "git checkout"
            else "source copy (plugin cache?)"
          else "native install"
        render(raw, kind, root.toString, engine, osName, osArch)
