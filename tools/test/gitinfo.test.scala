//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4

// Unit tests for gitinfo.scala's PURE remote-sync classifier — the ancestry logic that replaced the
// old hash-equality "IN SYNC or DIVERGED" verdict. No git process is touched here: classify takes the
// ancestry facts as already-decided Option[Boolean], exactly as the driver feeds them from
// `merge-base --is-ancestor`. Regression-pins the bug meta-minion push-19 caught: a deliberately-behind
// mirror (codeberg batch) must read BEHIND, never DIVERGED.

class GitInfoSuite extends munit.FunSuite:
  import GitInfo.RemoteSync

  test("equal heads are IN SYNC regardless of ancestry answers") {
    assertEquals(GitInfo.classify("abc", "abc", None, None), RemoteSync.InSync)
    // equality wins even if a stale ancestry fact says otherwise
    assertEquals(GitInfo.classify("abc", "abc", Some(false), Some(false)), RemoteSync.InSync)
  }

  test("remote HEAD an ancestor of local => remote BEHIND (the lagging-mirror case)") {
    assertEquals(
      GitInfo.classify("localnew", "remoteold", remoteAncestorOfLocal = Some(true), localAncestorOfRemote = Some(false)),
      RemoteSync.RemoteBehind,
    )
  }

  test("local HEAD an ancestor of remote => remote AHEAD") {
    assertEquals(
      GitInfo.classify("localold", "remotenew", remoteAncestorOfLocal = Some(false), localAncestorOfRemote = Some(true)),
      RemoteSync.RemoteAhead,
    )
  }

  test("neither an ancestor of the other => DIVERGED") {
    assertEquals(
      GitInfo.classify("l", "r", remoteAncestorOfLocal = Some(false), localAncestorOfRemote = Some(false)),
      RemoteSync.Diverged,
    )
  }

  test("ancestry undecidable (remote object absent locally) => Unresolved, not Diverged") {
    assertEquals(GitInfo.classify("l", "r", None, None), RemoteSync.Unresolved)
    // one side None, the other a definite false, still cannot prove a fork
    assertEquals(GitInfo.classify("l", "r", Some(false), None), RemoteSync.Unresolved)
    assertEquals(GitInfo.classify("l", "r", None, Some(false)), RemoteSync.Unresolved)
  }

  test("a true ancestry beats a None on the other side") {
    assertEquals(GitInfo.classify("l", "r", Some(true), None), RemoteSync.RemoteBehind)
    assertEquals(GitInfo.classify("l", "r", None, Some(true)), RemoteSync.RemoteAhead)
  }

  // --- issue 004: the porcelain entry classifier behind `tt gitinfo --files` -------------------
  // Pure, so the whole matrix runs without a git process. The column semantics are the point: X is
  // the index, Y the worktree, and confusing the two is exactly the mistake that would let an agent
  // stage a human's in-progress file.

  import GitInfo.ChangeKind

  test("the index column and the worktree column are not interchangeable") {
    assertEquals(GitInfo.classifyEntry("M  tools/a.scala").map(_.kind), Some(ChangeKind.Staged))
    assertEquals(GitInfo.classifyEntry(" M tools/a.scala").map(_.kind), Some(ChangeKind.Unstaged))
    assertEquals(GitInfo.classifyEntry("MM tools/a.scala").map(_.kind), Some(ChangeKind.Both))
    // the raw code survives, so the reader can always check the tool's labelling
    assertEquals(GitInfo.classifyEntry(" M tools/a.scala").map(_.code), Some(" M"))
  }

  test("untracked is its own kind — the case a broad add damages most") {
    val c = GitInfo.classifyEntry("?? tmp/scratch.md")
    assertEquals(c.map(_.kind), Some(ChangeKind.Untracked))
    assertEquals(c.map(_.path), Some("tmp/scratch.md"))
  }

  test("conflicts are called out rather than filed as staged") {
    assertEquals(GitInfo.classifyEntry("UU tools/a.scala").map(_.kind), Some(ChangeKind.Conflicted))
    assertEquals(GitInfo.classifyEntry("AA tools/a.scala").map(_.kind), Some(ChangeKind.Conflicted))
    assertEquals(GitInfo.classifyEntry("DD tools/a.scala").map(_.kind), Some(ChangeKind.Conflicted))
    assertEquals(GitInfo.classifyEntry("AU tools/a.scala").map(_.kind), Some(ChangeKind.Conflicted))
    assertEquals(GitInfo.classifyEntry("UD tools/a.scala").map(_.kind), Some(ChangeKind.Conflicted))
  }

  test("a rename reports the DESTINATION, the path you would --add") {
    val c = GitInfo.classifyEntry("R  tools/old.scala -> tools/new.scala")
    assertEquals(c.map(_.kind), Some(ChangeKind.Staged))
    assertEquals(c.map(_.path), Some("tools/new.scala"))
    assertEquals(c.map(_.code), Some("R "))
  }

  test("non-entries are skipped, never guessed at") {
    assertEquals(GitInfo.classifyEntry(""), None)
    assertEquals(GitInfo.classifyEntry("   "), None)
    assertEquals(GitInfo.classifyEntry("M"), None)
    assertEquals(GitInfo.classifyEntry("warning: something from git"), None) // no space in column 3
    assertEquals(GitInfo.classifyEntry("!! target/out.jar"), None)           // ignored is not a change
    assertEquals(GitInfo.classifyEntry("   path/with/blank/columns"), None)  // both columns blank
  }

  test("formatChanges groups by kind, conflicts first and untracked last") {
    val porcelain =
      """?? tmp/scratch.md
        | M reqts/ROADMAP.md
        |M  tools/gitinfo.scala
        |UU tools/conflicted.scala
        |""".stripMargin
    val rows = GitInfo.formatChanges(porcelain)
    assertEquals(rows.size, 4)
    assert(rows(0).contains("conflict"), rows.mkString("\n"))
    assert(rows(1).contains("staged"), rows.mkString("\n"))
    assert(rows(2).contains("unstaged"), rows.mkString("\n"))
    assert(rows(3).contains("untracked"), rows.mkString("\n"))
    // every row still ends in a bare path, so the output composes with `tt git commit --add`
    assert(rows(3).endsWith("tmp/scratch.md"), rows(3))
  }

  test("the 2026-07-27 near-miss: the human's file is visible as NOT mine to stage") {
    // nine agent files plus reqts/ROADMAP.md, which BR was editing under an edit baton
    val mine = (1 to 9).map(i => s"M  tools/agent$i.scala").mkString("\n")
    val rows = GitInfo.formatChanges(mine + "\n M reqts/ROADMAP.md\n")
    assertEquals(rows.size, 10)
    val roadmap = rows.filter(_.contains("reqts/ROADMAP.md"))
    assertEquals(roadmap.size, 1)
    assert(roadmap.head.contains("unstaged"), roadmap.head) // untouched by the agent's index work
  }

  test("verdictLine renders the mirror-lag verdict with short hashes") {
    val line = GitInfo.verdictLine("codeberg", "0123456789abcdef", "fedcba9876543210", RemoteSync.RemoteBehind)
    assert(line.contains("remote BEHIND"), line)
    assert(line.contains("0123456789ab"), line) // local head truncated to 12
    assert(line.contains("fedcba987654"), line) // remote head truncated to 12
    assert(!line.contains("DIVERGED"), line)
  }
