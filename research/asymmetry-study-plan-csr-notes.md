# CSR grounding notes for the asymmetry-study plan

> Companion reference for [`asymmetry-study-plan.md`](asymmetry-study-plan.md). Reading notes from the
> case-study-research methodology book (Runeson, Höst, Rainer & Regnell, *Case Study Research in Software
> Engineering: Guidelines and Examples*, Wiley 2012 - BR is a co-author). All content here is **paraphrased**
> from the book with section/page cites; no text is reproduced verbatim (the source is a copyrighted book held
> in the closed synch repo; this repo is public). Purpose: verify the plan's scheme against the book and mark
> what to fold in after BR reviews the plan. **This is reference only - no coding or mining happens here.**

## 1. The coding scheme is validated by Appendix D (pp. 213-217)

Appendix D is a worked coding guide from the REVV large-scale study (Ch. 14), used to align **11** coders and
reduce the reliability threat of inconsistent coding. It uses **three code levels** - which is exactly the plan's
§3 structure:

- **High level = research questions** (Table D.1). Each high-level code is a research-question theme, and the
  high-level codes are **prioritized** (assign the highest-priority applicable one; note the others in Comments).
  → Our plan's A/B/C/D primary categories are the RQ1/RQ2 seed. **Consider adding an explicit priority order**
  the way D.1 does (e.g. if a specimen is both a Difference and a Positive-effect, which code wins, with the
  other logged in a rationale/notes field).
- **Medium level = categories** (Tables D.2-D.4). Coded in **two columns, "Primary code" and "Secondary code"**;
  a 13th **"Other"** category absorbs anything not in the fixed list, described in Comments. → Matches the plan's
  subcodes + the append-only **emergent-codes** list. The Primary/Secondary two-column idea is worth adopting:
  it lets a specimen carry a best-fit and a runner-up code without forcing a single choice.
- **Low level = the coder's interpretation / one-line statement summary** in a Comments field. → Matches the
  plan's "one-line rationale" per specimen.

**Takeaway:** the plan's coding structure is faithful to the book. The two concrete borrowings to consider:
(a) prioritized high-level codes with a tie-break rule; (b) Primary/Secondary columns at the category level.

## 2. The four validity types (§5.4, pp. 71-72) - paraphrased definitions

The book uses Yin's four aspects (it notes Robson's alternative names - credibility / transferability /
dependability / confirmability - but prefers the four). Precise-enough glosses for our internal checklist:

- **Construct validity** - do the things we measure/code actually stand for what we mean by the research
  questions? Threat: the coder and the source read a "construct" differently. *For us:* are the categories
  A/B/C/D and their subcodes defined tightly enough that "difference", "similarity", "introspection-payoff"
  mean the same thing on every specimen? Mitigation already in plan: frozen pre-hoc scheme with per-code
  definitions, reviewed by BR before mining.
- **Internal validity** - only about **causal** claims: when you say factor X affected factor Y, is there a
  hidden third factor Z also moving Y that you missed? *For us:* the RQ2 claim "introspection improved the
  work" is causal, so this is where we are most exposed - a workflow improvement may have many causes besides
  introspection. Mitigation: treat RQ2 as **associational + exemplar-based, not proof**, and actively hunt
  category-D counter-examples.
- **External validity** - how far do the findings transfer? Case studies give **analytical / theoretical
  generalization** (extend to cases sharing characteristics, i.e. defining a theory), never statistical
  generalization from a sample. A good **context description** lets the reader judge relevance to their own
  situation. *For us:* one human + one agent + one project → claim analytical transfer only, stated plainly,
  and describe the context (who, what tooling, what period) so a reader can judge.
- **Reliability** - would another researcher, repeating it, get the same result? Threats: unclear how to code
  the data; unclear instruments. *For us:* **the big one** - the agent is coder *and* participant. Mitigation:
  the human-audit inter-coder check (N=10), the transparent append-only coding log, and a chain of evidence
  from each claim back to its specimen.

This confirms the plan's §7 table. No change needed to the four rows; the glosses above just sharpen them.

## 3. The book's "improving validity" techniques already live in the plan (§5.5, pp. 72-74)

