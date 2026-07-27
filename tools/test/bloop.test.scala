//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Unit tests for bloop.scala's PURE clean guards. Nothing is deleted here and nothing walks a real
// tree — these test the decisions a recursive delete is allowed to make, which is exactly the part
// that must be right before the effectful half ever runs.
//
// The refusal cases are the substance. `bloop clean` removes directory trees, so every test below is
// a case where the answer must be "no" even though the caller asked for it.

class BloopCleanSuite extends munit.FunSuite:

  private val home = System.getProperty("user.home")

  test("a plausible project dir is accepted") {
    assertEquals(BloopTool.unsafeRoot("/home/someone/git/proj"), None)
  }

  test("relative roots are refused — an ambiguous cwd is how the wrong tree gets walked") {
    assert(BloopTool.unsafeRoot("proj").isDefined)
    assert(BloopTool.unsafeRoot("./proj").isDefined)
  }

  test("a root containing .. is refused rather than normalised") {
    assert(BloopTool.unsafeRoot("/home/someone/git/../../etc").isDefined)
  }

  test("the filesystem root is refused") {
    assert(BloopTool.unsafeRoot("/").isDefined)
  }

  test("the home directory itself is refused, with or without a trailing slash") {
    assert(BloopTool.unsafeRoot(home).isDefined)
    assert(BloopTool.unsafeRoot(home + "/").isDefined)
  }

  test("single-segment roots are refused as too shallow") {
    assert(BloopTool.unsafeRoot("/home").isDefined)
    assert(BloopTool.unsafeRoot("/usr").isDefined)
    assert(BloopTool.unsafeRoot("/tmp").isDefined)
  }

  test("somebody ELSE's home directory is refused too, not just the caller's") {
    assert(BloopTool.unsafeRoot("/home/someone").isDefined)
    assert(BloopTool.unsafeRoot("/home/someone/").isDefined)
  }

  // REGRESSION PIN. An earlier guard demanded three or more segments, which refused perfectly ordinary
  // roots like /tmp/build and /opt/proj — and the pure tests all passed, because they only ever tried
  // deep paths. The CLI tests caught it: os.temp.dir() hands back a two-segment /tmp/<name>.
  test("a legitimate two-segment root outside /home is ACCEPTED") {
    assertEquals(BloopTool.unsafeRoot("/tmp/build-xyz"), None)
    assertEquals(BloopTool.unsafeRoot("/opt/proj"), None)
    assertEquals(BloopTool.unsafeRoot("/srv/thing"), None)
  }

  // WINDOWS. `tt bloop clean --dir` used to demand a leading "/", so it refused every Windows path and
  // this whole guard was dead code there. Accepting those paths WIDENS what reaches a destructive verb,
  // so each refusal is pinned in the Windows spelling too — the guards split on "/", and without
  // separator normalisation `C:\Users\bjornr` is ONE segment and slips past both the too-shallow and
  // the home-directory refusal. These cases fail loudly if that normalisation is ever removed.
  test("a Windows project path is ACCEPTED, in either separator spelling") {
    assertEquals(BloopTool.unsafeRoot("""C:\git\proj"""), None)
    assertEquals(BloopTool.unsafeRoot("C:/git/proj"), None)
    assertEquals(BloopTool.unsafeRoot("""\\server\share\proj\sub"""), None)
  }

  test("a Windows drive root is refused, like / is") {
    assert(BloopTool.unsafeRoot("""C:\""").isDefined)
    assert(BloopTool.unsafeRoot("C:/").isDefined)
  }

  test("a Windows user profile directory is refused, like /home/<name> is") {
    assert(BloopTool.unsafeRoot("""C:\Users\bjornr""").isDefined)
    assert(BloopTool.unsafeRoot("C:/Users/bjornr").isDefined)
    assert(BloopTool.unsafeRoot("""C:\Users\bjornr\""").isDefined)
  }

  test("a Windows top-level directory is still too shallow") {
    assert(BloopTool.unsafeRoot("""C:\Users""").isDefined)
    assert(BloopTool.unsafeRoot("""D:\temp""").isDefined)
  }

  test("a Windows path with .. is refused, in either spelling") {
    assert(BloopTool.unsafeRoot("""C:\git\proj\..\..\Windows""").isDefined)
    assert(BloopTool.unsafeRoot("C:/git/proj/../../Windows").isDefined)
  }

  test("planClean defaults to a DRY RUN — deletion is never the default") {
    assertEquals(BloopTool.planClean(List("--dir", "/home/someone/git/proj")),
      Right(BloopTool.CleanPlan("/home/someone/git/proj", apply = false)))
  }

  test("--yes opts in to deletion, in either order") {
    assertEquals(BloopTool.planClean(List("--dir", "/home/someone/git/proj", "--yes")).map(_.apply), Right(true))
    assertEquals(BloopTool.planClean(List("--yes", "--dir", "/home/someone/git/proj")).map(_.apply), Right(true))
  }

  test("clean without --dir is a usage error, never an implicit cwd sweep") {
    assert(BloopTool.planClean(Nil).isLeft)
    assert(BloopTool.planClean(List("--yes")).isLeft)
    assert(BloopTool.planClean(List("--dir")).isLeft)
    assert(BloopTool.planClean(List("/home/someone/git/proj")).isLeft)
  }

  test("an unsafe root fails planning, so --yes on a bad root never reaches the filesystem") {
    assert(BloopTool.planClean(List("--dir", "/", "--yes")).isLeft)
    assert(BloopTool.planClean(List("--dir", home, "--yes")).isLeft)
  }

  test("stray extra arguments are rejected rather than ignored") {
    assert(BloopTool.planClean(List("--dir", "/home/someone/git/proj", "extra")).isLeft)
  }

  test("isRemovable accepts only a directory named exactly .scala-build") {
    val root = "/home/someone/git/proj"
    assert(BloopTool.isRemovable(s"$root/.scala-build", root))
    assert(BloopTool.isRemovable(s"$root/sub/deep/.scala-build", root))
    assert(!BloopTool.isRemovable(s"$root/scala-build", root))
    assert(!BloopTool.isRemovable(s"$root/.scala-build-old", root))
    assert(!BloopTool.isRemovable(s"$root/src", root))
  }

  test("isRemovable refuses the root itself, however it is named") {
    assert(!BloopTool.isRemovable("/home/someone/.scala-build", "/home/someone/.scala-build"))
  }

  test("isRemovable is not fooled by a sibling directory sharing the root's prefix") {
    // /a/b-other is NOT under /a/b, though it starts with the same characters.
    assert(!BloopTool.isRemovable("/a/b-other/.scala-build", "/a/b"))
  }

  test("isRemovable handles a root given with a trailing slash") {
    assert(BloopTool.isRemovable("/a/b/.scala-build", "/a/b/"))
  }
