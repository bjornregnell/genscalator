//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.4

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

  // ---- assetDeleteUrl (issue 006): the two dialects address an asset DIFFERENTLY ----
  // Getting this wrong is a 404 at best and someone else's asset at worst, so both shapes are pinned.

  test("assetDeleteUrl --gh addresses the asset by its own id, under the FIXED api root") {
    val u = Forge.assetDeleteUrl(true, "https://evil.example/api/v1", "o", "r", 42L, 7L)
    assertEquals(u, "https://api.github.com/repos/o/r/releases/assets/7")
    assert(!u.contains("evil.example"), u) // the api-root argument must never steer a GitHub call
  }

  test("assetDeleteUrl Gitea scopes the attachment under its release, on the given api root") {
    val u = Forge.assetDeleteUrl(false, "https://codeberg.org/api/v1", "o", "r", 42L, 7L)
    assertEquals(u, "https://codeberg.org/api/v1/repos/o/r/releases/42/assets/7")
  }

  test("assetDeleteUrl: the release id appears only in the Gitea shape") {
    assert(!Forge.assetDeleteUrl(true, "", "o", "r", 42L, 7L).contains("42"))
    assert(Forge.assetDeleteUrl(false, "https://x/api/v1", "o", "r", 42L, 7L).contains("/releases/42/"))
  }

  // ---- repoFileUrl (issue 007): dialect routing, ref handling, path encoding ----

  test("repoFileUrl --gh uses the contents endpoint on the FIXED api root") {
    val u = Forge.repoFileUrl(true, false, "https://evil.example", "o", "r", "docs/native.md", None)
    assertEquals(u, "https://api.github.com/repos/o/r/contents/docs/native.md")
  }

  test("repoFileUrl Gitea uses the raw endpoint on the given api root") {
    val u = Forge.repoFileUrl(false, false, "https://codeberg.org/api/v1", "o", "r", "docs/native.md", None)
    assertEquals(u, "https://codeberg.org/api/v1/repos/o/r/raw/docs/native.md")
  }

  test("repoFileUrl GitLab encodes the project AND the whole path as single components") {
    val u = Forge.repoFileUrl(false, true, "https://git.cs.lth.se/", "o", "r", "docs/native.md", None)
    assertEquals(u, "https://git.cs.lth.se/api/v4/projects/o%2Fr/repository/files/docs%2Fnative.md/raw")
  }

  test("repoFileUrl appends --ref as a query, and encodes it") {
    val u = Forge.repoFileUrl(true, false, "", "o", "r", "a.md", Some("v0.10.3"))
    assert(u.endsWith("/contents/a.md?ref=v0.10.3"), u)
    val slashy = Forge.repoFileUrl(true, false, "", "o", "r", "a.md", Some("feature/x"))
    assert(slashy.endsWith("?ref=feature%2Fx"), slashy) // a branch name with a slash must not split the URL
  }

  test("repoFileUrl keeps path separators as separators but encodes what is IN a segment") {
    val u = Forge.repoFileUrl(true, false, "", "o", "r", "docs/my file.md", None)
    assert(u.endsWith("/contents/docs/my%20file.md"), u) // %20, never the form-encoding +
    val uni = Forge.repoFileUrl(true, false, "", "o", "r", "docs/å.md", None)
    assert(uni.endsWith("/contents/docs/%C3%A5.md"), uni)
  }

  test("repoFileUrl tolerates a leading slash on the path without doubling it") {
    val u = Forge.repoFileUrl(true, false, "", "o", "r", "/docs/a.md", None)
    assertEquals(u, "https://api.github.com/repos/o/r/contents/docs/a.md")
    val gl = Forge.repoFileUrl(false, true, "https://gitlab.com", "o", "r", "/docs/a.md", None)
    assert(gl.contains("/files/docs%2Fa.md/raw"), gl)
  }

  test("the two path encoders differ exactly in how they treat a slash") {
    assertEquals(Forge.encodePathSegments("a/b c.md"), "a/b%20c.md")
    assertEquals(Forge.encodePathWhole("a/b c.md"), "a%2Fb%20c.md")
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

  // ---- pr-files / pr-diff URL builders: the typed shapes that replace a raw `gh pr diff` ----
  test("prFilesUrl --gh targets the fixed api.github.com root, never the api root argument") {
    val u = Forge.prFilesUrl(true, "https://evil.example/api/v1", "o", "r", 2)
    assertEquals(u, "https://api.github.com/repos/o/r/pulls/2/files?per_page=100")
  }

  test("prFilesUrl Gitea uses the given api root with the limit param") {
    val u = Forge.prFilesUrl(false, "https://codeberg.org/api/v1", "o", "r", 7)
    assertEquals(u, "https://codeberg.org/api/v1/repos/o/r/pulls/7/files?limit=100")
  }

  test("prDiffUrl --gh is the pulls endpoint itself (the diff comes from the Accept header)") {
    assertEquals(Forge.prDiffUrl(true, "ignored", "o", "r", 2),
      "https://api.github.com/repos/o/r/pulls/2")
  }

  test("prDiffUrl Gitea appends the .diff suffix") {
    assertEquals(Forge.prDiffUrl(false, "https://codeberg.org/api/v1", "o", "r", 2),
      "https://codeberg.org/api/v1/repos/o/r/pulls/2.diff")
  }

  // ---- pr-commits (issue 029): URL builder + the credit-trailer scan ----
  test("prCommitsUrl --gh targets the fixed api.github.com root with per_page") {
    val u = Forge.prCommitsUrl(true, "https://evil.example/api/v1", "o", "r", 3, 100)
    assertEquals(u, "https://api.github.com/repos/o/r/pulls/3/commits?per_page=100")
  }

  test("prCommitsUrl Gitea uses the given api root with the limit param") {
    val u = Forge.prCommitsUrl(false, "https://codeberg.org/api/v1", "o", "r", 3, 25)
    assertEquals(u, "https://codeberg.org/api/v1/repos/o/r/pulls/3/commits?limit=25")
  }

  test("creditTrailers finds a Co-Authored-By trailer case-insensitively") {
    val msg = "headline\n\nbody text\n\nco-authored-by: Claude <noreply@anthropic.com>"
    assertEquals(Forge.creditTrailers(msg), List("co-authored-by: Claude <noreply@anthropic.com>"))
  }

  test("creditTrailers finds a Generated-with line anywhere in the message") {
    val msg = "headline\n\nGenerated with Claude Code\n"
    assertEquals(Forge.creditTrailers(msg), List("Generated with Claude Code"))
  }

  test("creditTrailers is empty for a clean message with human trailers") {
    val msg = "fix: handle empty input\n\nSigned-off-by: A Human <a@b.se>\nReviewed-by: B <b@c.se>"
    assert(Forge.creditTrailers(msg).isEmpty)
  }

  test("creditTrailers surfaces an indented trailer too (trim before matching)") {
    assertEquals(Forge.creditTrailers("h\n\n  Co-Authored-By: X <x@y.z>"),
      List("Co-Authored-By: X <x@y.z>"))
  }

  // ---- pr-merge (issue 030): subject composition + URL + payload dialects ----
  test("mergeSubject names the PR: number + title (CONTRIBUTING.md's merge-commit rule)") {
    assertEquals(Forge.mergeSubject(3, "issues 024-028 and report 087"),
      "Merge PR #3: issues 024-028 and report 087")
  }

  test("prMergeUrl --gh targets the fixed api.github.com root, never the api root argument") {
    assertEquals(Forge.prMergeUrl(true, "https://evil.example/api/v1", "o", "r", 3),
      "https://api.github.com/repos/o/r/pulls/3/merge")
  }

  test("prMergeUrl Gitea uses the given api root") {
    assertEquals(Forge.prMergeUrl(false, "https://codeberg.org/api/v1", "o", "r", 3),
      "https://codeberg.org/api/v1/repos/o/r/pulls/3/merge")
  }

  test("mergePayload --gh carries merge_method + commit_title, and NO commit_message when the body is empty") {
    val p = Forge.mergePayload(true, "merge", "Merge PR #3: t", "")
    assertEquals(p.obj.keySet, Set("merge_method", "commit_title"))
    assertEquals(p.obj("commit_title").str, "Merge PR #3: t")
    assertEquals(p.obj("merge_method").str, "merge")
  }

  test("mergePayload --gh includes commit_message only when a body was given") {
    val p = Forge.mergePayload(true, "squash", "s", "the body")
    assertEquals(p.obj.keySet, Set("merge_method", "commit_title", "commit_message"))
    assertEquals(p.obj("commit_message").str, "the body")
  }

  test("mergePayload Gitea speaks the capitalized Do/MergeTitleField dialect") {
    val p = Forge.mergePayload(false, "merge", "s", "")
    assertEquals(p.obj.keySet, Set("Do", "MergeTitleField"))
    assertEquals(p.obj("Do").str, "merge")
    val pb = Forge.mergePayload(false, "merge", "s", "b")
    assertEquals(pb.obj.keySet, Set("Do", "MergeTitleField", "MergeMessageField"))
  }
