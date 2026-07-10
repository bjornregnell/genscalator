//> using scala 3.8.4
//> using jvm 21
//> using dep com.lihaoyi::os-lib:0.11.8

// harden — Layer-1 deterministic secret scanner (SM042). Surfaces CANDIDATES for semantic (Layer-2) triage.
//   tt harden repo   <dir>   scan git-TRACKED text files (respects .gitignore); falls back to a walk if not a repo
//   tt harden egress <dir>   scan ALL files under <dir> (a payload destined to LEAVE — a ZIP-staging dir, a deploy
//                            bundle); the higher-value half, because a secret fine at rest can leak on egress.
// Options: --entropy <bits>  (min Shannon bits/char for entropy-gated hits; default 3.6)
// Detects: (1) sensitive FILENAMES (.netrc, id_rsa/ed25519, *.pem/*.key/*.p12/*.pfx, .npmrc/.pypirc);
//          (2) known-PREFIX signatures (PEM private key, AWS AKIA, GitHub gh*_/github_pat_, Google AIza, Slack xox*);
//          (3) entropy-gated: openai sk-…, and generic `key = value` assignments whose VALUE clears the gate.
// It DELIBERATELY does NOT flag bare high-entropy blobs by default (git hashes / base64 data / UUIDs = too many
// FPs) — that is a later --aggressive mode. Expect false positives (a var name / placeholder / doc mention); that is
// BY DESIGN — Layer 1 surfaces, Layer 2 (agent/human) triages. Output is REDACTED (first 4 chars + length) so the
// report itself never leaks a secret. Exit: 1 if any candidate found, 0 if clean, 2 usage/error.
import scala.util.matching.Regex

