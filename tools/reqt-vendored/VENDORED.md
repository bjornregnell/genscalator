# reqt-vendored — provenance

This directory is a **vendored, pristine copy of the reqT-lang parsing core**, used by
`tt parsereqt` / `tt reqt` to parse reqT-lang requirements (e.g. `reqts/PRD.md`).

## Source baseline
- Upstream: reqT-lang — https://github.com/reqT/reqT-lang
- Baseline commit: **`df6a7f2`**, version **4.6.1** (`build.sbt` `reqTLangVer`), from `src/main/scala/`
- Local clone: `/home/bjornr/git/hub/reqT/reqT-lang`
- Vendored into genscalator in commit `77c195a` ("go 2: tt reqt tool (vendored reqT-lang parser)")

## What is vendored
Every `src/main/scala/*.scala` from that baseline **except `05-Quper.scala`**. Verified
2026-07-24 (`git diff --no-index`, whole-tree stat + per-file): the vendored files are
**byte-identical to upstream** — the only difference is the dropped Quper file.

## Why `05-Quper.scala` is pruned
It renders the Quper quality-model to **SVG** using `scala.xml` (`Elem`, `PrettyPrinter`, XML
literals), which pulls the external **`scala-xml`** dependency. genscalator uses reqT only for
**parsing**, so Quper is irrelevant, and pruning it keeps `scala-xml` out of the flat `tt`
classpath and the native image. Recorded in `research/015-reqt-lang-review.md`. This reason
**evaporates on de-vendor** — a real dependency resolves `scala-xml` transitively — see Endgame.

## What we did NOT change here — read this to avoid a recurring confusion
- **The vendored parser is UNMODIFIED.** We do not edit reqT-lang directly (changing it cascades
  release + docs work on the reqT desktop tool — BR's working model, 2026-07-01).
- **genscalator's own markdown parser is a SEPARATE file:** `tools/mdparse.scala` (`MdParse.parse`),
  a hand-rolled GitHub-flavored-markdown parser feeding `tt ssg` + `tt md-fmt` (the website / blog).
  It is not this and not vendored. `reqt-vendored/05-MarkdownParser.scala` parses the reqT-lang
  requirements markdown-subset (ENT/REL/ATTR); `mdparse.scala` parses general GFM prose.
- **genscalator's strict/lint error-handling over reqT is a WRAPPER:** `tools/parsereqt.scala`
  (`ParseReqt.lint`) flags content that silently fell through to a `Text` attribute — typos,
  un-mapped concepts, and relations lost under a `has` block. It does not fork the parser, so this
  copy stays diff-clean. The NATIVE in-parser strict mode is a pending upstream contribution:
  reqT/reqT-lang#15.

## Known warnings (all UPSTREAM reqT's, present identically here — not local edits)
- `method next must be called with () argument` (Scala-3 deprecation): `00-zero-dep-utils.scala:100`,
  `04-ModelCompanion.scala:40`, `05-MarkdownParser.scala:20/22/23`.
- `The package name NN-…$package will be encoded on the classpath …`: `01-api-exports.scala`,
  `03-model-GENERATED.scala`, `06-examples.scala` (reqT's numbered top-level-definition files).

Harmless (suite green, native image builds). The durable fix, if ever wanted, belongs upstream
in reqT-lang, not here.

## Re-vendoring
Copy `src/main/scala/*.scala` from the reqT-lang baseline into this dir, then delete
`05-Quper.scala`. Update the baseline commit + version above.

## Endgame
Vendoring is temporary scaffolding. When reqT-lang is released and depended on as a library
(`io.github.reqt` :: `reqt-lang`), this directory is deleted and `tt` depends on the published
artifact — Quper / `scala-xml` return for free as a transitive dep, and the upstream warnings
become reqT's concern. Tracked as SM222 (BR: cut a reqT-lang release, then de-vendor).
