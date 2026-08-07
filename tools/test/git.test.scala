//> using file ../project.scala
//> using jvm 21
//> using dep org.scalameta::munit::1.3.4

// Tests for tt git's push paths (SM232). The mirror-set push is the risky one: a dropped or reordered
// remote means work the caller believes is pushed is not, and nothing downstream notices. So the argv
// parsing is tested purely, and the multi-remote push is tested END-TO-END against real bare repos in a
// temp dir — no network, no shell, and nothing outside the temp dir is touched.
class GitPushSuite extends munit.FunSuite:

  test("parsePushArgs reads --repo and keeps every --remote in order") {
    val p = Git.parsePushArgs(List("--repo", "/r", "--remote", "origin", "--remote", "gitlab"))
    assertEquals(p.repo, Some("/r"))
    assertEquals(p.remotes, Vector("origin", "gitlab"))
  }

  test("parsePushArgs with no --remote yields an empty set (push to upstream)") {
    val p = Git.parsePushArgs(List("--repo", "/r"))
    assertEquals(p.repo, Some("/r"))
    assertEquals(p.remotes, Vector.empty)
  }

  test("parsePushArgs keeps a repeated remote (a typo must not be silently de-duplicated)") {
    val p = Git.parsePushArgs(List("--repo", "/r", "--remote", "gitlab", "--remote", "gitlab"))
    assertEquals(p.remotes, Vector("gitlab", "gitlab"))
  }

  test("parsePushArgs reports a missing --repo as absent rather than defaulting") {
    val p = Git.parsePushArgs(List("--remote", "origin"))
    assertEquals(p.repo, None)
  }

  // --tags. OFF unless asked: a tag is a near-permanent published ref, so pushing one must be a
  // decision the caller typed, never a side effect of an ordinary push.
  test("parsePushArgs leaves tags OFF by default") {
    assertEquals(Git.parsePushArgs(List("--repo", "/r", "--remote", "origin")).tags, false)
  }

  test("parsePushArgs reads --tags, in any position among the remotes") {
    assertEquals(Git.parsePushArgs(List("--repo", "/r", "--tags")).tags, true)
    val p = Git.parsePushArgs(List("--repo", "/r", "--remote", "origin", "--tags", "--remote", "codeberg"))
    assertEquals(p.tags, true)
    assertEquals(p.remotes, Vector("origin", "codeberg"))  // --tags must not swallow a following remote
  }

  // End-to-end: one work repo, two bare remotes, one commit pushed to BOTH by a single call.
  test("commit --push --remote x --remote y lands the commit in both remotes") {
    val work = os.temp.dir(prefix = "ttgit-")
    try
      val alpha = work / "alpha.git"
      val beta  = work / "beta.git"
      val repo  = work / "repo"
      for bare <- Seq(alpha, beta) do
        os.makeDir.all(bare)
        os.proc("git", "init", "--bare", "--initial-branch=main", bare.toString).call(cwd = work)
      os.makeDir.all(repo)
      os.proc("git", "init", "--initial-branch=main").call(cwd = repo)
      os.proc("git", "config", "user.email", "test@example.com").call(cwd = repo)
      os.proc("git", "config", "user.name", "Test").call(cwd = repo)
      os.proc("git", "remote", "add", "alpha", alpha.toString).call(cwd = repo)
      os.proc("git", "remote", "add", "beta", beta.toString).call(cwd = repo)

      os.write(repo / "a.txt", "hello\n")
      val msg = work / "msg.txt"
      os.write(msg, "add a.txt\n")
      Git.dispatch("commit", "--repo", repo.toString, "--message-file", msg.toString,
                   "--add", "a.txt", "--push", "--remote", "alpha", "--remote", "beta")

      val local = os.proc("git", "rev-parse", "HEAD").call(cwd = repo).out.trim()
      for bare <- Seq(alpha, beta) do
        val remoteHead = os.proc("git", "rev-parse", "main").call(cwd = bare).out.trim()
        assertEquals(remoteHead, local, s"$bare did not receive the commit")
    finally TestFs.removeAllForce(work)
  }

  // The standalone verb exists so syncing a remote needs no new commit — the real shape it serves is an
  // established repo (upstream set) whose extra mirrors need catching up. NB a branch with NO upstream and
  // exactly ONE remote is refused by git itself: that remote becomes the default push remote, and push.default
  // simple then requires an upstream. The tool surfaces that error rather than quietly setting one, so the
  // seed push below sets the upstream the way a human would.
  test("push --remote sends already-committed work without making a commit") {
    val work = os.temp.dir(prefix = "ttgit-")
    try
      val alpha = work / "alpha.git"
      val repo  = work / "repo"
      os.makeDir.all(alpha)
      os.proc("git", "init", "--bare", "--initial-branch=main", alpha.toString).call(cwd = work)
      os.makeDir.all(repo)
      os.proc("git", "init", "--initial-branch=main").call(cwd = repo)
      os.proc("git", "config", "user.email", "test@example.com").call(cwd = repo)
      os.proc("git", "config", "user.name", "Test").call(cwd = repo)
      os.proc("git", "remote", "add", "alpha", alpha.toString).call(cwd = repo)
      os.write(repo / "a.txt", "hello\n")
      os.proc("git", "add", "a.txt").call(cwd = repo)
      os.proc("git", "commit", "-m", "seed").call(cwd = repo)
      os.proc("git", "push", "-u", "alpha", "main").call(cwd = repo)

      // a second commit the remote has NOT seen, then sync it with the tool alone
      os.write(repo / "b.txt", "second\n")
      os.proc("git", "add", "b.txt").call(cwd = repo)
      os.proc("git", "commit", "-m", "second").call(cwd = repo)
      val before = os.proc("git", "rev-list", "--count", "HEAD").call(cwd = repo).out.trim()

      Git.dispatch("push", "--repo", repo.toString, "--remote", "alpha")

      val after = os.proc("git", "rev-list", "--count", "HEAD").call(cwd = repo).out.trim()
      assertEquals(after, before, "push must not create a commit")
      assertEquals(os.proc("git", "rev-parse", "main").call(cwd = alpha).out.trim(),
                   os.proc("git", "rev-parse", "HEAD").call(cwd = repo).out.trim())
    finally TestFs.removeAllForce(work)
  }

  // END-TO-END tag push against real bare remotes. The parse tests above prove a flag is READ; only
  // this proves a tag ARRIVES. Both tag kinds are exercised on purpose: --follow-tags (the tempting
  // choice) would push the annotated one and silently skip the lightweight one, and a test that used
  // only annotated tags would have called that a pass.
  test("push --tags lands BOTH lightweight and annotated tags, on every remote") {
    val work = os.temp.dir(prefix = "ttgit-")
    try
      val alpha = work / "alpha.git"
      val beta  = work / "beta.git"
      val repo  = work / "repo"
      for bare <- List(alpha, beta) do
        os.makeDir.all(bare)
        os.proc("git", "init", "--bare", "--initial-branch=main", bare.toString).call(cwd = work)
      os.makeDir.all(repo)
      os.proc("git", "init", "--initial-branch=main").call(cwd = repo)
      os.proc("git", "config", "user.email", "test@example.com").call(cwd = repo)
      os.proc("git", "config", "user.name", "Test").call(cwd = repo)
      os.proc("git", "remote", "add", "alpha", alpha.toString).call(cwd = repo)
      os.proc("git", "remote", "add", "beta", beta.toString).call(cwd = repo)
      os.write(repo / "a.txt", "hello\n")
      os.proc("git", "add", "a.txt").call(cwd = repo)
      os.proc("git", "commit", "-m", "seed").call(cwd = repo)
      os.proc("git", "tag", "v0.1.0").call(cwd = repo)                            // LIGHTWEIGHT
      os.proc("git", "tag", "-a", "v0.2.0", "-m", "annotated").call(cwd = repo)   // ANNOTATED

      Git.dispatch("push", "--repo", repo.toString, "--remote", "alpha", "--remote", "beta", "--tags")

      for bare <- List(alpha, beta) do
        val tags = os.proc("git", "tag", "-l").call(cwd = bare).out.trim().linesIterator.toSet
        assert(tags.contains("v0.1.0"), s"lightweight tag missing from $bare: $tags")
        assert(tags.contains("v0.2.0"), s"annotated tag missing from $bare: $tags")
    finally TestFs.removeAllForce(work)
  }

  test("push without --tags leaves tags at home") {
    val work = os.temp.dir(prefix = "ttgit-")
    try
      val alpha = work / "alpha.git"
      val repo  = work / "repo"
      os.makeDir.all(alpha)
      os.proc("git", "init", "--bare", "--initial-branch=main", alpha.toString).call(cwd = work)
      os.makeDir.all(repo)
      os.proc("git", "init", "--initial-branch=main").call(cwd = repo)
      os.proc("git", "config", "user.email", "test@example.com").call(cwd = repo)
      os.proc("git", "config", "user.name", "Test").call(cwd = repo)
      os.proc("git", "remote", "add", "alpha", alpha.toString).call(cwd = repo)
      os.write(repo / "a.txt", "hello\n")
      os.proc("git", "add", "a.txt").call(cwd = repo)
      os.proc("git", "commit", "-m", "seed").call(cwd = repo)
      // Set the upstream first, as the standalone-push test above does: with a single remote and no
      // upstream git refuses the push outright, which would fail this test BEFORE it ever reached the
      // tag question it exists to ask.
      os.proc("git", "push", "-u", "alpha", "main").call(cwd = repo)
      os.proc("git", "tag", "v9.9.9").call(cwd = repo)

      Git.dispatch("push", "--repo", repo.toString, "--remote", "alpha")

      assertEquals(os.proc("git", "tag", "-l").call(cwd = alpha).out.trim(), "",
        "an ordinary push must not publish tags as a side effect")
    finally TestFs.removeAllForce(work)
  }
