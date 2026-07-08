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
// USAGE
//   # render the deploy set first (adjust to whatever you are deploying):
//   tt ssg blog/index.md tmp/site
//   tt ssg blog/000-why-genscalator.md tmp/site
//   tt ssg blog/002-braceful-or-braceless-or-the-common-style.md tmp/site
//
//   # ALWAYS dry-run first to confirm the target path before touching the live site:
//   scala-cli run deployblog.sc -- --dry-run
//
//   # then deploy for real:
//   scala-cli run deployblog.sc                                   # defaults: tmp/site -> webroots/www/blog (additive)
//   scala-cli run deployblog.sc -- tmp/site webroots/www/blog      # explicit local + remote dir
//   scala-cli run deployblog.sc -- tmp/site webroots/www/blog --delete  # exact mirror (removes stale remote files)
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

import java.nio.file.{Files, Paths}
import scala.collection.mutable

def die(msg: String): Nothing = { System.err.println(s"deployblog: $msg"); sys.exit(2) }

def onPath(exe: String): Boolean =
  Option(System.getenv("PATH")).exists(_.split(java.io.File.pathSeparator)
    .exists(d => Files.isExecutable(Paths.get(d, exe))))

// lftp-quote a token (double quotes, backslash-escaped) so passwords/paths with
// special characters survive; the value travels on lftp's stdin, never on argv.
def q(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

// ---- args ----
val positional = args.filterNot(_.startsWith("--")).toList
val flags      = args.filter(_.startsWith("--")).toSet
val localDir   = positional.lift(0).getOrElse("tmp/site")
val remoteDir  = positional.lift(1).getOrElse("webroots/www/blog")
val doDelete   = flags.contains("--delete")
val dryRun     = flags.contains("--dry-run")
val check      = flags.contains("--check")

// ---- checks ----
if !onPath("lftp") then die("`lftp` not found. Install it:  sudo apt install lftp")
if !check && !Files.isDirectory(Paths.get(localDir)) then
  die(s"local dir not found: $localDir  (render it first with `tt ssg <post> $localDir`)")

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

val (host, login, password) = entries.collectFirst {
  case (h, (u, pw)) if h.endsWith(".service.one") && u.nonEmpty && pw.nonEmpty => (h, u, pw)
}.getOrElse(die("no complete `machine <...>.service.one` entry (with login + password) found in ~/.netrc."))

// ---- build the lftp command script (creds go on stdin, never on the command line) ----
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
      |open sftp://$host
      |user ${q(login)} ${q(password)}
      |$action
      |bye
      |""".stripMargin

println(
  if check then s"deployblog: --check  connect to $host and list the login directory (read-only, no changes)"
  else s"deployblog: ${if dryRun then "DRY-RUN " else ""}mirror  $localDir  ->  $host:$remoteDir  (${if doDelete then "exact mirror, --delete" else "additive"})"
)

// ---- run lftp, feeding the script on its stdin ----
val pb = new ProcessBuilder("lftp")
pb.redirectErrorStream(true)
pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
val proc = pb.start()
proc.getOutputStream.write(lftpScript.getBytes("UTF-8"))
proc.getOutputStream.close()
val code = proc.waitFor()
if code == 0 then println(s"deployblog: done${if dryRun then " (dry-run: nothing changed)" else "."}")
else die(s"lftp exited $code -- check the ~/.netrc credentials, that SFTP is enabled, and the remote path.")
