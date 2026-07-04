//> using scala 3.8.3

// Mine the WebFetch domains surfed in one session's transcripts (main + subagent task outputs), so they can be
// reviewed and (with the human's approval) persisted as WebFetch(domain:...) allow-rules — stopping the harness from
// re-prompting next session. Supports the allowlist-mining WR finding in harness-ux.md.
//
// NOTE (WR, 2026-07-04): the harness already auto-persists "always allow" grants into .claude/settings.local.json, so
// most domains are already there; this miner's live value is AUDIT — diff its output against the settings allowlist to
// catch "just this session" grants that were NOT persisted. Curation of what to allow is the HUMAN's call (security
// judgment can't be delegated — the authority-anchor point). And prefer direct authoritative sources over fetch-proxies
// (r.jina.ai / webcache.googleusercontent.com): a proxy launders grounding and is a broad fetch surface.
//
// Usage: scala-cli run mine-approved-domains.scala -- <session-tasks-dir-or-jsonl> [more paths ...]

import java.nio.file.*
import scala.jdk.CollectionConverters.*

@main def mineDomains(paths: String*): Unit =
  val urlRe = """"url"\s*:\s*"(https?://[^"]+)"""".r
  val domains = scala.collection.mutable.TreeSet[String]()

  def scanFile(f: Path): Unit =
    if Files.isRegularFile(f) then
      val stream = Files.lines(f)
      try
        stream.iterator.asScala.foreach { line =>
          if line.contains("\"name\":\"WebFetch\"") then
            urlRe.findAllMatchIn(line).foreach { m =>
              val url = m.group(1)
              val afterScheme = url.substring(url.indexOf("://") + 3)
              val host = afterScheme.takeWhile(c => c != '/' && c != ':' && c != '"')
              if host.nonEmpty then domains += host
            }
        }
      finally stream.close()

  for p <- paths do
    val path = Paths.get(p)
    if Files.isDirectory(path) then
      val ds = Files.list(path)
      try ds.iterator.asScala.foreach(scanFile) finally ds.close()
    else scanFile(path)

  System.err.println(s"scanned ${paths.size} path(s); found ${domains.size} unique WebFetch domain(s)")
  println("# WebFetch domains surfed this session:")
  domains.foreach(println)
  println()
  println("# settings permissions.allow candidates (HUMAN curates before persisting):")
  domains.foreach(d => println(s"""    "WebFetch(domain:$d)","""))
