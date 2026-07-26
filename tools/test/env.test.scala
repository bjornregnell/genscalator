//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Unit tests for env.scala + secrets.scala. The redaction cases are the point: this tool exists because
// a bare `printenv` put two live tokens into a transcript, so every test that asserts a value is WITHHELD
// is a test against that recurrence. Real token SHAPES appear below; the values are invented.

class EnvSuite extends munit.FunSuite:

  test("a credential-bearing NAME withholds the value however innocuous the value looks") {
    assert(Secrets.looksSecret("GITHUB_TOKEN", "abc"))
    assert(Secrets.looksSecret("GENSCALATOR_CODEBERG_TOKEN", "x"))
    assert(Secrets.looksSecret("AWS_SECRET_ACCESS_KEY", "y"))
    assert(Secrets.looksSecret("db_password", "hunter2"))
  }

  test("a credential-shaped VALUE is withheld even under a boring name") {
    // shapes only; these strings are invented
    assert(Secrets.looksSecret("HARMLESS", "gho_0000000000000000000000000000000000"))
    assert(Secrets.looksSecret("HARMLESS", "AKIAAAAAAAAAAAAAAAAA"))
    assert(Secrets.looksSecret("HARMLESS", "-----BEGIN PRIVATE KEY-----"))
  }

  test("ordinary variables are NOT withheld, or the tool becomes useless") {
    assert(!Secrets.looksSecret("HOME", "/home/someone"))
    assert(!Secrets.looksSecret("LANG", "en_US.UTF-8"))
    assert(!Secrets.looksSecret("TERM", "xterm-256color"))
    assert(!Secrets.looksSecret("CLAUDE_CODE_SESSION_ID", "e25a6e2d-f576-4ecb-8131-6ab7434ba24b"))
  }

  test("a long high-entropy value is withheld even with no name or shape signal") {
    assert(Secrets.looksSecret("X", "aZ9qW3rT7yU1iO5pL2kJ8hG4fD6sA0zX"))
  }

  // REGRESSION PIN. The first version of the catch-all flagged anything long and high-entropy, which
  // redacted a session UUID, PATH and LS_COLORS. `tt harden` had already documented that lesson
  // ("git hashes / base64 / UUIDs = too many FPs") and this repeated it. A secret is an opaque WORD;
  // anything with a path or list separator is configuration.
  test("identifiers and configuration are NOT mistaken for secrets") {
    assert(!Secrets.looksSecret("CLAUDE_CODE_SESSION_ID", "e25a6e2d-f576-4ecb-8131-6ab7434ba24b"))
    assert(!Secrets.looksSecret("GIT_COMMIT", "a19a3a469d6685da9040788cbd7ab4556d9388c7"))
    assert(!Secrets.looksSecret("PATH", "/usr/local/bin:/usr/bin:/bin:/home/x/.local/bin:/snap/bin"))
    assert(!Secrets.looksSecret("LS_COLORS", "rs=0:di=01;34:ln=01;36:mh=00:pi=40;33:so=01;35"))
  }

  test("but a name signal still wins over the identifier shape") {
    // an opaque id under a credential name is still withheld — shape rules only fence the CATCH-ALL
    assert(Secrets.looksSecret("SESSION_TOKEN", "e25a6e2d-f576-4ecb-8131-6ab7434ba24b"))
  }

  test("redact reveals a prefix and a length but nothing usable") {
    val r = Secrets.redact("abcdefghijklmnop")
    assert(r.startsWith("abcd"), r)
    assert(r.contains("len=16"), r)
    assert(!r.contains("efghijklmnop"), r)
    assertEquals(Secrets.redact("ab"), "[redacted len=2]") // too short to show any prefix
  }

  test("show withholds by default and obeys an explicit reveal") {
    assertEquals(Secrets.show("HOME", "/home/x"), "/home/x")
    assert(Secrets.show("GITHUB_TOKEN", "gho_secretvalue").startsWith("gho_"))
    assert(!Secrets.show("GITHUB_TOKEN", "gho_secretvalue").contains("secretvalue"))
    assertEquals(Secrets.show("GITHUB_TOKEN", "gho_secretvalue", reveal = true), "gho_secretvalue")
  }

  test("selectNames returns sorted names and filters case-insensitively") {
    val env = Map("HOME" -> "/h", "CLAUDE_CODE_SESSION_ID" -> "abc", "CLAUDE_PID" -> "1")
    assertEquals(EnvTool.selectNames(env, None), Right(Vector("CLAUDE_CODE_SESSION_ID", "CLAUDE_PID", "HOME")))
    assertEquals(EnvTool.selectNames(env, Some("claude")).map(_.size), Right(2))
    assert(EnvTool.selectNames(env, Some("[")).isLeft) // bad regex reported, not thrown
  }

  test("renderGet redacts a secret and says how to see it") {
    val line = EnvTool.renderGet("GITHUB_TOKEN", "gho_0000000000000000000000000000000000", reveal = false)
    assert(line.startsWith("GITHUB_TOKEN="), line)
    assert(line.contains("redacted"), line)
    assert(!line.contains("0000000000000000000000000000000000"), line)
  }

  test("renderGet prints an ordinary value plainly, with no redaction noise") {
    assertEquals(EnvTool.renderGet("HOME", "/home/x", reveal = false), "HOME=/home/x")
  }

  test("help documents that has treats blank as absent") {
    // the credential-helper case: `export TOK="$(keyring get svc acct)"` yields "" when the keyring is
    // locked, and reporting that as set is false reassurance
    assert(EnvTool.Help.contains("non-blank"), EnvTool.Help)
  }

  test("help states that no whole-environment dump exists, which is the safety property") {
    assert(EnvTool.Help.contains("NO verb that prints the whole environment"), EnvTool.Help)
  }
