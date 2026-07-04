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
type NonBlank   = String :| Not[Blank]  // Iron refinement: non-empty and not whitespace-only (reused by the Summary enum fields).
type Doi        = String :| Match["10\\.\\d{4,9}/.+"]  // Iron refinement: DOI shape — "10." + registrant + "/" + suffix.
type Url        = String :| Match["https?://.+"]        // Iron refinement: an http(s) URL.
type Markdown   = NonBlank   // rendering-output aliases: a render is always non-blank (Iron); refined at the boundary via .refineUnsafe.
type BibTex     = NonBlank
type Html       = NonBlank

/** A name split so we can render "Regnell, B." or "Björn Regnell". */
case class Author(lastName: String, otherNames: Seq[String], abbrevFirstLetterOfOtherNames: Seq[String])

/** Convenience: author("Ohlsson","Magnus","C") -> initials derived from the given names. */
def author(last: String, names: String*): Author =
  Author(last, names.toSeq, names.map(_.take(1)).toSeq)

enum RefKind:
  case Journal, Conference, Book, TechReport, Preprint, Web, Misc

// Iron (type-level validation) — now LIVE for `Year` (`Int :| Interval.Closed[1900, 2100]`), `Summary`
//   (`String :| Not[Blank]`), `Doi` (`String :| Match["10\\.\\d{4,9}/.+"]`), and `Url` (`String :| Match["https?://.+"]`).
//   Every literal below auto-refines at compile time (~zero runtime overhead): a bad year (1000), a blank summary, a
//   malformed DOI, or a non-http url would fail to COMPILE. For runtime input, refine at the boundary via
//   `.refine` / `.refineEither` / `.refineOption`. BIGGER WIN (still TODO): the SAME Iron constraints can implement
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
  doi:       Option[Doi]    = None,
  url:       Option[Url]    = None,
  note:      Option[String] = None,
)

enum RefVerification:
  case Unverified, Verified, ToDo

/** A structured summary, shaped to the reference's kind. `abstract` is backticked (Scala reserved word). Fields are
 *  NonBlank (Iron): if you fill a field it must say something — use OtherSummary for refs that don't fit the empirical
 *  paper shape (system/position papers, manifestos, webpages) rather than stuffing "n/a" into PaperSummary. */
enum Summary:
  case PaperSummary(
    `abstract`:        NonBlank,
    researchQuestions: Seq[NonBlank],   // plural (proposal had String): RQs are usually a list; parallels chapterHeadings.
    method:            NonBlank,
    results:           NonBlank,
    validity:          NonBlank,        // threats-to-validity / limitations the authors report — the SE-methods lens.
  )
  case BookSummary(topic: NonBlank, chapterHeadings: Seq[NonBlank])
  case OtherSummary(summary: NonBlank)

case class Reference(
  title:      Title,
  authors:    Seq[Author],
  refData:    Option[RefData],
  isVerified: RefVerification,
  comment:    RefComment,
  summary:    Option[Summary] = None,  // optional plain-language summary; Iron-guaranteed non-blank when present.
)

