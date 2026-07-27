//> using file ../project.scala
//> using jvm 21
//> using dep org.scalameta::munit::1.3.3

// Tests for tt git's push paths (SM232). The mirror-set push is the risky one: a dropped or reordered
// remote means work the caller believes is pushed is not, and nothing downstream notices. So the argv
// parsing is tested purely, and the multi-remote push is tested END-TO-END against real bare repos in a
// temp dir — no network, no shell, and nothing outside the temp dir is touched.
class GitPushSuite extends munit.FunSuite:

  test("parsePushArgs reads --repo and keeps every --remote in order") {
    val (repo, remotes) = Git.parsePushArgs(List("--repo", "/r", "--remote", "origin", "--remote", "gitlab"))
    assertEquals(repo, Some("/r"))
    assertEquals(remotes, Vector("origin", "gitlab"))
  }

  test("parsePushArgs with no --remote yields an empty set (push to upstream)") {
    val (repo, remotes) = Git.parsePushArgs(List("--repo", "/r"))
    assertEquals(repo, Some("/r"))
    assertEquals(remotes, Vector.empty)
  }

  test("parsePushArgs keeps a repeated remote (a typo must not be silently de-duplicated)") {
    val (_, remotes) = Git.parsePushArgs(List("--repo", "/r", "--remote", "gitlab", "--remote", "gitlab"))
    assertEquals(remotes, Vector("gitlab", "gitlab"))
  }

  test("parsePushArgs reports a missing --repo as absent rather than defaulting") {
    val (repo, _) = Git.parsePushArgs(List("--remote", "origin"))
    assertEquals(repo, None)
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
