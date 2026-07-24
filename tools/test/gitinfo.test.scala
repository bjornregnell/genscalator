//> using scala 3.8.4
//> using dep org.scalameta::munit::1.3.3

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

  test("verdictLine renders the mirror-lag verdict with short hashes") {
    val line = GitInfo.verdictLine("codeberg", "0123456789abcdef", "fedcba9876543210", RemoteSync.RemoteBehind)
    assert(line.contains("remote BEHIND"), line)
    assert(line.contains("0123456789ab"), line) // local head truncated to 12
    assert(line.contains("fedcba987654"), line) // remote head truncated to 12
    assert(!line.contains("DIVERGED"), line)
  }
