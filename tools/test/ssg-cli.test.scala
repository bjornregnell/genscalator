//> using file ../project.scala
//> using jvm 21
//> using dep org.scalameta::munit::1.3.4

// CLI-level tests for ssg's SOURCE SELECTION — the three-armed match that resolves (sources, outDir, srcDir)
// from the arguments. SsgSuite covers the pure renderer only, and CliSuite has no ssg case, so before this
// file that match had NO test at all: the Scala 3.9 tuple-ascription fix (RC4 bump) was compiler-checked but
// not behaviour-checked. Each test drives one arm end-to-end in a temp dir.
class SsgSelectionSuite extends munit.FunSuite:

  private def post(status: String): String =
    s"# Title\n\n> **Status: $status 2026-07-01.** intro\n\nbody text\n"

  test("legacy arm: <srcdir> <outdir> renders every .md in the dir") {
    val work = os.temp.dir(prefix = "ssgsel-")
    try
      val src = work / "src"; val out = work / "out"
      os.makeDir.all(src)
      os.write(src / "a.md", "# A\n\nalpha\n")
      os.write(src / "b.md", "# B\n\nbeta\n")
      staticSiteGen(src.toString, out.toString)
      assert(os.exists(out / "a.html"), "a.html missing")
      assert(os.exists(out / "b.html"), "b.html missing")
      assert(os.read(out / "a.html").contains("alpha"))
    finally TestFs.removeAllForce(work)
  }

  test("legacy arm: a single .md file source renders just that page") {
    val work = os.temp.dir(prefix = "ssgsel-")
    try
      val src = work / "only.md"; val out = work / "out"
      os.write(src, "# Only\n\nsolo\n")
      staticSiteGen(src.toString, out.toString)
      assert(os.exists(out / "only.html"), "only.html missing")
    finally TestFs.removeAllForce(work)
  }

  test("list arm: --out <dir> renders exactly the listed files") {
    val work = os.temp.dir(prefix = "ssgsel-")
    try
      val src = work / "src"; val out = work / "out"
      os.makeDir.all(src)
      os.write(src / "a.md", "# A\n\nalpha\n")
      os.write(src / "b.md", "# B\n\nbeta\n")
      staticSiteGen("--out", out.toString, (src / "a.md").toString)
      assert(os.exists(out / "a.html"), "a.html missing")
      assert(!os.exists(out / "b.html"), "b.html should not be rendered when not listed")
    finally TestFs.removeAllForce(work)
  }

  test("status arm: --status selects by current status and always keeps index") {
    val work = os.temp.dir(prefix = "ssgsel-")
    try
      val src = work / "blog"; val out = work / "out"
      os.makeDir.all(src)
      os.write(src / "index.md", "# Index\n\nlanding\n")
      os.write(src / "live.md", post("published"))
      os.write(src / "wip.md", post("drafted"))
      staticSiteGen("--status", "published", "--out", out.toString, src.toString)
      assert(os.exists(out / "live.html"), "published post missing")
      assert(os.exists(out / "index.html"), "index is always rendered")
      assert(!os.exists(out / "wip.html"), "drafted post must not be rendered")
    finally TestFs.removeAllForce(work)
  }

  test("set render prunes a stale page the current set no longer contains") {
    val work = os.temp.dir(prefix = "ssgsel-")
    try
      val src = work / "src"; val out = work / "out"
      os.makeDir.all(src); os.makeDir.all(out)
      os.write(src / "a.md", "# A\n\nalpha\n")
      os.write(out / "gone.html", "<html>stale</html>")
      staticSiteGen("--out", out.toString, (src / "a.md").toString)
      assert(os.exists(out / "a.html"), "a.html missing")
      assert(!os.exists(out / "gone.html"), "stale page should be pruned from a set render")
    finally TestFs.removeAllForce(work)
  }