The book lists six general approaches and explicitly says there is **no clean approach→threat mapping** - each
must be judged per study. What is striking: five of the six are already instantiated in our plan, and the sixth
is the one to watch.

| Book technique (§5.5) | Where it already is in our plan | Note |
|---|---|---|
| **Triangulation** | §2 corpora table: wr-data + RAW-DATA + PB + git + memory, cross-checked | the book: multiple sources surface contradictions; multiple coders lowers single-person bias |
| **Member checking** | §6: BR member-checks the final findings + the N=10 blind re-code | the book: send material back to the participant to confirm it was not misunderstood |
| **Peer debriefing** | §6 human-audit; the joint BR+agent review of the account | the book: a second reader on the design/analysis lowers single-researcher bias |
| **Negative case analysis** | §3 **category D** (negative/null, weighted equally) | the book calls this "a normal, valuable step" - our confirmation-bias guard is textbook |
| **Audit trail** | append-only coding log + append-only RAW-DATA + git history | the book literally says **"a version control system can be helpful"** - we are already there |
| **Prolonged involvement** | the whole action-research setup (embedded, long-running) | **CAVEAT below** |

### The one caveat the book flags directly

On **prolonged involvement** (pp. 72-73) the book notes the double edge: long embedding gives trust and access
and shared vocabulary, **but risks the researcher losing independence and getting biased by being too involved.**
That is *precisely* our reflexivity / agent-double-role concern, named by the methodology itself. It is the
strongest textbook support for why the plan keeps the human-audit and category D as hard guards - and worth a
one-line nod in blog 021's "Can we trust this?" ("even the method warns that being this embedded can bias you;
here is what we did about it").

Example 5.6 (p. 73) shows the book's own XP study handling exactly this: checklist-based threat analysis,
triangulation, results reviewed by case representatives, and **two researchers coding the same material in
parallel** to surface negative cases. Our N=10 blind human-audit is the small-scale version of that parallel-
coding move.

## 4. Analysis-process points worth carrying (§5.2-5.3, pp. 63-67)

- **Editing vs template approaches** (p. 64): the book says these two (few a-priori codes refined during
  analysis / more a-priori codes from RQs) are what actually work in SE case studies - immersion is too
  unstructured, quasi-statistical too formal. Our **abductive** stance (frozen a-priori seed + emergent codes
  via constant comparison) sits squarely in the editing/template band. Good.
- **Hypothesis generation vs confirmation** (p. 65): generation techniques (constant comparison, cross-case)
  for exploratory work; confirmation techniques (triangulation, replication, **negative case analysis**) for
  explanatory work. RQ1 is generation-flavoured; RQ2 is confirmation-flavoured - which is why category D
  (negative cases) matters most for RQ2.
- **Chain of evidence** (p. 66): document each step so a reader can trace conclusion → coded specimen, "without
  which there is no way for the reader to understand that the conclusions are trustworthy." → our source
  anchors (file + line/section) per code are exactly this.
- **Iterative, not linear** (Fig 5.1, p. 63; p. 66): collection → coding → hypothesis → generalization →
  reporting is a loop; reformulating a code means going back and **re-coding** prior material. → our versioned
  scheme (v0 → v1 …) with dated, justified bumps is the disciplined form of this.
- **Developing a case description** (Yin, p. 66): organize findings by defining sections first, then reading the
  whole corpus for what is relevant to each. → a viable structure for the blog-021 results section.

## 5. What to fold in after BR reviews the plan (nothing done yet)

1. Add a **priority / tie-break rule** for the high-level categories (A/B/C/D), per Appendix D.1's prioritized
   high-level codes.
2. Consider **Primary/Secondary** category columns (App D.2) so a specimen can carry a best-fit + runner-up.
3. In blog 021's "Can we trust this?", add the **one honest line** that the method itself warns prolonged
   involvement can bias the embedded researcher - and point at category D + the human-audit as the answer.
4. Keep the four validity rows as-is (the §3 glosses above are just sharper wordings, not new threats).

*All of §5 is a suggestion list for BR; none of it changes the plan until he says so.*
