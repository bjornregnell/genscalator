//> using file ../project.scala
//> using jvm 21
//> using dep org.scalameta::munit::1.3.4

// Tests for tt git's push paths (SM232), fetch reporting (issue 026) and log path-filtering (issue 038).
// The mirror-set push is the risky one: a dropped or reordered remote means work the caller believes is
// pushed is not, and nothing downstream notices. So the argv parsing is tested purely, and the
// multi-remote push is tested END-TO-END against real bare repos in a temp dir — no network, no shell,
// and nothing outside the temp dir is touched. fetch and log print their findings, so those suites
// capture stdout (println goes through Console.out, which Console.withOut rebinds) and assert on the
// REPORT — for fetch the report's honesty IS the defect under test.
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

// Shared by the fetch/log suites below: capture what a dispatch PRINTS. Predef.println goes through
// Console.out, so Console.withOut sees every report line. Only success paths may be exercised this
// way — fail() calls sys.exit, which would take the test JVM with it.
private object GitTestIo:
  def captureOut(body: => Unit): String =
    val bos = java.io.ByteArrayOutputStream()
    Console.withOut(java.io.PrintStream(bos, true, "UTF-8"))(body)
    bos.toString("UTF-8")

  def initRepo(dir: os.Path): Unit =
    os.makeDir.all(dir)
    os.proc("git", "init", "--initial-branch=main").call(cwd = dir)
    os.proc("git", "config", "user.email", "test@example.com").call(cwd = dir)
    os.proc("git", "config", "user.name", "Test").call(cwd = dir)

  def commitFile(repo: os.Path, name: String, content: String, msg: String): Unit =
    os.write.over(repo / os.RelPath(name), content, createFolders = true)
    os.proc("git", "add", name).call(cwd = repo)
    os.proc("git", "commit", "-m", msg).call(cwd = repo)

// fetch (issue 026): the old implementation printed the constant `fetch: up to date` whenever git's
// output was empty — but empty output means "no refs updated", not "you are current", so the message
// asserted a proposition the command never evaluated, and could contradict `tt gitinfo` about the same
// repo seconds apart. These tests pin the honest report: it names the remote actually contacted, states
// the local branch's MEASURED standing when it can, and lists the remotes it did not touch.
class GitFetchSuite extends munit.FunSuite:
  import GitTestIo.*

  test("parseFetchArgs reads --repo and keeps every --remote in order") {
    val p = Git.parseFetchArgs(List("--repo", "/r", "--remote", "origin", "--remote", "upstream"))
    assertEquals(p.repo, Some("/r"))
    assertEquals(p.remotes, Vector("origin", "upstream"))
  }

  test("parseFetchArgs with no --remote yields an empty set (default remote resolved against the repo)") {
    val p = Git.parseFetchArgs(List("--repo", "/r"))
    assertEquals(p.repo, Some("/r"))
    assertEquals(p.remotes, Vector.empty)
  }

  // The issue-026 specimen, end-to-end: remote-tracking refs current, local branch genuinely behind.
  test("fetch names its remote, measures the local branch's standing, and lists unfetched remotes") {
    val work = os.temp.dir(prefix = "ttgit-")
    try
      val alpha = work / "alpha.git"
      val beta  = work / "beta.git"
      val repo  = work / "repo"
      for bare <- List(alpha, beta) do
        os.makeDir.all(bare)
        os.proc("git", "init", "--bare", "--initial-branch=main", bare.toString).call(cwd = work)
      initRepo(repo)
      os.proc("git", "remote", "add", "alpha", alpha.toString).call(cwd = repo)
      os.proc("git", "remote", "add", "beta", beta.toString).call(cwd = repo)
      commitFile(repo, "a.txt", "hello\n", "seed")
      os.proc("git", "push", "-u", "alpha", "main").call(cwd = repo)

      // a second writer advances alpha, so repo's checkout is genuinely 1 behind
      val other = work / "other"
      os.proc("git", "clone", alpha.toString, other.toString).call(cwd = work)
      os.proc("git", "config", "user.email", "test@example.com").call(cwd = other)
      os.proc("git", "config", "user.name", "Test").call(cwd = other)
      commitFile(other, "b.txt", "second\n", "advance")
      os.proc("git", "push").call(cwd = other)

      val headBefore = os.proc("git", "rev-parse", "HEAD").call(cwd = repo).out.trim()

      // first fetch transfers the new commit: the report must NAME alpha and list beta as untouched
      val out1 = captureOut(Git.dispatch("fetch", "--repo", repo.toString, "--remote", "alpha"))
      assert(clue(out1).contains("fetch alpha"))
      assert(clue(out1).contains("(not fetched: beta)"))

      // second fetch: git prints NOTHING (refs already current) while the local branch is still 1
      // behind — the exact case the old constant message called "fetch: up to date"
      val out2 = captureOut(Git.dispatch("fetch", "--repo", repo.toString, "--remote", "alpha"))
      assert(clue(out2).contains("fetch alpha: no new refs"))
      assert(clue(out2).contains("0 ahead, 1 behind alpha/main"))
      assert(!clue(out2).contains("fetch: up to date"))

      // read-only: two fetches moved neither HEAD nor the working tree
      assertEquals(os.proc("git", "rev-parse", "HEAD").call(cwd = repo).out.trim(), headBefore)
      assert(!os.exists(repo / "b.txt"), "fetch must never touch the working tree")
    finally TestFs.removeAllForce(work)
  }

  test("fetch with no --remote resolves the branch's remote and STILL names it in the report") {
    val work = os.temp.dir(prefix = "ttgit-")
    try
      val alpha = work / "alpha.git"
      val repo  = work / "repo"
      os.makeDir.all(alpha)
      os.proc("git", "init", "--bare", "--initial-branch=main", alpha.toString).call(cwd = work)
      initRepo(repo)
      os.proc("git", "remote", "add", "alpha", alpha.toString).call(cwd = repo)
      commitFile(repo, "a.txt", "hello\n", "seed")
      os.proc("git", "push", "-u", "alpha", "main").call(cwd = repo)

      val out = captureOut(Git.dispatch("fetch", "--repo", repo.toString))
      // "up to date" is allowed HERE because it is measured (rev-list against alpha/main), and scoped
      // to the named remote — never the old unqualified constant.
      assert(clue(out).contains("fetch alpha: no new refs"))
      assert(clue(out).contains("up to date with alpha/main"))
      assert(!clue(out).contains("not fetched"), "the only remote was fetched; nothing to list")
    finally TestFs.removeAllForce(work)
  }

