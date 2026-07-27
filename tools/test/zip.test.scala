//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Unit tests for zip.scala's compute layer. Real archives are BUILT here rather than committed as
// binary fixtures: a fixture would be opaque to every other tool in this repo (the SM246 finding —
// nothing in the toolbox can see inside a binary), whereas a zip written by the test is fully
// described by the code that writes it.
//
// The corruption test is the one that earns its keep. `check` exists to catch an archive that is
// intact-by-checksum but broken inside, so a suite that only ever feeds it GOOD archives would pass
// while the verb's entire reason for existing was broken.

import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipOutputStream}

class ZipSuite extends munit.FunSuite:

  private def withTmp[A](body: os.Path => A): A =
    val dir = os.temp.dir(prefix = "ttzip-")
    try body(dir) finally TestFs.removeAllForce(dir)

  /** Write a zip with the given entries; a name ending in `/` becomes a directory entry. */
  private def makeZip(at: os.Path, entries: (String, String)*): Path =
    val p   = at.toNIO
    val out = ZipOutputStream(Files.newOutputStream(p))
    try
      entries.foreach { (name, content) =>
        out.putNextEntry(ZipEntry(name))
        if !name.endsWith("/") then out.write(content.getBytes("UTF-8"))
        out.closeEntry()
      }
    finally out.close()
    p

  test("entriesOf reports every entry, and flags directories as such") {
    withTmp { d =>
      val z  = makeZip(d / "a.zip", "bin/" -> "", "bin/tt" -> "hello", "VERSION.txt" -> "v1")
      val es = Zip.entriesOf(z)
      assertEquals(es.map(_.name), Vector("bin/", "bin/tt", "VERSION.txt"))
      assertEquals(es.map(_.isDir), Vector(true, false, false))
    }
  }

  test("sizes are the UNCOMPRESSED lengths, so a caller can total them") {
    withTmp { d =>
      val z  = makeZip(d / "a.zip", "one.txt" -> "12345", "two.txt" -> "1234567890")
      val es = Zip.entriesOf(z).filterNot(_.isDir)
      assertEquals(es.map(_.size).sum, 15L)
    }
  }

  test("a directory entry contributes no bytes to the file total") {
    withTmp { d =>
      val z  = makeZip(d / "a.zip", "docs/" -> "", "docs/x.md" -> "abc")
      val es = Zip.entriesOf(z)
      assertEquals(es.filterNot(_.isDir).map(_.size).sum, 3L)
      assertEquals(es.count(_.isDir), 1)
    }
  }

  test("a sound archive has NO failures") {
    withTmp { d =>
      val z = makeZip(d / "a.zip", "a.txt" -> "alpha", "b.txt" -> "beta")
      assertEquals(Zip.failures(z), Vector.empty)
    }
  }

  test("a CORRUPTED entry is reported by name — the verb's whole purpose") {
    withTmp { d =>
      // Corrupt the DEFLATE STREAM, deliberately not the archive structure. The two are different
      // failures and only one is the subject here: damage the central directory and `ZipFile` cannot
      // open the file at all, which is `entriesOf`'s error path, not `check`'s.
      //
      // ⚠ Getting this wrong is how this test first failed. `bytes.length / 2` looks like "the middle
      // of the payload" and is not: the content was highly repetitive, deflate crushed it to a few
      // dozen bytes, and the midpoint of the FILE landed in the central directory at the end. So the
      // offset is computed from the layout instead of guessed — a local file header is 30 fixed bytes
      // plus the filename, so the entry's data provably begins after that, and the content is long and
      // varied so the compressed stream is comfortably bigger than the offset we poke.
      val name  = "payload.txt"
      val body  = (1 to 2000).map(i => s"line $i of payload\n").mkString
      val z     = makeZip(d / "a.zip", name -> body)
      val bytes = Files.readAllBytes(z)
      val dataStart = 30 + name.length
      assert(bytes.length > dataStart + 64, s"fixture too small to corrupt safely: ${bytes.length}")
      (dataStart + 8 until dataStart + 16).foreach(i => bytes(i) = (bytes(i) ^ 0xff).toByte)
      Files.write(z, bytes)
      // The archive must still OPEN — otherwise this test would be exercising the wrong failure.
      assertEquals(Zip.entriesOf(z).map(_.name), Vector(name))
      val bad = Zip.failures(z)
      assert(bad.nonEmpty, "a corrupted payload must be reported, not silently accepted")
      assertEquals(bad.map(_._1), Vector("payload.txt"))
    }
  }

  test("a missing file and a non-zip file fail DIFFERENTLY, and neither exits the JVM") {
    withTmp { d =>
      val missing = intercept[IllegalArgumentException](Zip.entriesOf((d / "nope.zip").toNIO))
      assert(missing.getMessage.contains("not a readable file"), missing.getMessage)
      val notZip = d / "plain.txt"
      os.write(notZip, "I am not a zip archive")
      val bad = intercept[IllegalArgumentException](Zip.entriesOf(notZip.toNIO))
      assert(bad.getMessage.contains("not a readable zip archive"), bad.getMessage)
    }
  }

  // ---- the containment guard ----------------------------------------------------------------
  //
  // These are the tests that matter. `resolveEntry` is pure on purpose, so hostile names can be fired
  // at it directly with no filesystem and no archive, and the verdict is identical on every host —
  // which is the only way a guard's tests are worth anything. A guard whose tests only ever feed it
  // WELL-BEHAVED names proves nothing at all.

  private val root = Path.of("/tmp/dest").toAbsolutePath.normalize

  test("an ordinary nested entry is allowed, and lands under the destination") {
    val got = Zip.resolveEntry(root, "bin/tt")
    assertEquals(got.map(_.toString), Right(root.resolve("bin/tt").toString))
  }

  test("a name containing a space is ALLOWED — the control-char check must not eat ordinary names") {
    assert(Zip.resolveEntry(root, "docs/My File.md").isRight)
  }

  test("dot-dot escape is rejected, however deeply buried") {
    List(
      "../evil",
      "../../../../etc/passwd",
      "bin/../../evil",
      "a/b/c/../../../../../evil",
      "./../evil",
    ).foreach: name =>
      val got = Zip.resolveEntry(root, name)
      assert(got.isLeft, s"MUST reject '$name' but got $got")
      assert(got.left.exists(_.contains("escapes")), s"wrong reason for '$name': $got")
  }

  test("BACKSLASH separators are folded first, so a Windows-shaped escape cannot slip past normalize") {
    // On POSIX, Path.of would treat this as ONE innocent filename that normalize cannot collapse, while
    // Windows would read it as an escape. Folding makes the verdict identical on both.
    val got = Zip.resolveEntry(root, """..\..\evil""")
    assert(got.isLeft, s"backslash escape must be rejected, got $got")
  }

  test("absolute names are rejected on every host: POSIX, drive letter and UNC alike") {
    List("/etc/passwd", """C:\Windows\evil""", "C:/Windows/evil", """\\server\share\evil""").foreach: name =>
      val got = Zip.resolveEntry(root, name)
      assert(got.isLeft, s"MUST reject absolute '$name' but got $got")
      assert(got.left.exists(_.contains("absolute")), s"wrong reason for '$name': $got")
  }

  test("an empty name, a control character, and the destination itself are all rejected") {
    assert(Zip.resolveEntry(root, "").isLeft)
    assert(Zip.resolveEntry(root, "a\u0000b").left.exists(_.contains("control character")))
    assert(Zip.resolveEntry(root, ".").left.exists(_.contains("destination itself")))
  }

  test("a name that merely LOOKS like an escape but stays inside is allowed") {
    // Guarding must not be so blunt that it rejects legitimate archives: `..` as a substring of a
    // filename, and a traversal that resolves back inside, are both fine.
    assert(Zip.resolveEntry(root, "a..b/c").isRight)
    assert(Zip.resolveEntry(root, "a/../b").isRight)
  }

  test("a sibling directory sharing a prefix is NOT treated as inside the destination") {
    // The classic startsWith bug: /tmp/destevil is a string-prefix match on /tmp/dest but is a
    // different directory. Path.startsWith compares name elements, and this pins that it stays that way.
    val got = Zip.resolveEntry(root, "../destevil/x")
    assert(got.isLeft, s"prefix-sibling escape must be rejected, got $got")
  }

  test("methodName names the two real methods and does not lie about an unknown one") {
    assertEquals(Zip.methodName(ZipEntry.STORED), "stored")
    assertEquals(Zip.methodName(ZipEntry.DEFLATED), "deflated")
    assertEquals(Zip.methodName(99), "method99")
  }
