//> using dep io.github.iltotore::iron:3.3.1

// blog/References.scala — typed, verification-tracked bibliography for the genscalator blog.
//
// PROTOCOL (echt / grounding, structuralised): every Reference carries an explicit RefVerification.
//   - Verified  : the citation data was checked against the source (DOI / publisher / arXiv page).
//   - ToDo      : not yet checked — DO NOT cite as fact in a post until verified (a recalled-but-wrong
//                 citation is *false echt*, doubly embarrassing in a thread about agent confabulation).
//   - Unverified: legacy/imported, status unknown.
// Never mark Verified without checking. "ToDo" is a first-class, greppable state, not a silent risk.
// See skills/blog-assistant (§ References). Data model is BibTeX-lite — enough to render a citation.

package blog

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.numeric.*
import io.github.iltotore.iron.constraint.string.*   // Blank / Match (string refinements)
import io.github.iltotore.iron.constraint.any.*       // Not (constraint combinator)

type Title      = String
type RefComment = String
type Year       = Int :| Interval.Closed[1900, 2100]  // Iron refinement: a plausible publication-year range.
type Summary    = String :| Not[Blank]  // Iron refinement: a summary must actually say something (non-empty, not whitespace-only).

/** A name split so we can render "Regnell, B." or "Björn Regnell". */
case class Author(lastName: String, otherNames: Seq[String], abbrevFirstLetterOfOtherNames: Seq[String])

/** Convenience: author("Ohlsson","Magnus","C") -> initials derived from the given names. */
def author(last: String, names: String*): Author =
  Author(last, names.toSeq, names.map(_.take(1)).toSeq)

enum RefKind:
  case Journal, Conference, Book, TechReport, Preprint, Web, Misc

// Iron (type-level validation) — LIVE for `Year` above: `Int :| Interval.Closed[1900, 2100]`, so a bad year (e.g.
//   1000) fails to COMPILE and the literal years below auto-refine at compile time (~zero runtime overhead).
//   TODO — extend where it pays: refine `doi` / `url` with `String :| Match[...]` patterns; refine runtime input via
//   `.refine` / `.refineEither` / `.refineOption` at the boundary. BIGGER WIN: the SAME Iron constraints can implement
//   tt's typed-arg validator layer (intRange / oneOf / FROM..TO in tools/DESIGN-single-dispatcher.md) — one validation
//   vocabulary shared across RefData and tt. Repo: https://github.com/Iltotore/iron · Docs: https://iltotore.github.io/iron/docs/
/** BibTeX-lite: optional fields cover journal / conference / book / techreport / preprint / web. */
case class RefData(
  kind:      RefKind,
  year:      Option[Year]   = None,
  venue:     Option[String] = None,  // journal / conference / repository name
  volume:    Option[String] = None,
  number:    Option[String] = None,  // issue, or arXiv id, or report number
  pages:     Option[String] = None,
  publisher: Option[String] = None,
  edition:   Option[String] = None,
  doi:       Option[String] = None,
  url:       Option[String] = None,
  note:      Option[String] = None,
)

enum RefVerification:
  case Unverified, Verified, ToDo

case class Reference(
  title:      Title,
  authors:    Seq[Author],
  refData:    Option[RefData],
  isVerified: RefVerification,
  comment:    RefComment,
  summary:    Option[Summary] = None,  // optional plain-language summary; Iron-guaranteed non-blank when present.
)

import RefKind.*, RefVerification.*

