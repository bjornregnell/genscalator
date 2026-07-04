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

type Title      = String
type RefComment = String

/** A name split so we can render "Regnell, B." or "Björn Regnell". */
case class Author(lastName: String, otherNames: Seq[String], abbrevFirstLetterOfOtherNames: Seq[String])

/** Convenience: author("Ohlsson","Magnus","C") -> initials derived from the given names. */
def author(last: String, names: String*): Author =
  Author(last, names.toSeq, names.map(_.take(1)).toSeq)

enum RefKind:
  case Journal, Conference, Book, TechReport, Preprint, Web, Misc

/** BibTeX-lite: optional fields cover journal / conference / book / techreport / preprint / web. */
case class RefData(
  kind:      RefKind,
  year:      Option[Int]    = None,
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
)

import RefKind.*, RefVerification.*

val references: Seq[Reference] = Seq(

  // ── SE research-methods foundations (BR is a co-author of both — DISCLOSED CONFLICT OF INTEREST) ──
  Reference(
    "Experimentation in Software Engineering",
    Seq(author("Wohlin","Claes"), author("Runeson","Per"), author("Höst","Martin"),
        author("Ohlsson","Magnus","C"), author("Regnell","Björn"), author("Wesslén","Anders")),
    Some(RefData(Book, year = Some(2024), publisher = Some("Springer"),
      edition = Some("3rd (1st ed. 2000 'An Introduction', Kluwer; 2nd ed. 2012, Springer)"),
      doi = Some("10.1007/978-3-662-69306-3"),
      url = Some("https://link.springer.com/book/10.1007/978-3-662-69306-3"))),
    Verified,
    "Standard SE textbook on controlled experiments; grounds our permutation-test + preregistration practice. COI: BR co-author.",
  ),
  Reference(
    "Case Study Research in Software Engineering: Guidelines and Examples",
    Seq(author("Runeson","Per"), author("Höst","Martin"), author("Rainer","Austen"), author("Regnell","Björn")),
    Some(RefData(Book, year = Some(2012), publisher = Some("Wiley (Wiley-Blackwell)"), pages = Some("256"),
      note = Some("ISBN 9781118104354"),
      url = Some("https://www.wiley.com/en-us/Case+Study+Research+in+Software+Engineering%3A+Guidelines+and+Examples-p-9781118181003"))),
    Verified,
    "SE case-study methodology; relevant to the qualitative-synthesis alternative to a full SLR. COI: BR co-author.",
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
      url = Some("https://www.csee.umbc.edu/courses/331/papers/eliza.html"),
      note = Some("free copy at UMBC; origin of the 'ELIZA effect'"))),
    Verified,
    "Origin of the ELIZA effect (human over-attribution of understanding to a conversational program).",
  ),

  // ── ToDo: recalled but NOT verified this session — verify on Google Scholar before citing as fact ──
  Reference(
    "Personality Traits in Large Language Models",
    Seq(author("Serapio-García","Greg")), // author list UNVERIFIED
    Some(RefData(Preprint, year = Some(2023), venue = Some("arXiv"),
      note = Some("recollection: ~arXiv:2307.00184, Google DeepMind — VERIFY id + authors"))),
    ToDo,
    "LLM Big-Five/OCEAN personality measurement. Id + author list UNVERIFIED.",
  ),
  Reference(
    "The Media Equation: How People Treat Computers, Television, and New Media Like Real People and Places",
    Seq(author("Reeves","Byron"), author("Nass","Clifford")),
    Some(RefData(Book, year = Some(1996), publisher = Some("CSLI / Cambridge University Press"),
      note = Some("VERIFY year / publisher / author order"))),
    ToDo,
    "Foundational anthropomorphism-of-media work. Details UNVERIFIED.",
  ),
  Reference(
    "Guidelines for performing Systematic Literature Reviews in Software Engineering",
    Seq(author("Kitchenham","Barbara"), author("Charters","Stuart")),
    Some(RefData(TechReport, year = Some(2007), publisher = Some("Keele University / EBSE"),
      number = Some("EBSE-2007-01?"), note = Some("VERIFY report id / year"))),
    ToDo,
    "The SE SLR standard. BR's view: may be over-arching given scarce hard empirical evidence in SE (see lit-review note).",
  ),
  Reference(
    "Guidelines for snowballing in systematic literature studies and a replication in software engineering",
    Seq(author("Wohlin","Claes")),
    Some(RefData(Conference, year = Some(2014), venue = Some("EASE"),
      note = Some("VERIFY venue / year / doi"))),
    ToDo,
    "Snowball sampling as a lighter-weight search strategy — BR's preferred alternative to a full protocol. UNVERIFIED.",
  ),
  Reference(
    "Theory of Mind May Have Spontaneously Emerged in Large Language Models",
    Seq(author("Kosinski","Michal")),
    Some(RefData(Preprint, year = Some(2023), venue = Some("arXiv"),
      note = Some("contested; VERIFY id + note the rebuttals"))),
    ToDo,
    "Theory-of-mind-in-LLMs (contested) — useful for the unfalsifiable-from-inside point. UNVERIFIED.",
  ),
)
