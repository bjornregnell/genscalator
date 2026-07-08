//> using scala 3.8.4

// =============================================================================
// deployblog.sc  -  deploy the rendered static site to bjornregnell.se (one.com SFTP)
// =============================================================================
//
// SAFE TO COMMIT / PUBLIC: this file contains NO host, username, or password.
// It reads all of them from ~/.netrc, which only you can read (chmod 600). No
// secret is ever placed on a command line, so nothing leaks via the process list.
//
// WHAT IT DOES
//   Mirrors a locally-rendered site directory up to your one.com web space over
//   SFTP, using `lftp`. By default it is ADDITIVE (uploads + overwrites, never
//   deletes remote files). Pass --delete to make it an EXACT mirror (removes
//   remote files that no longer exist locally). Pass --dry-run to preview first.
//
// ONE-TIME SETUP
//   1. Install the tools:   sudo apt install lftp openssh-client
//   2. one.com Control Panel -> SSH & SFTP (SFTP Administration):
//        - make sure "Allow SFTP access" is ON
//        - SET / GENERATE THE SFTP PASSWORD there. This is a SEPARATE password
//          from your one.com account login.
//   3. Create ~/.netrc with your one.com SFTP entry (values from Host Settings):
//        machine ssh.XXXXXXXX.service.one
//        login    your-sftp-username
//        password your-sftp-password
//      then lock it down:   chmod 600 ~/.netrc
//      (the `machine` value is the Host from Host Settings; it ends in .service.one)
//
// USAGE  (run from the genscalator root)
//   STATUS-DRIVEN FLOW (SM032, recommended) -- each post carries a status preamble; `deployed` = what should be live:
//     scala-cli run deployblog.sc -- --serve             # render the published+deployed posts, preview on :8000
//     scala-cli run deployblog.sc -- --release --dry-run # render + show what WOULD upload, change nothing
//     scala-cli run deployblog.sc -- --release           # promote published:deployed + push, one idempotent shot
//     scala-cli run deployblog.sc -- --push              # (re)render + upload just the already-deployed set
//     scala-cli run deployblog.sc -- --status-update published:deployed   # only stamp status (no render/upload)
//   These call `tt ssg` to render (by status) + append status transitions. A spec uses a colon (from:to).
//
//   LOW-LEVEL FLOW (mirror a pre-rendered dir as-is):
//     tt ssg --status published,deployed --out tmp/site blog   # render the deploy set yourself
//     scala-cli run deployblog.sc -- --dry-run                 # ALWAYS dry-run first to confirm the target path
//     scala-cli run deployblog.sc                              # defaults: tmp/site -> webroots/www/blog (additive)
//     scala-cli run deployblog.sc -- tmp/site webroots/www/blog --delete   # exact mirror (removes stale remote files)
//
// REMOTE PATH NOTE
//   On SFTP login you land in your one.com account home. bjornregnell.se's public web
//   root is `webroots/www/` (it holds the live index.html), so the blog goes to
//   `webroots/www/blog` (the default below). The --dry-run output shows exactly where
//   files would go -- check it once before the first real deploy.
//
// SECURITY NOTE
//   Host-key checking uses OpenSSH `StrictHostKeyChecking=accept-new`: trust-on-first-
//   use, pinned in ~/.ssh/known_hosts; a later CHANGED key is rejected (MITM protection
//   after the first connect).
// =============================================================================

import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable

def die(msg: String): Nothing = { System.err.println(s"deployblog: $msg"); sys.exit(2) }

def onPath(exe: String): Boolean =
  Option(System.getenv("PATH")).exists(_.split(java.io.File.pathSeparator)
    .exists(d => Files.isExecutable(Paths.get(d, exe))))