object Harden:
  final case class Finding(file: String, line: Int, kind: String, redacted: String, entropy: Double)

  /** Shannon entropy (bits/char) of a string. PURE. */
  def entropy(s: String): Double =
    if s.isEmpty then 0.0
    else
      val n = s.length.toDouble
      s.groupMapReduce(identity)(_ => 1.0)(_ + _).values.map { c =>
        val p = c / n; -p * (math.log(p) / math.log(2))
      }.sum

  /** Redact a candidate value: first 4 chars + length only (4 chars of a real secret is not crackable, but reveals
    * a placeholder like "your…" / a prefix like "AKIA…" for triage). PURE. */
  def redact(v: String): String =
    if v.length <= 4 then s"[redacted len=${v.length}]" else s"${v.take(4)}… [len=${v.length}]"

  // (2) known-prefix signatures — specific enough to flag without an entropy gate.
  private val prefixSigs: Seq[(String, Regex)] = Seq(
    "pem-private-key" -> raw"-----BEGIN (?:[A-Z0-9 ]+ )?PRIVATE KEY-----".r,
    "aws-access-key"  -> raw"\bAKIA[0-9A-Z]{16}\b".r,
    "github-token"    -> raw"\bgh[pousr]_[A-Za-z0-9]{36,}\b".r,
    "github-pat"      -> raw"\bgithub_pat_[A-Za-z0-9_]{22,}\b".r,
    "google-api-key"  -> raw"\bAIza[0-9A-Za-z_\-]{35}\b".r,
    "slack-token"     -> raw"\bxox[baprs]-[A-Za-z0-9-]{10,}\b".r,
  )
  // (3a) entropy-gated prefix: sk- collides with lots of non-secrets, so gate it.
  private val openaiRe: Regex = raw"\bsk-[A-Za-z0-9]{20,}\b".r
  // (3b) generic assignment: capture the VALUE (group 2) and gate on its entropy.
  private val assignRe: Regex =
    raw"""(?i)\b(password|passwd|pwd|secret|api[_-]?key|access[_-]?token|auth[_-]?token|client[_-]?secret|private[_-]?key)\b\s*[:=]\s*["']?([^\s"']{8,})["']?""".r

  // (1) sensitive filenames (flag by BASENAME regardless of content).
  def sensitiveFilename(name: String): Option[String] =
    val lower = name.toLowerCase
    if Set(".netrc", "_netrc", "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519", ".npmrc", ".pypirc").contains(lower) then
      Some("sensitive-filename")
    else if Seq(".pem", ".key", ".p12", ".pfx", ".pkcs12").exists(lower.endsWith) then Some("sensitive-filename")
    else None

  /** Scan one file's CONTENT for candidates (filename signatures are the caller's job). PURE. A NUL byte (code
    * point 0) marks the file as binary and skips the content scan. */
  def scanText(fileName: String, text: String, entThreshold: Double): Seq[Finding] =
    val out = scala.collection.mutable.ArrayBuffer[Finding]()
    val isBinary = text.indexOf(0) >= 0
    if !isBinary then
      for (raw, idx) <- text.linesIterator.zipWithIndex do
        val ln = idx + 1
        for (kind, re) <- prefixSigs; m <- re.findAllMatchIn(raw) do
          out += Finding(fileName, ln, kind, redact(m.matched), entropy(m.matched))
        for m <- openaiRe.findAllMatchIn(raw) do
          val e = entropy(m.matched)
          if e >= entThreshold then out += Finding(fileName, ln, "openai-key", redact(m.matched), e)
        for m <- assignRe.findAllMatchIn(raw) do
          val value = m.group(2)
          val e = entropy(value)
          if e >= entThreshold then
            out += Finding(fileName, ln, s"secret-assignment(${m.group(1).toLowerCase})", redact(value), e)
    out.toSeq

  def fmt(f: Finding): String =
    val loc = if f.line == 0 then f.file else s"${f.file}:${f.line}"
    f"$loc  [${f.kind}]  ${f.redacted}  (ent=${f.entropy}%.2f)"

  private def walkFiles(dir: os.Path): Seq[os.Path] =
    val skip = Set(".git", "tmp", "target", ".scala-build", "node_modules", ".bloop", ".metals")
    os.walk(dir, skip = p => skip.contains(p.last)).filter(os.isFile)

  private def trackedFiles(dir: os.Path): Seq[os.Path] =
    try
      val r = os.proc("git", "-C", dir.toString, "ls-files", "-z").call(check = false, stderr = os.Pipe)
      if r.exitCode == 0 then r.out.text().split(0.toChar).filter(_.nonEmpty).map(f => os.Path(f, dir)).toSeq
      else walkFiles(dir)
    catch case _: Throwable => walkFiles(dir)

  def scanDir(dir: os.Path, tracked: Boolean, entThreshold: Double): Seq[Finding] =
    val files = if tracked then trackedFiles(dir) else walkFiles(dir)
    files.flatMap { f =>
      val rel = f.relativeTo(dir).toString
      val byName = sensitiveFilename(f.last).map(k => Finding(rel, 0, k, "(by filename)", 0.0)).toSeq
      val text = try os.read(f) catch case _: Throwable => "" // unreadable/too-large → skip content
      val byContent = if text.nonEmpty then scanText(rel, text, entThreshold) else Nil
      byName ++ byContent
    }

  def usage(): Unit =
    println("""harden - Layer-1 deterministic secret scanner (candidates for Layer-2 triage)
      |  tt harden repo   <dir>            scan git-tracked files (respects .gitignore; falls back to a walk)
      |  tt harden egress <dir>            scan ALL files under <dir> (a payload destined to leave)
      |  options: --entropy <bits>         min Shannon bits/char for entropy-gated hits (default 3.6)
      |exit: 0 clean, 1 candidate(s) found, 2 usage/error. Output is REDACTED. Expect FPs (Layer 2 triages).""".stripMargin)

  def dispatch(args: List[String]): Int =
    var ent = 3.6
    val pos = scala.collection.mutable.ArrayBuffer[String]()
    val a = args.toVector
    var i = 0
    while i < a.length do
      a(i) match
        case "--entropy" if i + 1 < a.length => ent = a(i + 1).toDoubleOption.getOrElse(3.6); i += 2
        case other                           => pos += other; i += 1
    pos.toList match
      case mode :: dirStr :: Nil if mode == "repo" || mode == "egress" =>
        val dir = os.Path(dirStr, os.pwd)
        if !os.isDir(dir) then { System.err.println(s"harden: not a directory: $dir"); return 2 }
        val findings = scanDir(dir, tracked = mode == "repo", ent).sortBy(f => (f.file, f.line))
        if findings.isEmpty then { println(s"harden $mode: clean (0 candidates, entropy>=$ent)"); 0 }
        else
          findings.foreach(f => println(fmt(f)))
          println(s"harden $mode: ${findings.length} candidate(s) for Layer-2 triage (REDACTED; expect false positives)")
          1
      case _ => usage(); 2

@main def hardenScan(args: String*): Unit = sys.exit(Harden.dispatch(args.toList))
