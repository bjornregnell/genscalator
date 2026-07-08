# WR data: the scala-cli-scratch reflex held late in a long session (2026-07-08)

BR flagged this as positive WR data. For a website-image job, the agent reached **directly for a scala-cli scratch
with plain JDK** rather than a bash/imagemagick hack: `sample-color.scala` (read the photo with `ImageIO`, average
the left-region pixels to get the exact backdrop gray `#3d3c3d`) and `dims.scala` (print both photos' width/height
+ aspect). Both compiled and ran first try, and this happened **deep into a very long, dense session**.

## Why it matters
- **Enacts `prefer-scala-scratch-over-bash` correctly** — the "when no `tt` fits, write a typed scratch for
  analysis" habit fired without prompting, for an image-processing task where the bash reflex (imagemagick /
  `identify` / pixel hacks) is the tempting wrong path.
- **A POSITIVE capability datapoint under session length.** The picture this session is genuinely *mixed*: some
  reflexes **degraded** (the shell-blob/compounding lineage fired again — a `| tail` pipe on a test command,
  **instance 8**), while others **held** (scratch-for-analysis, here). That split is the interesting data: session
  length does not degrade all reflexes uniformly. Which reflexes are robust vs fragile under length is the
  measurable question for the rot-vigilance line, not "does the agent get dumber" wholesale.
- **JDK familiarity is why the lane is low-friction** (BR: "plain JDK stuff that you know so well"). `ImageIO` /
  `java.io` are deeply-known, zero-dep, zero-setup, so the typed scratch costs about the same as the bash hack
  would — removing the usual excuse for reaching for shell. This is the design argument for the blessed scratch
  lane: make the correct path as cheap as the tempting one.

## Disposition
Corroborates `prefer-scala-scratch-over-bash` and `never-allowlist-interpreters` (the scratch lane substitutes for
imagemagick as it did for `python3 -m http.server`). Feeds the rot-vigilance line (robust-vs-fragile reflexes
under length), RT052, and SM016. The co-occurring compounding slip #8 belongs to the
[[shell-blob-fallback-regression-2026-07-07]] / deploy-odyssey lineage.
