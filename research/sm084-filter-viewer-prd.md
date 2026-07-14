# filter-viewer — reqT-lang PRD (DRAFT for BR review)

**Status:** agent-authored DRAFT, 2026-07-14 (SM084, STEP 1 of 2). This is the reviewable PRD; the BUILD (a
single-page, server-less ScalaJS + Laminar app) is gated on BR's go. Model: Opus 4.8 (1M). Author from
goal-level down; verified with `tt parsereqt`. OPENs are carried as `Comment` bullets in the model and collected
in "Open questions for BR" at the foot.

## Requirements model (reqT-lang)

* Stakeholder: user has
  * Goal: findInContext
  * Goal: reduceNoise
  * Goal: readable
  * Goal: liveFilter
* Stakeholder: br has
  * Goal: serverless
* Stakeholder: overload has
  * Goal: hideRelevance

* Goal: findInContext has
  * Spec: The user finds each filter hit and reads it WITH its surrounding lines, not as an isolated match.
  * Why: a match is only meaningful in its context.
* Goal: reduceNoise has
  * Spec: Only the neighbourhoods of hits are shown; unrelated lines are hidden.
  * Why: the point is a filtered VIEW, not the whole file.
* Goal: readable has
  * Spec: Shown text is syntax-coloured and carries original line numbers.
  * Why: legibility of code and structured text.
* Goal: liveFilter has
  * Spec: The view updates live as the filter-expression is typed.
  * Why: exploration is interactive.
* Goal: serverless has
  * Spec: The app is a single static page with no server and no run-time network calls.
  * Why: portability and data sovereignty; the file never leaves the user's machine.
* Goal: hideRelevance has
  * Spec: Anti-goal: the user drowns in irrelevant lines and cannot locate the relevant regions.
  * Why: the noise the app exists to defeat.

* Target: filterLatencyOk verifies Goal: liveFilter
* Target: filterLatencyOk has
  * Spec: The view re-renders within a responsive budget as the filter is typed, for a mid-size file.
  * Max: 100
  * Comment: OPEN - Max = target re-render milliseconds; the threshold and the mid-size file-line count TBD with BR.
* Target: noNetworkAtRuntime verifies Goal: serverless
* Target: noNetworkAtRuntime has
  * Spec: Zero network requests after the page has loaded; the app is self-contained HTML plus JS.

* Component: filterViewerApp has
  * Spec: A single-page, server-less web app built with ScalaJS and Laminar, shipping as self-contained HTML+JS.
  * Constraints: no server; no run-time network; runs from a local file or a static host.

* Feature: fileLoad helps Goal: findInContext
* Feature: fileLoad has
  * Spec: Load the text file to view.
  * Comment: OPEN - source: paste into a textarea, open a local file via the file picker, or fetch a URL - which, or several?
* Feature: filterBox helps Goal: liveFilter
* Feature: filterBox has
  * Spec: A text input at the top holding the filter-expression.
  * Comment: OPEN - filter semantics: plain substring, regex, or both with a toggle? case-sensitive or a case toggle?
* Feature: windowedView helps Goal: findInContext
* Feature: windowedView helps Goal: reduceNoise
* Feature: windowedView hurts Goal: hideRelevance
* Feature: windowedView has
  * Spec: For each line n where the filter matches, show the lines in the window from n minus w to n plus w; hide all others.
* Feature: windowControl helps Goal: findInContext
* Feature: windowControl helps Goal: reduceNoise
* Feature: windowControl has
  * Spec: The user sets the context width w.
  * Comment: OPEN - default w; minimum 0; symmetric window, or separate before/after widths?
* Feature: mergeWindows helps Goal: reduceNoise
* Feature: mergeWindows helps Goal: readable
* Feature: mergeWindows has
  * Spec: Overlapping or adjacent windows merge into one contiguous region, so no line is shown twice.
  * Why: when hits are close their windows overlap; merging avoids duplicate lines.
* Feature: gapMarkers helps Goal: readable
* Feature: gapMarkers has
  * Spec: Between shown regions, indicate the elided lines - e.g. a divider labelled with the skipped line range.
* Feature: lineNumbers helps Goal: readable
* Feature: lineNumbers has
  * Spec: Each shown line carries its original file line number n.
* Feature: hitHighlight helps Goal: findInContext
* Feature: hitHighlight has
  * Spec: Within the shown lines, highlight the matched substrings.
* Feature: syntaxColour helps Goal: readable
* Feature: syntaxColour has
  * Spec: Syntax-colour the shown text.
  * Comment: OPEN - which languages, and how, under serverless plus ScalaJS: bundle a JS highlighter, or a Scala tokeniser? auto-detect by extension?

* Feature: windowedView requires Feature: filterBox
* Feature: windowedView requires Feature: fileLoad
* Feature: mergeWindows requires Feature: windowedView
* Feature: gapMarkers requires Feature: windowedView
* Feature: hitHighlight requires Feature: windowedView
* Feature: lineNumbers requires Feature: windowedView
* Feature: filterViewerApp requires
  * Feature: fileLoad
  * Feature: filterBox
  * Feature: windowedView

## Open questions for BR (the decisions the PRD needs before build)

1. **Filter semantics** — plain substring, regex, or both (with a toggle)? Case-sensitive, or a case toggle?
2. **Window width w** — default value; symmetric (±w), or separate before/after? User-adjustable live (assumed yes via `windowControl`)?
3. **Overlap** — merge overlapping/adjacent windows into one region? (Assumed **yes** via `mergeWindows`.)
4. **Between regions** — show a gap marker with the elided line range? (Assumed **yes** via `gapMarkers`.)
5. **Syntax colouring** — which languages, and how under serverless + ScalaJS (bundle a JS highlighter vs a Scala tokeniser; auto-detect by extension)?
6. **File source** — paste into a textarea, open a local file, and/or fetch a URL?
7. **Scale** — expected max file size (drives the latency target + whether virtualised rendering is needed)?
8. **Match navigation** — beyond filtering, do you want next/prev-hit jumps or a hit count? (Not yet modelled.)
