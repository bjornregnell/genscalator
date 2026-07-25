//> using scala 3.9.0-RC4
//> using jvm 21

// secrets — the ONE home for "does this look like a credential, and how do I show it without leaking it".
// PURE; no @main, so tools can `//> using file secrets.scala` (the minijson.scala / limitstore.scala
// pattern — a tool file WITH a @main cannot be included, which is why this exists separately).
//
// WHY IT WAS SPLIT OUT (2026-07-25, the hard way): the agent ran a bare `printenv` to find ONE variable
// and put the entire environment — including two live API tokens — into a durable transcript. The fix is
// `tt env`, which must redact; `tt harden` already knew how to redact; a second copy of that knowledge is
// exactly the drift this project keeps finding elsewhere (three Swedish detectors, same week). So the
// knowledge moved here and both tools call it.
//
// THE PRINCIPLE THIS ENCODES, and it generalises past the environment:
//   READ-ONLY IS NOT THE SAME AS SAFE. For an agent, the hazard of a read is set by where the OUTPUT
//   lands, not by whether the command mutates anything. A transcript is durable, copied and quoted, so a
//   bulk read of any credential-bearing surface is a DISCLOSURE operation however read-only it is.

object Secrets:

  /** Shannon entropy (bits/char). PURE. */
  def entropy(s: String): Double =
    if s.isEmpty then 0.0
    else
      val n = s.length.toDouble
      s.groupMapReduce(identity)(_ => 1.0)(_ + _).values.map { c =>
        val p = c / n; -p * (math.log(p) / math.log(2))
      }.sum

  /** Redact a value: first 4 chars + length. Four chars of a real secret is not crackable, but it still
    * distinguishes a placeholder ("your…") from a real prefix ("AKIA…", "gho_") for triage. PURE. */
  def redact(v: String): String =
    if v.length <= 4 then s"[redacted len=${v.length}]" else s"${v.take(4)}… [len=${v.length}]"

  /** Variable NAMES that carry a credential by convention. Matched case-insensitively as a substring,
    * because the real-world names are compounds: GITHUB_TOKEN, GENSCALATOR_CODEBERG_TOKEN, AWS_SECRET_ACCESS_KEY.
    * PURE. */
  private val SecretNameParts: Vector[String] =
    Vector("token", "secret", "password", "passwd", "pwd", "apikey", "api_key", "auth",
           "credential", "private_key", "privatekey", "access_key", "accesskey", "session_key", "cookie")

  /** Value shapes that are a credential regardless of the variable's name. PURE. */
  private val SecretValueSigs: Vector[scala.util.matching.Regex] = Vector(
    raw"\bgh[pousr]_[A-Za-z0-9]{20,}".r,      // GitHub tokens, incl. the gho_ that leaked
    raw"\bgithub_pat_[A-Za-z0-9_]{20,}".r,
    raw"\bAKIA[0-9A-Z]{16}\b".r,              // AWS
    raw"\bxox[abposr]-[A-Za-z0-9-]{10,}".r,   // Slack
    raw"\bAIza[0-9A-Za-z\-_]{35}\b".r,        // Google
    raw"-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY-----".r,
  )

  /** Values that are long and high-entropy but are NOT credentials. `tt harden`'s header records this
    * lesson already ("deliberately does NOT flag bare high-entropy blobs — git hashes / base64 / UUIDs =
    * too many FPs"); the first version of this file repeated the mistake and a unit test caught it
    * redacting a session UUID. Everything here is a shape a secret essentially never has. PURE. */
  private val UuidRx  = raw"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$".r
  private val HexRx   = raw"^[0-9a-fA-F]{7,64}$$".r
  /** A credential is a single opaque word. Anything with a path, a list separator or whitespace is
    * configuration — PATH and LS_COLORS are long and high-entropy and must never be redacted. */
  private val OpaqueWordRx = raw"^[A-Za-z0-9_.\-]{24,}$$".r

  def looksLikeIdentifierNotSecret(value: String): Boolean =
    UuidRx.matches(value) || HexRx.matches(value)

  /** PURE: should this variable's VALUE be withheld?
    *
    * Three independent reasons, ORed, because each catches what the others miss:
    *   1. the NAME says so           — GITHUB_TOKEN, however innocuous its value looks
    *   2. the VALUE matches a known credential shape — even under a boring name
    *   3. the value is an opaque high-entropy WORD — the unknown-format catch-all, fenced by the
    *      shape rules above so it cannot fire on a UUID, a git hash, PATH or LS_COLORS
    *
    * Deliberately biased toward withholding. A wrongly-redacted value costs one explicit
    * `tt env get NAME --reveal`; a wrongly-revealed one costs a rotation, as it did on 2026-07-25. */
  def looksSecret(name: String, value: String, entropyThreshold: Double = 3.6): Boolean =
    val n = name.toLowerCase
    def opaqueHighEntropy =
      OpaqueWordRx.matches(value)
        && !looksLikeIdentifierNotSecret(value)
        && entropy(value) >= entropyThreshold
    SecretNameParts.exists(n.contains)
      || SecretValueSigs.exists(_.findFirstIn(value).isDefined)
      || opaqueHighEntropy

  /** PURE: the value as it is safe to PRINT — the real thing, or a redaction. */
  def show(name: String, value: String, reveal: Boolean = false): String =
    if reveal || !looksSecret(name, value) then value else redact(value)