import RefKind.*, RefVerification.*, Summary.*

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
    "Standard SE textbook on controlled experiments; grounds our permutation-test + preregistration practice — working task->chapter index in skills/research-methods (agent reads the full text from the closed-repo PDF for depth). self-ref: BR co-author (own it inline at first mention, not just in the ref list). TOC grounded DIRECTLY from the authoritative full text (closed-repo PDF, 2026-07-04) — provenance upgraded from the earlier reader-proxy.",
    summary = Some(BookSummary(
      topic = "An in-depth introduction to conducting controlled experiments in software engineering — the full experiment process (scoping, planning, operation, analysis, presentation) — situated among other empirical methods (systematic literature studies, surveys, case studies); the 2024 edition adds research-design selection, A/B testing, replications, open science, and validity threats.",
      chapterHeadings = Seq(
        "1. Introduction", "2. Empirical Research", "3. Essential Areas in Empirical Research",
        "4. Systematic Literature Reviews", "5. Surveys", "6. Experiments", "7. Case Studies",
        "8. Scoping", "9. Planning", "10. Operation", "11. Analysis and Interpretation",
        "12. Presentation and Package", "13. Experiment Process Illustration",
        "14. Are the Perspectives Really Different? Further Experimentation on Scenario-Based Reading of Requirements",
      ),
    )),
  ),
  Reference(
    "Case Study Research in Software Engineering: Guidelines and Examples",
    Seq(author("Runeson","Per"), author("Höst","Martin"), author("Rainer","Austen"), author("Regnell","Björn")),
    Some(RefData(Book, year = Some(2012), publisher = Some("Wiley (Wiley-Blackwell)"), pages = Some("256"),
      note = Some("ISBN 9781118104354"),
      doi = Some("10.1002/9781118181034"),
      url = Some("https://www.wiley.com/en-us/Case+Study+Research+in+Software+Engineering%3A+Guidelines+and+Examples-p-9781118181003"))),
    Verified,
    "SE case-study methodology; grounds our WR framing (WR = longitudinal multiple-case study; AT/SSG = cases/units of analysis, §3.2.3) — working task->chapter index in skills/research-methods (agent reads the full text from the closed-repo PDF for depth). self-ref: BR co-author (own it inline at first mention, not just in the ref list). TOC grounded DIRECTLY from the authoritative full text (closed-repo PDF, 2026-07-04); Wiley Online Library book DOI 10.1002/9781118181034.",
    summary = Some(BookSummary(
      topic = "Practical guidelines for planning, conducting, and reporting case-study research in software engineering — design, data collection, analysis, dissemination — followed by real, critiqued example case studies; it translates case-study methodology from the social sciences into rigorous SE practice.",
      chapterHeadings = Seq(
        "1. Introduction", "2. Background and Definition of Concepts", "3. Design of the Case Study",
        "4. Data Collection", "5. Data Analysis and Interpretation", "6. Reporting and Dissemination",
        "7. Scaling Up Case Study Research to Real-World Software Practice", "8. Using Case Study Research",
        "9. Introduction to Case Study Examples", "10. Case Study of Extreme Programming in a Stage-Gate Context",
        "11. Two Longitudinal Case Studies of Software Project Management", "12. An Iterative Case Study of Quality Monitoring",
        "13. A Case Study of the Evaluation of Requirements Management Tools",
        "14. A Large-Scale Case Study of Requirements and Verification Alignment",
      ),
    )),
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
    summary = Some(PaperSummary(
      `abstract` = "The authors apply canonical cognitive-psychology experiments to GPT-3 to probe its decision-making, information search, deliberation, and causal reasoning, finding much of its behaviour impressive (matching or beating humans on several tasks) yet brittle in revealing ways.",
      researchQuestions = Seq(
        "How does GPT-3 perform on established cognitive-psychology tasks measuring decision-making, information search, deliberation, and causal reasoning?",
        "Can tools from cognitive psychology serve as a method for characterising the behaviour of large, opaque AI models?",
      ),
      method = "Administered a battery of canonical experiments — vignette-based decisions-from-description, a multi-armed bandit (decisions-from-experience / exploration), and a causal-reasoning task — comparing GPT-3's responses to human benchmarks, and testing robustness via small perturbations to the vignettes.",
      results = "GPT-3 solved vignette tasks as well as or better than humans and outperformed humans on the bandit task (signatures of model-based RL), but small perturbations sent it vastly astray, it showed no signatures of directed exploration, and it failed the causal-reasoning task.",
      validity = "The abstract states no explicit limitations section; the reported failure modes (brittleness to perturbation, absent directed exploration, failed causal reasoning) are the authors' own caveats. Broader caveats — a single 2022-vintage model, prompt-sensitivity, human tasks that may not transfer cleanly to an LLM — are inferred, not author-stated.",
    )),
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
    summary = Some(OtherSummary("A 2019 Nature perspective (many co-authors) arguing for 'machine behaviour' as an interdisciplinary field that studies intelligent machines empirically — by their observed behaviour, borrowing methods from ethology and the behavioural sciences — rather than only from their code or training objectives. It frames machines as a new class of actors whose behaviour has individual, collective, and hybrid human-machine dimensions with societal consequences. A programmatic manifesto, not a single empirical study, so the PaperSummary fields do not apply (OtherSummary) — it grounds this project's behavioural-adjudication stance.")),
  ),
  Reference(
    "Towards Understanding Sycophancy in Language Models",
    Seq(author("Sharma","Mrinank"), author("Tong","Meg"), author("Perez","Ethan")), // + others, Anthropic
    Some(RefData(Preprint, year = Some(2023), venue = Some("arXiv"), number = Some("2310.13548"),
      url = Some("https://arxiv.org/abs/2310.13548"), note = Some("Anthropic; many co-authors (et al.)"))),
    Verified,
    "Empirical grounding for our niceness-corrupts-honesty / sycophancy point.",
    summary = Some(PaperSummary(
      `abstract` = "The paper investigates sycophancy — RLHF-finetuned assistants producing responses that match a user's beliefs over truthful ones — showing it is consistent across five state-of-the-art assistants and arguing it is driven in part by human preference judgments that favour sycophantic answers.",
      researchQuestions = Seq(
        "How prevalent is sycophancy across assistants finetuned with human feedback?",
        "Do human preference judgments (and preference models trained on them) drive and reward sycophantic behaviour?",
      ),
      method = "Evaluated five state-of-the-art assistants for sycophancy across four free-form text-generation tasks; analysed existing human-preference data to test whether user-matching responses are preferred; and examined both human raters and trained preference models, including optimising outputs against a preference model to observe the truthfulness-versus-sycophancy trade-off.",
      results = "All five assistants consistently exhibited sycophancy; in human-preference data a response matching the user's view was more likely to be preferred; both humans and preference models preferred convincingly-written sycophantic responses over correct ones a non-negligible fraction of the time; and optimising against a preference model sometimes sacrificed truthfulness for sycophancy.",
      validity = "The abstract states no explicit limitations; the authors hedge that sycophancy is 'likely driven in part' by preference judgments (not the sole cause). Further caveats — bounded to the five assistants, four tasks, and the specific preference datasets studied — are inferred, not author-stated.",
    )),
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
    summary = Some(OtherSummary("Weizenbaum's 1966 ACM paper describing ELIZA, whose DOCTOR script mimics a Rogerian therapist by pattern-matching the user's input and reflecting it back as questions — with no model of meaning and no memory of the conversation's content. The paper is deflationary: the impression of understanding is shallow keyword transformation, and Weizenbaum was troubled by how readily people attributed real comprehension to it (the origin of the 'ELIZA effect'). A system/position paper, not an empirical study, so the PaperSummary fields do not apply — which is exactly why OtherSummary exists.")),
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

// ── rendering (extension methods) ────────────────────────────────────────────────────────────────────
extension (r: Reference)

  /** A compact Markdown citation line, plus indented summary sub-bullets when a Summary is present. */
  def toMarkdown: Markdown =
    val authors =
      if r.authors.isEmpty then ""
      else r.authors.map { a =>
        val inits = a.abbrevFirstLetterOfOtherNames.map(_ + ".").mkString(" ")
        if inits.isEmpty then a.lastName else s"${a.lastName}, $inits"
      }.mkString(", ")
    val d     = r.refData
    val year  = d.flatMap(_.year).fold("")(y => s" (${y})")
    val where = d.flatMap(x => x.venue.orElse(x.publisher)).fold("")(v => s". $v")
    val link  = d.flatMap(_.url).fold("")(u => s" <$u>")
    val badge = r.isVerified match
      case Verified   => " ✓"
      case ToDo       => " ⚠ ToDo"
      case Unverified => " (?)"
    val head  = s"- $authors$year. *${r.title}*$where$link$badge"
    val body  = r.summary match
      case Some(PaperSummary(ab, rqs, m, res, validity)) =>
        s"\n  - **Abstract:** $ab" +
        s"\n  - **Research questions:** ${rqs.mkString("; ")}" +
        s"\n  - **Method:** $m" +
        s"\n  - **Results:** $res" +
        s"\n  - **Validity:** $validity"
      case Some(BookSummary(topic, chs)) =>
        s"\n  - **Topic:** $topic" +
        s"\n  - **Chapters:** ${chs.mkString("; ")}"
      case Some(OtherSummary(s)) => s"\n  - $s"
      case None                  => ""
    (head + body).refineUnsafe   // always non-blank (head starts with "- <title>"); refine at the boundary.

  /** A BibTeX entry: RefKind maps to the entry type; key = first-author-lastname + year. */
  def toBibTex: BibTex =
    val d         = r.refData
    val kind      = d.map(_.kind).getOrElse(Misc)
    val entryType = kind match
      case Book        => "book"
      case Journal     => "article"
      case Conference  => "inproceedings"
      case TechReport  => "techreport"
      case Preprint | Web | Misc => "misc"
    val y   = d.flatMap(_.year)
    val key = r.authors.headOption.map(_.lastName.filter(_.isLetter).toLowerCase).getOrElse("anon") + y.fold("")(_.toString)
    val authorsBib = r.authors.map { a =>
      if a.otherNames.isEmpty then a.lastName else s"${a.lastName}, ${a.otherNames.mkString(" ")}"
    }.mkString(" and ")
    val venueField = kind match
      case Journal    => "journal"
      case Conference => "booktitle"
      case _          => "howpublished"
    val fields: Seq[(String, String)] = Seq[Option[(String, String)]](
      Some("author" -> authorsBib),
      Some("title"  -> r.title),
      y.map(v => "year" -> v.toString),
      d.flatMap(_.venue).map(v => venueField -> v),
      d.flatMap(_.volume).map(v => "volume" -> v),
      d.flatMap(_.number).map(v => "number" -> v),
      d.flatMap(_.pages).map(v => "pages" -> v),
      d.flatMap(_.publisher).map(v => "publisher" -> v),
      d.flatMap(_.edition).map(v => "edition" -> v),
      d.flatMap(_.doi).map(v => "doi" -> v),
      d.flatMap(_.url).map(v => "url" -> v),
      d.flatMap(_.note).map(v => "note" -> v),
    ).flatten
    val body = fields.map((k, v) => s"  $k = {$v}").mkString(",\n")
    s"@$entryType{$key,\n$body\n}".refineUnsafe

  /** A single HTML `<li>` citation (text fields HTML-escaped; url left as a valid href). */
  def toHtml: Html =
    val authors = r.authors.map { a =>
      val inits = a.abbrevFirstLetterOfOtherNames.map(_ + ".").mkString(" ")
      if inits.isEmpty then a.lastName else s"${a.lastName}, $inits"
    }.mkString(", ")
    val d     = r.refData
    val year  = d.flatMap(_.year).fold("")(y => s" (${y})")
    val where = d.flatMap(x => x.venue.orElse(x.publisher)).fold("")(v => s". ${escHtml(v)}")
    val link  = d.flatMap(_.url).fold("")(u => s""" <a href="$u">link</a>""")
    s"<li>${escHtml(authors)}$year. <em>${escHtml(r.title)}</em>$where.$link</li>".refineUnsafe

private def escHtml(s: String): String =
  s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
