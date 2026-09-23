// (no version include: mainless helper — inherits it from its includer; see project.scala)

// ability — ONE declared description per `tt` verb, PROJECTED into the surfaces that show it
// (issue 041). Pure: data plus string rendering, no I/O.
//
// Before this, every verb hand-maintained its description in THREE carriers: the
// `### <verb> — <tagline>` heading in tools/README.md (the source `gs help tt` reads, per
// skills/gs-dwim/SKILL.md:36-37), the tool's own `--help` string, and the `case _ =>` usage block it
// prints on bad arguments. Nothing related them, and all six verbs that carried all three DISAGREED
// across them. Four — guardcheck, harden, typo, wr — dropped the PURE/EFFECTFUL classification, the
// most safety-relevant fact about a verb, from the third carrier entirely. They survived because
// `case _ =>` is the bad-arguments path and the one test that looked at descriptions ran `--help`.
//
// Why DERIVE rather than assert agreement (version.test.scala's shape, strategy two): an agreement
// test has to be TOLD which strings are carriers, and that enumeration is the same act that produced
// the drift. The six were found by sweeping tools/*.scala for a `println("""…` block whose first line
// reads like a tagline, so six is a FLOOR and nobody knows the total — an agreement gate built today
// would be built against exactly the six we happened to find, and would certify the rest as clean. A
// projection has no enumeration step at all: a carrier renders from the declaration or it has no text
// at all, so the unfound tail stops existing rather than staying unfound. skillcheck.scala:9 is the
// reference shape (its expected set is DERIVED from disk, "so it never drifts").
//
// WHY `Ability` AND NOT `Capability`, which is what issue 041 calls this in prose. Two reasons, and
// the second is the one that decided it:
//   1. Scala 3.9 ships `scala.caps.Capability`, the marker trait of the experimental CAPTURE-CHECKING
//      feature, where a "capability" is a value whose TYPE tracks an effect. There is no name
//      collision to fix — probed on the pinned 3.9.0-RC4: `scala.caps` is not auto-imported,
//      `import language.experimental.captureChecking` does not import `caps.*`, and even an explicit
//      `import scala.caps.*` leaves the TERM `Capability` bound to a root-package object, since the
//      caps one is a trait. But on a toolbox that tracks bleeding-edge Scala AND classifies its verbs
//      PURE/EFFECTFUL, `Capability` is precisely the name that would send a reader looking for `^`
//      and capture sets. The concepts are adjacent enough to mislead while being unrelated.
//   2. `ability` is the PRIOR ART'S OWN WORD, not a coinage to dodge (1). The shape this file borrows
//      is soundness PR #1833's `@ability` / `@about`, cited in issue 041 for exactly this: one
//      annotation whose description serves a method exposed both as an MCP tool and as a model
//      ability. And it is the vocabulary Phase 2 renders INTO. So the code names the thing after what
//      it will project into, and the issue's "capability projection" stays the name of the problem.
//
// This must NOT become a fourth place to keep in sync — that is the way it fails. So the declarations
// live on the `object <Tool>` that already holds each verb's pure core, and `ProjectedAbilities` (in
// dispatch.scala, the one file allowed to name every tool) only COLLECTS references to them: it holds
// no copy of any fact, and a renamed field breaks the build rather than drifting.
//
// PHASE 1 ONLY, per bjornregnell's decision on issue 041 (2026-08-25): the six verbs with PROVEN
// drift, `text` first, sequenced deliberately — "a 46-verb mechanical diff arriving in one PR is not
// reviewable at the level this repo reviews things, and the design would be getting ratified by its
// own diff size". Phase 2 (a parameter SCHEMA rendered from the same declaration, the enabler for
// `Feature: mcpServer`, reqts/PRD.md:411, UNSCHEDULED) stays gated on a typed layer beneath
// `args: String*` and is deliberately not started here.
object Ability {

  /** What a verb does to the world, as a VALUE rather than an adjective in a markdown heading.
    *
    * This is the payload that makes the projection worth more than tidiness. The classification
    * decides whether a verb belongs in the allowlist, whether it needs a preview default, and — the
    * moment the toolbox is exposed over any protocol — what a caller may invoke unattended. A verb
    * that gains a write path and does not get its tagline updated used to be silently misfiled as
    * safe; now it cannot be, because the tagline is not a separate thing anyone has to remember to
    * update.
    *
    * A DECLARED effect class, not a checked one: it says what the verb does, and nothing in the type
    * system enforces it. (That is the honest difference from capture checking — see the note above.) */
  enum Effect:
    /** Reads, computes, prints. `readOnly` additionally records that it writes NO file at all. */
    case Pure(readOnly: Boolean)
    /** Pure until an explicitly guarded flag is passed — md-fmt's `--write` is the shape. */
    case PureByDefault(guardedEffect: String)
    /** Effects in normal use. `previewDefault` = a dry run is what you get WITHOUT opting in. */
    case Effectful(detail: String, previewDefault: Boolean)

    /** The classification as a tagline states it.
      *
      * `shout` selects the tools/README.md form, where it is capitalised because it is a safety
      * CLAIM, not a description; otherwise the running-prose form a `--help` tagline uses. The two
      * are written out rather than derived from each other by `toLowerCase`, so a future
      * classification whose prose form is not a mechanical lowercasing cannot render wrong. */
    def marker(shout: Boolean): String =
      def pick(caps: String, prose: String): String = if shout then caps else prose
      this match
        case Pure(false)        => pick("PURE", "pure")
        case Pure(true)         => pick("PURE, read-only", "pure, read-only")
        case PureByDefault(g)   => pick(s"PURE by default; $g", s"pure by default; $g")
        case Effectful(d, prev) =>
          val head = pick(
            if prev then "EFFECTFUL; PREVIEW BY DEFAULT" else "EFFECTFUL",
            if prev then "effectful; preview by default" else "effectful")
          if d.isEmpty then head else s"$head: $d"

  /** One verb's declared ability — the single source its descriptions render from.
    *
    * `summary` is ONE sentence fragment that has to read correctly in both renderings below, because
    * they are the same sentence twice. It carries no purity marker of its own: the classification is
    * `effect`, and appending it is the renderer's job. */
  case class Decl(verb: String, summary: String, effect: Effect):

    /** The tools/README.md `## Tools` heading — the source of truth `gs help tt` reads. */
    def readmeHeading: String = s"### $verb — $summary (${effect.marker(shout = true)})"

    /** The FIRST LINE of both the `--help` text and the `case _ =>` usage block: one string where
      * there were two, so the bad-arguments path no longer gets to describe the verb differently or
      * (as four of the six did) omit its classification. Names the verb as `tt <verb>`, the form a
      * caller actually types — the old `text` usage block called the tool `texttool`. */
    def tagline: String = s"tt $verb — $summary (${effect.marker(shout = false)})"

  /** Reads, computes, prints — may write a file it is explicitly told to write. */
  def pure(verb: String, summary: String): Decl = Decl(verb, summary, Effect.Pure(readOnly = false))

  /** Reads, computes, prints, and writes nothing at all. */
  def pureRead(verb: String, summary: String): Decl = Decl(verb, summary, Effect.Pure(readOnly = true))
}
