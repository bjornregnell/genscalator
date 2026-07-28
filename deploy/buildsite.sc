//> using dep com.lihaoyi::os-lib:0.11.8

// buildsite — assemble the WHOLE deployable site into ONE directory, `out/`, so that deploying is a
// single upload of a single tree instead of three pipelines with three destinations.
//
//   scala-cli run deploy/buildsite.sc -- --dry-run     # say what would be built, write nothing
//   scala-cli run deploy/buildsite.sc                  # build out/
//   scala-cli run deploy/buildsite.sc -- --root <abs>  # explicit checkout root (else cwd)
//
// ⭐ `out/` MIRRORS THE DEPLOYED SITE, NOT THE REPO. That one choice is what makes relative links honest
// by construction, and it is worth stating because the obvious alternative (mirror the repo layout) is
// subtly worse:
//
//     out/                      ->  webroots/www/genscalator/
//       index.html                    <- docs/manual-src/index.md
//       getting-started.html          <- docs/manual-src/getting-started.md
//       foundations.html  tool-selection.html  allowlist.html  statusline-manual.html
//       blog/                         <- media/blog (published + deployed only)
//       img/                          <- media/img
//       graphical-profile/            <- media/graphical-profile
//       api/                          <- scaladoc (see NOT YET WIRED, below)
//
// Why site-mirroring: a blog post's `../img/x.png` resolves because out/blog -> out/img is the real
// on-site relationship; and the manual's sibling links (`foundations.html` from `index.html`) resolve
// because those pages ARE siblings at the site root. Repo-mirroring would preserve neither, and it would
// ALSO make `tt links check` agree with a layout no reader ever visits - a green check that proves
// nothing. Checking `out/` is checking the site.
//
// ⚠ Links from a published page to REPO-ONLY paths (`../research/`, `../tools/README.md`) will still
// dangle here, and that is CORRECT rather than a regression: they are genuinely broken for a reader,
// because those trees are not published. Fix them at the source, or point them at a public URL.
//
// ⚠⚠ NOT YET WIRED: `out/api/`. Held deliberately; tracked as a JOINT task on the pin board.
//
// The DESTINATION IS DECIDED (BR, 2026-07-28): `https://bjornregnell.se/genscalator/api`, so inside this
// tree it is `out/api/` - the path under `out/` is just the site URL with the domain and the
// `/genscalator` prefix removed, which is the whole benefit of mirroring the site rather than the repo.
//
// The alternative considered and REJECTED was `/genscalator/docs/api`, and the reason is worth keeping
// because it will come up again: `/genscalator/docs/` would be a level containing NOTHING BUT `api`,
// since the actual docs (foundations, tool-selection, getting-started) render flat at the site ROOT. A
// reader who trims that URL gets a 404, and every other section here is exactly one level down
// (`blog/`, `img/`, `graphical-profile/`), so `api/` matches the shape the site already has. It also
// keeps the repo's internal `docs/` directory name out of public URLs.
// ⇒ If the whole manual ever moves under `/genscalator/docs/`, move the api there IN THE SAME CHANGE -
// doing the api first is what creates the orphaned level, and moving the manual later breaks every
// existing inbound link.
//
// WHY THE WIRING IS HELD: `deployttapi.sc` generates into `docs/generated/api/` and CLEARS that
// directory under a path-pinned safety check. Getting a directory-clearing path wrong is the one mistake
// in this area that destroys work, so it wants its own change and its own dry-run.
// ⇒ Also found while writing this (deployttapi.sc:7): the api site has NEVER been uploaded. Its header
// says "a LATER step will deploy to bjornregnell.se" and that step was never built. Unifying under one
// push is what finally closes that.

val argv   = args.toList
val dryRun = argv.contains("--dry-run")
def flag(n: String): Option[String] =
  val i = argv.indexOf(n); if i >= 0 && i + 1 < argv.size then Some(argv(i + 1)) else None

val root = flag("--root").map(os.Path(_, os.pwd)).getOrElse(os.pwd)
val out  = flag("--out").map(os.Path(_, os.pwd)).getOrElse(root / "out")
val tt   = root / "tools" / "tt"

if !os.exists(root / "tools" / "tt") then
  System.err.println(s"buildsite: $root does not look like the genscalator root (no tools/tt)")
  sys.exit(2)

/** The manual set, in the order the regenerate note in docs/manual-src/index.md lists them. These render
  * FLAT into the site root, which is what makes their sibling links correct. */
val manualPages = Vector(
  "docs/manual-src/index.md",
  "docs/manual-src/getting-started.md",
  "docs/foundations.md",
  "docs/tool-selection.md",
  "docs/allowlist.md",
  "docs/statusline-manual.md",
)

/** Static trees copied verbatim. The site needs the bytes; nothing is rendered. */
val staticTrees = Vector(
  "media/img"               -> "img",
  "media/graphical-profile" -> "graphical-profile",
)

def run(label: String, cmd: Seq[String]): Unit =
  if dryRun then println(s"  would run: $label")
  else
    val r = os.proc(cmd).call(cwd = root, check = false, stdout = os.Inherit, stderr = os.Inherit)
    if r.exitCode != 0 then
      System.err.println(s"buildsite: FAILED at $label (exit ${r.exitCode})")
      sys.exit(r.exitCode)

println(s"buildsite${if dryRun then "  (DRY RUN: nothing will be written)" else ""}")
println(s"  root: $root")
println(s"  out:  $out")

// 1. the manual, flat at the site root
val missing = manualPages.filterNot(p => os.exists(root / os.RelPath(p)))
if missing.nonEmpty then
  System.err.println(s"buildsite: missing source page(s): ${missing.mkString(", ")}")
  sys.exit(2)
println(s"  manual: ${manualPages.size} page(s) -> $out")
run("ssg manual", Vector(tt.toString, "ssg", "--out", out.toString) ++ manualPages)

// 2. the blog, under out/blog, only the posts whose status says they may ship
println(s"  blog:   published+deployed -> $out/blog")
run("ssg blog", Vector(tt.toString, "ssg", "--status", "published,deployed", "--out", (out / "blog").toString, "media/blog"))

// 3. static trees
staticTrees.foreach { (src, dst) =>
  val from = root / os.RelPath(src)
  if !os.exists(from) then println(s"  static: SKIP $src (absent)")
  else
    println(s"  static: $src -> $out/$dst")
    if !dryRun then
      os.makeDir.all(out / dst)
      os.list(from).filter(os.isFile).foreach(f => os.copy.over(f, out / dst / f.last))
}

println()
if dryRun then println("DRY RUN complete - nothing written. Re-run without --dry-run to build.")
else
  println(s"built: $out")
  println(s"  preview:  tt serv $out")
  println(s"  check:    tt links check $out     # this is the SITE check, not the repo check")
  println(s"  deploy:   see deployblog.sc for the sftp transport (one upload of $out)")