// log --path (issue 038): the acceptance sketch from the issue, end-to-end in a scratch repo, plus the
// `--` separator's reason for existing (a path that names a ref must stay a path).
class GitLogSuite extends munit.FunSuite:
  import GitTestIo.*

  test("log --path filters by touched path, repeats as OR, and intersects with --grep") {
    val work = os.temp.dir(prefix = "ttgit-")
    try
      val repo = work / "repo"
      initRepo(repo)
      commitFile(repo, "A.txt", "1\n", "alpha one")
      commitFile(repo, "A.txt", "2\n", "alpha two")
      commitFile(repo, "B.txt", "b\n", "beta one")
      def logOut(extra: String*): String =
        captureOut(Git.dispatch(("log" :: "--repo" :: repo.toString :: extra.toList)*))

      val a = logOut("--path", "A.txt")
      assert(clue(a).contains("alpha one"))
      assert(clue(a).contains("alpha two"))
      assert(!clue(a).contains("beta one"))
      assert(clue(a).contains("=== 2 commit(s)"))

      val b = logOut("--path", "B.txt")
      assert(clue(b).contains("beta one"))
      assert(clue(b).contains("=== 1 commit(s)"))

      assert(clue(logOut("--path", "A.txt", "--path", "B.txt")).contains("=== 3 commit(s)"))

      val ag = logOut("--path", "A.txt", "--grep", "two")
      assert(clue(ag).contains("alpha two"))
      assert(!clue(ag).contains("alpha one"))
      assert(clue(ag).contains("=== 1 commit(s)"))

      // --path composes with the cap contract: the count line still flags a hit --limit
      assert(clue(logOut("--path", "A.txt", "--limit", "1")).contains("hit --limit 1"))
    finally TestFs.removeAllForce(work)
  }

  test("log --path matching nothing yields (no matching commits), not an error") {
    val work = os.temp.dir(prefix = "ttgit-")
    try
      val repo = work / "repo"
      initRepo(repo)
      commitFile(repo, "A.txt", "1\n", "seed")
      val out = captureOut(Git.dispatch("log", "--repo", repo.toString, "--path", "never-existed.txt"))
      assert(clue(out).contains("(no matching commits)"))
    finally TestFs.removeAllForce(work)
  }

  // The separator's whole point: a FILE named like a REF must be read as a path. Without the `--`,
  // git would either error on the ambiguity or read `main` as the branch and return every commit;
  // with it, exactly the one commit that touched the file comes back.
  test("log --path treats a path that names a ref as a PATH (the -- separator at work)") {
    val work = os.temp.dir(prefix = "ttgit-")
    try
      val repo = work / "repo"
      initRepo(repo)                                  // branch is named main
      commitFile(repo, "A.txt", "1\n", "unrelated")
      commitFile(repo, "main", "ref-lookalike\n", "add file named main")
      val out = captureOut(Git.dispatch("log", "--repo", repo.toString, "--path", "main"))
      assert(clue(out).contains("add file named main"))
      assert(!clue(out).contains("unrelated"))
      assert(clue(out).contains("=== 1 commit(s)"))
    finally TestFs.removeAllForce(work)
  }