// lftp-quote a token (double quotes, backslash-escaped) so passwords/paths with
// special characters survive; the value travels on lftp's stdin, never on argv.
def q(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// List every regular file under `root` (sorted), so we can synthesize our OWN upload summary from local state.
def listLocalFiles(root: Path): Vector[Path] =
  if !Files.isDirectory(root) then Vector.empty
  else
    val s = Files.walk(root)
    try { import scala.jdk.CollectionConverters.*; s.iterator.asScala.filter(p => Files.isRegularFile(p)).toVector.sorted }
    finally s.close()

// Run lftp with `script` on its stdin and capture its merged stdout+stderr into a LOCAL string; the caller prints
// its OWN synthesized summary rather than lftp's verbose log. With the username-in-URL form (below) lftp reads the
// password from ~/.netrc itself, so the password never appears in this script, the process list, or lftp's output
// -- there is nothing to redact. Returns the exit code and the captured text (surfaced only on --check / failure).
def runContained(script: String): (Int, String) =
  val pb = new ProcessBuilder("lftp")
  pb.redirectErrorStream(true)
  val proc = pb.start()
  proc.getOutputStream.write(script.getBytes("UTF-8"))
  proc.getOutputStream.close()
  val captured = new String(proc.getInputStream.readAllBytes(), "UTF-8")
  (proc.waitFor(), captured)

// ---- args ----
def optVal(name: String): Option[String] =
  val i = args.indexOf(name); if i >= 0 && i + 1 < args.length then Some(args(i + 1)) else None
val valueFlags = Set("--status-update", "--date")           // flags that consume the next arg
val positional =
  val b = scala.collection.mutable.ArrayBuffer[String]()
  var i = 0
  while i < args.length do
    val x = args(i)
    if valueFlags.contains(x) then i += 2
    else if x.startsWith("--") then i += 1
    else { b += x; i += 1 }
  b.toList
val flags      = args.filter(_.startsWith("--")).toSet
val localDir   = positional.lift(0).getOrElse("tmp/site")
val remoteDir  = positional.lift(1).getOrElse("webroots/www/blog")
val blogDir    = "blog"                               // source posts dir (relative to cwd = the genscalator root)
val doDelete   = flags.contains("--delete")
val dryRun     = flags.contains("--dry-run")
val check      = flags.contains("--check")
val serve      = flags.contains("--serve")
val push       = flags.contains("--push")
val release    = flags.contains("--release")
val statusUpdateOpt = optVal("--status-update")
val dateOpt    = optVal("--date")

// ---- SM032 status-driven orchestration: shell out to `tt` (ssg render/promote, serv) ----
// deployblog stays a thin ORCHESTRATOR + transport: rendering lives in `tt ssg` (tested), so the
// render -> preview -> deploy gate survives. A status spec uses a colon (from:to; the arrow form also works).
def tt(ttArgs: String*): Int =
  if !onPath("tt") then die("`tt` not found on PATH (needed to render/serve/promote for --serve/--push/--release).")
  val pb = new ProcessBuilder(("tt" +: ttArgs.toVector)*); pb.inheritIO(); pb.start().waitFor()
def render(statusSel: String): Unit =
  println(s"deployblog: render (status in {$statusSel})  ->  $localDir")
  val rc = tt("ssg", "--status", statusSel, "--out", localDir, blogDir)
  if rc != 0 then die(s"render failed (tt ssg --status exited $rc)")
def statusUpdate(spec: String): Unit =
  val rc = tt(("ssg" +: "--status-update" +: spec +: (dateOpt.map(d => Seq("--date", d)).getOrElse(Nil) :+ blogDir))*)
  if rc != 0 then die(s"promote failed (tt ssg --status-update exited $rc)")

// standalone --status-update: mutate status only (no render, no upload)
statusUpdateOpt match
  case Some(spec) if !push && !release && !serve => statusUpdate(spec); sys.exit(0)
  case _ => ()

// --serve: render the deploy candidates (published + deployed) and preview locally (loopback only, no network)
if serve then
  render("published,deployed")
  println(s"deployblog: previewing $localDir on http://127.0.0.1:8000  (Ctrl-C to stop)")
  sys.exit(tt("serv", localDir))

// --push re-renders the already-deployed set (idempotent); --release also renders the published posts it is
// about to promote, and stamps them deployed AFTER a successful upload (push-then-stamp keeps the source honest).
if push then render("deployed")
if release then render("published,deployed")

// ---- checks ----
if !onPath("lftp") then die("`lftp` not found. Install it:  sudo apt install lftp")
if !check && !Files.isDirectory(Paths.get(localDir)) then
  die(s"local dir not found: $localDir  (render it first with `tt ssg`, or use --push/--release/--serve)")

// ---- read host + credentials from ~/.netrc ----
val netrc = Paths.get(System.getProperty("user.home"), ".netrc")
if !Files.isRegularFile(netrc) then
  die:
    s"""|no ~/.netrc
        |create it (chmod 600) with your one.com SFTP machine/login/password
        |(see this file's header).
        |""".stripMargin

// minimal .netrc tokenizer: machine <h> ... login <l> ... password <p> ... (until next machine/default)
val toks = Files.readString(netrc).split("\\s+").filter(_.nonEmpty).iterator
val entries = mutable.Map[String, (String, String)]()
var m, l, p = ""
def flush(): Unit = if m.nonEmpty then entries(m) = (l, p)
while toks.hasNext do
  toks.next() match
    case "machine"  => flush(); m = if toks.hasNext then toks.next() else ""; l = ""; p = ""
    case "default"  => flush(); m = ""; l = ""; p = ""
    case "login"    => if toks.hasNext then l = toks.next()
    case "password" => if toks.hasNext then p = toks.next()
    case "account" | "macdef" => if toks.hasNext then toks.next()  // skip the value
    case _ => ()
flush()

// deployblog reads only the machine (host) + login; the PASSWORD stays in ~/.netrc and is read by lftp itself
// (username-in-URL below), so no secret ever enters this script, the process list, or the output.
val (host, login) = entries.collectFirst {
  case (h, (u, _)) if h.endsWith(".service.one") && u.nonEmpty => (h, u)
}.getOrElse(die("no `machine <...>.service.one` entry (with a login) found in ~/.netrc."))

// ---- build the lftp command script (username in the URL; lftp reads the password from ~/.netrc itself) ----
val mirrorOpts = Seq(
  "-R",                                   // reverse = upload (local -> remote)
  if doDelete then "--delete" else "",
  if dryRun then "--dry-run" else "",
  "--verbose"
).filter(_.nonEmpty).mkString(" ")

val action =
  if check then "pwd\nls"                 // connection test: show the login directory, change nothing
  else s"mirror $mirrorOpts ${q(localDir)} ${q(remoteDir)}"

// ssh flags: accept-new pins the host key on first use; PubkeyAuthentication=no +
// PreferredAuthentications=password force password auth -- otherwise ssh offers all your
// keys first and one.com disconnects with "too many authentication failures".
val lftpScript =
  s"""|set sftp:connect-program "ssh -a -x -oStrictHostKeyChecking=accept-new -oPubkeyAuthentication=no -oPreferredAuthentications=password"
      |open sftp://$login@$host
      |$action
      |bye
      |""".stripMargin

// ---- report our OWN plan (synthesized from localDir/remoteDir -- never from lftp's credential-bearing output) ----
if check then
  println(s"deployblog: --check  connect to $host and list the login directory (read-only, no changes)")
else
  val mode = if doDelete then "exact mirror (--delete)" else "additive"
  println(s"deployblog: ${if dryRun then "DRY-RUN " else ""}mirror  $localDir  ->  $host:$remoteDir  ($mode)")
  val root = Paths.get(localDir)
  for f <- listLocalFiles(root) do println(s"  ${if dryRun then "would send" else "send"}  ${root.relativize(f)}")

// ---- run lftp CONTAINED: its output is captured locally and never printed on the normal path ----
val (code, captured) = runContained(lftpScript)
if code == 0 then
  if check then println(captured.trim)   // the remote listing IS the point of --check
  println(s"deployblog: done${if dryRun then " (dry-run: nothing changed)" else "."}")
  // --release: ONLY after a real (non-dry-run) successful upload, stamp the promoted posts deployed.
  if release && !dryRun then
    println("deployblog: upload OK  ->  promoting published:deployed")
    statusUpdate("published:deployed")
else
  System.err.println(captured)           // full diagnostics on failure (no password appears - lftp reads it from ~/.netrc)
  die(s"lftp exited $code -- check the ~/.netrc credentials, that SFTP is enabled, and the remote path.")
