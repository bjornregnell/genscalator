//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4
//> using dep com.lihaoyi::os-lib:0.11.8

// Tests for the single-sourced product version (VERSION at the repo root). Sibling of
// ScalaVersionSuite, and it exists for the same reason: the failure is SILENT. The number lived in
// four hand-maintained places at once — plugin.json, marketplace.json (at a DIFFERENT json path),
// AGENTS.md, and the git tag — so a release that bumped three of them and missed the fourth would
// look entirely healthy, and the mismatch would surface later as a plugin claiming to be something
// the installed toolbox is not. These tests fail instead.
//
// VERSION is the in-repo SOURCE. The git tag stays canonical for a release, per CHANGELOG; the tag
// is checked against this file at release time, so this file is a checkable mirror rather than a
// second authority.
//
// Issue 036 widened the stamped-carrier set: CONTRIBUTING.md and reqts/issues/README.md carry the
// same banner AGENTS.md does, so a stale checkout's policy pages announce their own vintage —
// asserted below by the same mechanism. The suite also covers the pure display core behind
// `tt --version` (tools/versionlib.scala, issue 028).

class VersionSuite extends munit.FunSuite:

  // Deliberately restates the locate logic (test independence over DRY, scala-style §5).
  private lazy val root: os.Path =
    sys.props.get("tt.tools").map(os.Path(_)).filter(d => os.exists(d / "tt")).map(_ / os.up).getOrElse:
      Iterator.iterate(os.pwd)(_ / os.up).take(8)
        .find(d => os.exists(d / "tools" / "tt"))
        .getOrElse(throw IllegalStateException(s"cannot locate the repo root (pass -Dtt.tools=<dir>); cwd=${os.pwd}"))

  private lazy val version: String = os.read(root / "VERSION.txt").trim

  /** Every `"version": "..."` value in a json file, at ANY path. Deliberately not a path lookup:
    * the two plugin files keep the number at different paths (root in plugin.json, nested under
    * plugins[] in marketplace.json), and a path-based check would silently miss a version field
    * added somewhere new later. `"schemaVersion"` and friends do not match: the regex requires the
    * quote immediately before `version`. */
  private def jsonVersions(p: os.Path): Seq[String] =
    """"version"\s*:\s*"([^"]+)"""".r.findAllMatchIn(os.read(p)).map(_.group(1)).toSeq

  test("VERSION holds a bare semver-shaped number, no leading v") {
    assert(clue(version).matches("""\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?"""))
    // Bare, because plugin.json and marketplace.json carry it bare; the `v` belongs to the git tag.
  }

  test("plugin.json agrees with VERSION") {
    val found = jsonVersions(root / ".claude-plugin" / "plugin.json")
    assert(clue(found).nonEmpty)
    assertEquals(found.distinct, Seq(version))
  }

  test("marketplace.json agrees with VERSION") {
    val found = jsonVersions(root / ".claude-plugin" / "marketplace.json")
    assert(clue(found).nonEmpty)
    assertEquals(found.distinct, Seq(version))
  }

  test("AGENTS.md states the same operating-rules version") {
    // AGENTS.md is what a vendored copy carries into another repo, so a stale number there is the
    // one that misleads an agent about which rules it is running under.
    val agents = os.read(root / "AGENTS.md")
    assert(clue(agents).contains(s"genscalator v$version"))
  }

  test("CONTRIBUTING.md states the same contributing-policy version") {
    // Issue 036: stale policy fails silently and OUTWARD — a contributor on an old checkout forks
    // the wrong forge or mails a stale address, having followed their local instructions correctly.
    // The banner lets a reader see the vintage without asking; this assertion is the release gate
    // that keeps the banner honest, exactly as it does for AGENTS.md above.
    val contributing = os.read(root / "CONTRIBUTING.md")
    assert(clue(contributing).contains(s"genscalator v$version"))
  }

  test("reqts/issues/README.md states the same issue-process version") {
    // Issue 036: the issue-filing rules travel with the repo to every mirror, so a stale copy
    // prescribes an outdated process with nothing to say so. Same banner, same gate.
    val issuesReadme = os.read(root / "reqts" / "issues" / "README.md")
    assert(clue(issuesReadme).contains(s"genscalator v$version"))
  }

  test("no OTHER file at the repo root re-declares a product version") {
    // Cheap guard against the number quietly acquiring a fifth home.
    val strays = os.list(root)
      .filter(p => os.isFile(p) && p.last.endsWith(".json"))
      .filter(p => jsonVersions(p).exists(_ != version))
    assertEquals(strays.map(_.last).toList, Nil)
  }

  // --- the `tt --version` display logic (issue 028; pure core in tools/versionlib.scala) ---

  test("Version.display normalises every carrier shape to the tag spelling") {
    // Display NORMALISATION, not carrier settlement (issue-028 triage): the carriers keep their
    // decided shapes — bare in-repo (pinned above), v-prefixed in an install — and only the
    // PRINTED form is unified. Four shapes ship: bare semver, vX.Y.Z, `latest` (the installer
    // fallback) and `dev` (native-release.yml).
    assertEquals(agenttools.Version.display("0.10.2"), "v0.10.2")
    assertEquals(agenttools.Version.display("v0.10.1"), "v0.10.1")
    assertEquals(agenttools.Version.display("latest"), "latest")
    assertEquals(agenttools.Version.display("dev"), "dev")
    assertEquals(agenttools.Version.display(" 0.10.2\n"), "v0.10.2")
    assertEquals(agenttools.Version.display(""), "unknown")
  }

  test("Version.render is one identifying line: version, kind, root, engine, platform") {
    // Enough to name the artifact in a bug report (the issue-028 acceptance), and a missing
    // VERSION.txt reads as `unknown` rather than crashing or guessing.
    assertEquals(
      agenttools.Version.render(Some("v0.10.1"), "native install", "/home/x/.genscalator",
        "native dispatcher", "Linux", "amd64"),
      "genscalator v0.10.1 (native install at /home/x/.genscalator; native dispatcher; Linux amd64)")
    assertEquals(
      agenttools.Version.render(None, "git checkout", "/repo", "jvm dispatcher", "Linux", "amd64"),
      "genscalator unknown (git checkout at /repo; jvm dispatcher; Linux amd64)")
  }
