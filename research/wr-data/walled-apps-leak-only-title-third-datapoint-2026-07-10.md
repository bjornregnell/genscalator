# The "walled app" pattern recurs a third time: a Google Doc leaks only its title (2026-07-10)

BR-flagged. A third independent instance of the same finding from the ChatGPT experiment (SM040): frontier models
cannot machine-read modern walled JS apps; only metadata (the title) leaks; the human is the mandatory conduit.

## The three instances (all 2026-07-10, all the same failure)
1. **ChatGPT could not read genscalator's repo** - Codeberg raw returned `UnexpectedStatusCode`; the GitHub-mirror
   raw fetch fell through to a generic explainer page. (SM040 rounds 2-3)
2. **Claude (CO4) could not read a shared ChatGPT conversation** - WebFetch got the SPA app-shell + a login wall;
   only the page TITLE ("Genscalator Novelty Analysis") was visible, the conversation body walled off. (the
   reciprocal finding)
3. **Claude could not read the community-note Google Doc** (Odersky / Regnell / Kerr, "Scala Style
   Recommendations") - WebFetch hit a login wall; only the document TITLE leaked, the body walled off. (this datum)

## The pattern
Modern collaborative apps (Codeberg's SPA, `chatgpt.com/share`, Google Docs) are JavaScript single-page apps
behind login / SPA shells. A non-JS anonymous fetcher - i.e. ANY model's browsing tool - reliably gets the outer
shell plus a metadata crumb (the `<title>`), NOT the content. So machine-to-machine reading across walled apps
fails BY DEFAULT, in every direction tried, regardless of host or vendor.

## Implications
- **Human-mediated context injection is not just the safe/valid path (the ZIP-hardening finding) but often the
  ONLY path.** The human carries the substrate across by hand (paste / upload). This is exactly why the resident
  agent (CO4) is capable where an external model is not: the human wired the substrate INTO CO4's context, and
  there is no reliable automated substitute.
- **The partial-metadata leak is consistent: the TITLE leaked but never the body.** A title is enough for a model
  to CONFABULATE about the content (the nearest-category-completion mechanism, round 1), so a leaked title without
  the body is a *confabulation seed*, not grounding. This reinforces
  [[validator-findings-disclosure-and-blind-arm-2026-07-10]]: a model handed only a title should DISCLOSE it saw
  only the title, not answer from it.
- **Practical corollary (live this session):** you cannot verify a "public read-only" share setting via a model's
  fetch - a genuinely public Google Doc and a restricted one can BOTH serve a login-ish shell to a crawler. The
  reliable test is a HUMAN incognito-browser check. Do not trust a model's fetch to confirm a doc is publicly
  readable.

## Fourth beat (2026-07-10): a human echt-test RESOLVED the ambiguity and confirmed the point
BR's wife - logged out, a genuine "kreti och pleti" reader - opened the SAME Google Doc in a real browser and
**could read it**. Two things follow:
1. **The doc was PUBLIC all along.** So the "login wall" my WebFetch hit was NOT a restriction; it was the
   non-JS-crawler-vs-JS-SPA degradation I had flagged as the echt caveat. My fetch-based lean ("probably still
   Restricted") was WRONG, and the human test corrected it in one move. A clean self-exemplifying case: **a
   model's fetch cannot judge a doc's public-readability, and here it produced a FALSE NEGATIVE** that a human
   browser immediately disproved. The earlier caveat ("do not trust a model's fetch") was right, and this is its
   proof.
2. **Human-in-a-real-browser succeeds where the model fetch fails - for a SECOND reason.** The gap is not only
   login / permission; even a fully-public doc is unreadable to the model because its fetcher does not run the
   JavaScript app a human browser does. So the human is the conduit twice over: past the permission wall AND past
   the JS-rendering wall. Beats 1-3 (only the title crosses to the machine) plus this (a human reads the full
   body) is the whole finding in one arc: **metadata crosses to the machine; content crosses only via the human.**

Ties: the substrate-access thesis, the sovereignty/control reframe (unreachability = control - "the friction is
the sovereignty"), the Reciprocal section of
`../other-model-validation-echt/2026-07-10-claude-on-chatgpt-on-genscalator-novelty.md`, and blog 014.
