# Scoping note: static-site generator (SSG) for publishing the genscalator blog to bjornregnell.se

**Provenance:** agent-scouted 2026-07-04 (background research agent, part of the AFK menu). Every tool claim is grounded
in a live doc/repo check or in reading the blog source — but **no candidate was built locally**, so capabilities are
from official docs/comparisons, not local reproduction. The obvious next step is a ~30-min **Laika-library spike** on two
real posts (003 with its SVG + cross-links; 006 with its PNG + `References`) to confirm anchor rewriting + citation
rendering before committing. This note is the input to the **SSG WR case-study** (see `notes/br-todo-2026-07-04.md`).

## 1. What bjornregnell.se is today
- **Hosting:** plain **Apache** behind a **Varnish** cache (`server: Apache`, `via: Varnish`). A classic static-file host
  — no server-side build. Deploy model is "generate locally → upload files" (same shape as muntabot's `publish.sh`
  rsync-to-fileadmin).
- **Content:** a single hand-written static HTML page, ~1 KB, `last-modified: 2023-10-19` (untouched ~3 years): photo,
  name/title, one-line bio, five outbound links. No framework, no generator meta, no nav, no post list, no feed.
- **Implication:** a clean slate. Any SSG that emits self-contained static files drops onto this Apache host with zero
  server changes.

## 2. The blog's actual shape (what the generator must handle)
- **Source:** `NNN-slug.md` (zero-padded sequence, no dates in filenames), GitHub-flavored Markdown.
- **Custom conventions, currently plain Markdown (not front-matter):** a **Status banner** (lifecycle
  `initialized → drafted → published → deployed → updated`), an **Audience** line, and the **004→008 arc** with
  "backwards cliffhanger" inter-post nav.
- **Cross-links:** relative `.md` links between posts *and* in-page `#anchor` links → the generator must rewrite
  `.md`→`.html` and preserve heading anchors.
- **Code blocks:** Scala, fenced → wants syntax highlighting.
- **Figures:** hand-authored **SVG** referenced as `![alt](figures/x.svg)`, plus one PNG. Small, self-contained.
  `<img>` works everywhere; inlining (for theme-aware CSS) is a nice-to-have only Scala-native/hand-rolled make easy.
- **KEY FINDING — no real math.** All notation is **Unicode** (`χ²`, `≈`, `≥`, `−`, `·`, `→`); **no** `$…$`/LaTeX
  anywhere → **no MathJax/KaTeX needed.** Removes the single biggest source of SSG complexity/JS weight.
- **`References.scala` is the discriminator.** The bibliography is a **typed Scala program** (Iron-refined `Year`,
  `enum RefKind/RefVerification`, `references: Seq[Reference]`, a first-class `Verified/ToDo` gate). Only a **JVM/Scala**
  renderer consumes it natively (call `references`, render citations, even *fail the build* on a `ToDo` cited as fact).
  Any non-JVM SSG forces serialising it to JSON/TOML — breaking "the types are the source of truth".

## 3. Candidates (surveyed, not built)
- **A. Laika (Typelevel) — best ethos fit.** Mature (**1.0**, "after 11 years"). Runs as sbt plugin, **plain Scala
  library (cats-effect)**, or Scala.js. Markdown in; HTML/EPUB/PDF out; built-in syntax highlighting; preview server;
  **directives for nav trees / breadcrumbs / auto-numbering** (direct match for the 004→008 arc); **raw-content
  passthrough** (inline SVG). As a library, a generator can `import blog.references` and render citations in-process →
  typed-bibliography dogfood stays intact. Cost: a real dependency with a directive/template model to learn.
- **A′. Hand-rolled scala-cli generator — max dogfood.** A single `scala-cli` script: parse the status banner, wire arc
  nav, render `References.scala` citations, emit self-contained HTML — using Laika's library API *or* flexmark/
  commonmark-java as the Markdown→HTML core. Fits `tt`/genscalator style exactly; own every line incl. build-time
  checks (reject a cited `ToDo`, verify figure paths). Cost: you build templating/nav/highlighting.
- **B. Zola (mainstream minimal).** Rust single binary, zero runtime deps, Tera templates, built-in highlighting/Sass/
  search, sub-second builds. Trivial to deploy. **Downside:** off-ecosystem → `References.scala` must be exported to
  TOML/JSON (impedance mismatch with the typed-bib ethos). (Hugo = comparable Go equivalent; Eleventy/Astro pull in a
  Node toolchain genscalator avoids.)
- **C. Hand-rolled non-Scala.** Collapses into A′ for a Scala/`tt` shop — no reason to lose `References.scala`. Not
  recommended over A′.

## 4. Recommendation
| Option | Setup | Maintenance | Ethos fit | `References.scala` + SVG + arc/status | Deploy |
|---|---|---|---|---|---|
| **Laika (library API from scala-cli/sbt)** — *primary* | Medium | Low–med | **Strong** (Scala/FP, same-process) | Native refs; directives do arc-nav/numbering; raw-HTML inlines SVG | HTML → rsync |
| **Hand-rolled scala-cli generator** — *if Laika feels heavy* | Medium | Medium | **Strongest** (pure `tt`-style tool) | Full control: build-time `ToDo`-citation gate, path checks, inline SVG | HTML → rsync |
| **Zola** — *pragmatic fallback* | **Low** | **Lowest** | Weak (Rust/Tera, off-ecosystem) | Must serialise refs → TOML (breaks dogfood) | HTML → rsync |

**Bottom line:** lead with **Laika via its library API**, invoked from a small `scala-cli`/`tt`-style entry point — the
honest sweet spot: a mature library does the hard parts (Markdown, highlighting, nav, cross-link rewriting, optional
PDF/EPUB) while the render stays in-process so `References.scala` + its `Verified/ToDo` gate remain the typed source of
truth. Fall to the **hand-rolled scala-cli generator** if Laika's directive model is more framework than wanted; fall to
**Zola** only if the priority flips to zero-maintenance-fastest-deploy and the typed-bib dogfood is negotiable. All three
emit self-contained files onto the existing Apache/Varnish host, and none need MathJax (Unicode-only math).

**Two decisions regardless of tool:** (a) keep **Status/Audience/arc** as plain Markdown (any tool renders it free) *or*
promote to structured front-matter to drive automatic nav + a `deployed`-vs-`updated` staleness view (more power/work —
and the front-matter could itself be a typed Scala model, extending the dogfood); (b) **inline** the SVGs (theme-aware;
Laika/hand-rolled only) or keep them as `<img>` (works everywhere).

**Sources:** Laika (typelevel.org/Laika, GitHub, features + library-API pages) · Zola (getzola.org, GitHub) ·
flexmark-java · CloudCannon "top five SSGs 2025" · "Hugo vs Eleventy" (bobrockefeller).
