//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Unit tests for forge.scala's PURE request-building (SM207: the dialect routing, URL encoding and
// payload shape are the safety-critical part, and they are testable without a network). The live
// effectful verbs are exercised by a human-approved run, never from a suite.

class ForgeRequestSuite extends munit.FunSuite:

  // ---- uploadAssetUrl: dialect routing + the FIXED GitHub uploads root ----
  test("uploadAssetUrl --gh targets the fixed uploads.github.com root, never the api root argument") {
    val u = Forge.uploadAssetUrl(true, "https://evil.example/api/v1", "o", "r", 42L, "a.zip")
    assertEquals(u, "https://uploads.github.com/repos/o/r/releases/42/assets?name=a.zip")
  }

  test("uploadAssetUrl Gitea uses the given api root") {
    val u = Forge.uploadAssetUrl(false, "https://codeberg.org/api/v1", "o", "r", 7L, "a.zip")
    assertEquals(u, "https://codeberg.org/api/v1/repos/o/r/releases/7/assets?name=a.zip")
  }

  test("uploadAssetUrl encodes spaces as %20, never the form-encoding +") {
    val u = Forge.uploadAssetUrl(true, "", "o", "r", 1L, "my file.zip")
    assert(u.endsWith("?name=my%20file.zip"), u)
  }

  test("uploadAssetUrl encodes a literal + and unicode") {
    val plus = Forge.uploadAssetUrl(true, "", "o", "r", 1L, "a+b.zip")
    assert(plus.endsWith("?name=a%2Bb.zip"), plus)
    val uni = Forge.uploadAssetUrl(true, "", "o", "r", 1L, "å.zip")
    assert(uni.endsWith("?name=%C3%A5.zip"), uni)
  }

  // ---- editPayload: ONLY the provided fields travel ----
  test("editPayload with nothing set is empty (the caller must refuse it)") {
    assert(Forge.editPayload(None, None, false, false).obj.isEmpty)
  }

  test("editPayload carries only the provided fields") {
    val p = Forge.editPayload(None, Some("new body"), false, false)
    assertEquals(p.obj.keySet, Set("body"))
    assertEquals(p.obj("body").str, "new body")
  }

  test("editPayload with all fields set carries all five") {
    val p = Forge.editPayload(Some("n"), Some("b"), true, true, Some("v1.0.0"))
    assertEquals(p.obj.keySet, Set("name", "body", "prerelease", "draft", "tag_name"))
    assert(p.obj("prerelease").bool && p.obj("draft").bool)
    assertEquals(p.obj("tag_name").str, "v1.0.0")
  }