val references: Seq[Reference] = Seq(

  // ── SE research-methods foundations (BR is a co-author of both — be open about that when self-referencing) ──
  Reference(
    "Experimentation in Software Engineering",
    Seq(author("Wohlin","Claes"), author("Runeson","Per"), author("Höst","Martin"),
        author("Ohlsson","Magnus","C"), author("Regnell","Björn"), author("Wesslén","Anders")),
    Some(RefData(Book, year = Some(2024), publisher = Some("Springer"),
      edition = Some("3rd (1st ed. 2000 'An Introduction', Kluwer; 2nd ed. 2012, Springer)"),
      doi = Some("10.1007/978-3-662-69306-3"),
      url = Some("https://link.springer.com/book/10.1007/978-3-662-69306-3"))),
    Verified,
    "Standard SE textbook on controlled experiments; grounds our permutation-test + preregistration practice. self-ref: BR co-author (own it inline at first mention, not just in the ref list).",
  ),
  Reference(
    "Case Study Research in Software Engineering: Guidelines and Examples",
    Seq(author("Runeson","Per"), author("Höst","Martin"), author("Rainer","Austen"), author("Regnell","Björn")),
    Some(RefData(Book, year = Some(2012), publisher = Some("Wiley (Wiley-Blackwell)"), pages = Some("256"),
      note = Some("ISBN 9781118104354"),
      url = Some("https://www.wiley.com/en-us/Case+Study+Research+in+Software+Engineering%3A+Guidelines+and+Examples-p-9781118181003"))),
    Verified,
    "SE case-study methodology; relevant to the qualitative-synthesis alternative to a full SLR. self-ref: BR co-author (own it inline at first mention, not just in the ref list).",
  ),

  // ── machine psychology / agent-psyche cluster ──
  Reference(
    "Machine Psychology: Investigating Emergent Capabilities and Behavior in Large Language Models Using Psychological Methods",
    Seq(author("Hagendorff","Thilo")),
    Some(RefData(Preprint, year = Some(2023), venue = Some("arXiv"), number = Some("2303.13988"),
      url = Some("https://arxiv.org/abs/2303.13988"))),
    Verified,
    "Nearest established umbrella term to our 'agent psyche' — psychology methods applied to LLM behaviour.",
  ),
  Reference(
    "Using cognitive psychology to understand GPT-3",
    Seq(author("Binz","Marcel"), author("Schulz","Eric")),
    Some(RefData(Journal, year = Some(2023), venue = Some("PNAS"), volume = Some("120"), number = Some("6"),
      pages = Some("e2218523120"), doi = Some("10.1073/pnas.2218523120"),
      url = Some("https://www.pnas.org/doi/10.1073/pnas.2218523120"), note = Some("also arXiv:2206.14576"))),
    Verified,
    "Peer-reviewed (PNAS): human cognitive-psychology batteries applied to an LLM.",
  ),
  Reference(
    "Machine behaviour",
    Seq(author("Rahwan","Iyad"), author("Cebrian","Manuel")), // + many co-authors (et al.)
    Some(RefData(Journal, year = Some(2019), venue = Some("Nature"), volume = Some("568"), number = Some("7753"),
      pages = Some("477-486"), doi = Some("10.1038/s41586-019-1138-y"),
      url = Some("https://www.nature.com/articles/s41586-019-1138-y"),
      note = Some("many co-authors (et al.); manifesto for an empirical behavioural science of machines"))),
    Verified,
    "Grounds our behavioural-adjudication stance (study machines by their behaviour).",
  ),
  Reference(
    "Towards Understanding Sycophancy in Language Models",
    Seq(author("Sharma","Mrinank"), author("Tong","Meg"), author("Perez","Ethan")), // + others, Anthropic
    Some(RefData(Preprint, year = Some(2023), venue = Some("arXiv"), number = Some("2310.13548"),
      url = Some("https://arxiv.org/abs/2310.13548"), note = Some("Anthropic; many co-authors (et al.)"))),
    Verified,
    "Empirical grounding for our niceness-corrupts-honesty / sycophancy point.",
  ),
  Reference(
    "ELIZA — A Computer Program For the Study of Natural Language Communication Between Man and Machine",
    Seq(author("Weizenbaum","Joseph")),
    Some(RefData(Journal, year = Some(1966), venue = Some("Communications of the ACM"), volume = Some("9"),
      number = Some("1"), pages = Some("36-45"), doi = Some("10.1145/365153.365168"),
      url = Some("https://courses.cs.umbc.edu/331/papers/eliza.html"),
      note = Some("free copy at UMBC; origin of the 'ELIZA effect'"))),
    Verified,
    "Origin of the ELIZA effect (human over-attribution of understanding to a conversational program).",
  ),

  // ── was ToDo, now VERIFIED (2026-07-04, >=2 authoritative sources each; corrections applied per entry) ──
  Reference(
    "Personality Traits in Large Language Models",
    Seq(author("Serapio-García","Greg"), author("Safdari","Mustafa"), author("Crepy","Clément"),
        author("Sun","Luning"), author("Fitz","Stephen"), author("Romero","Peter"),
        author("Abdulhai","Marwa"), author("Faust","Aleksandra"), author("Matarić","Maja")),
    Some(RefData(Preprint, year = Some(2023), venue = Some("arXiv"), number = Some("2307.00184"),
      doi = Some("10.48550/arXiv.2307.00184"),
      url = Some("https://arxiv.org/abs/2307.00184"),
      note = Some("v1 2023-07-01 (latest v4 2025-03-11); 9 authors. 'Google DeepMind' affiliation not confirmed on the arXiv page."))),
    Verified,
    "LLM Big-Five/OCEAN personality measurement. Verified: arXiv id 2307.00184 + full 9-author list (arXiv abstract page + HF Papers).",
  ),
  Reference(
    "The Media Equation: How People Treat Computers, Television, and New Media Like Real People and Places",
    Seq(author("Reeves","Byron"), author("Nass","Clifford")),
    Some(RefData(Book, year = Some(1996),
      publisher = Some("Cambridge University Press (hardcover); CSLI Publications, CSLI Lecture Notes (paperback)"),
      note = Some("paperback ISBN 9781575860534"))),
    Verified,
    "Foundational anthropomorphism-of-media work. Verified: 1996, author order Reeves-Nass, both publishers (Wikipedia + Stanford CSLI + ACM guide).",
  ),
  Reference(
    "Guidelines for performing Systematic Literature Reviews in Software Engineering",
    Seq(author("Kitchenham","Barbara"), author("Charters","Stuart")),
    Some(RefData(TechReport, year = Some(2007),
      publisher = Some("Keele University and University of Durham Joint Report (EBSE)"),
      number = Some("EBSE 2007-001"), note = Some("Version 2.3"))),
    Verified,
    "The SE SLR standard. BR's view: may be over-arching given scarce hard empirical evidence in SE (see lit-review note). Verified: report EBSE 2007-001 v2.3, Keele+Durham.",
  ),
  Reference(
    "Guidelines for snowballing in systematic literature studies and a replication in software engineering",
    Seq(author("Wohlin","Claes")),
    Some(RefData(Conference, year = Some(2014),
      venue = Some("EASE '14 (18th Intl. Conf. on Evaluation and Assessment in Software Engineering)"),
      pages = Some("38:1-38:10"), publisher = Some("ACM"),
      doi = Some("10.1145/2601248.2601268"),
      url = Some("https://doi.org/10.1145/2601248.2601268"))),
    Verified,
    "Snowball sampling as a lighter-weight search strategy — BR's preferred alternative to a full protocol. Verified: DOI 10.1145/2601248.2601268, pages 38:1-38:10 (DBLP + ACM DL + OpenAIRE).",
  ),
  Reference(
    "Theory of Mind May Have Spontaneously Emerged in Large Language Models",
    Seq(author("Kosinski","Michal")),
    Some(RefData(Preprint, year = Some(2023), venue = Some("arXiv"), number = Some("2302.02083"),
      url = Some("https://arxiv.org/abs/2302.02083"),
      note = Some("v1 title (2023-02-04). The SAME arXiv id was later retitled 'Evaluating Large Language Models in Theory of Mind Tasks' and published in PNAS 121(45), 2024, doi 10.1073/pnas.2405460121, with a softer claim. Cite the v1 title for the contested 'emerged' framing; rebuttal to check before citing: Ullman 2023, arXiv:2302.08399."))),
    Verified,
    "Theory-of-mind-in-LLMs (contested) — useful for the unfalsifiable-from-inside point. Verified: arXiv id 2302.02083 (v1); carries the retitle/PNAS note to avoid a stale-id trap.",
  ),
)
