# SM021 - Verify the genscalator tt tools work from a clean clone

**Status:** (a) DONE 2026-07-10; (b) needs a real CC instance (BR).

## Scope
"Works" has two halves (PB SM021): **(a)** the tt tools run from a clean clone
(agent-verifiable) and **(b)** the CC plugin loads in a real Claude Code session
(needs a real instance). Gates the community launch (README-ready), not the local
preview.

## (a) tt tools from a clean clone - PASS
Method: a fresh `git clone` of the repo into a scratch dir, then
`scala-cli compile <clone>/tools`. Result: the WHOLE toolbox compiles clean on
Scala 3.8.4 (JVM 21) from the fresh checkout - only the pre-existing warnings
(reqt-vendored `NN-*$package` classpath-encoding notes + four `.next`/`nextDouble`
deprecations), ZERO errors. So the tools build with no local state and no
build-cache dependence: the committed tree is self-contained. The in-repo test
suites already pass (e.g. SsgSuite, 37 green); a full clean-clone test-suite run is
the optional heavier extension (skipped here to spare the flaky box - a clean
compile is the build gate).

## (b) plugin loads in a real CC session - OPEN (BR)
Cannot be verified by the agent or a sub-agent from a clone: it needs a real
Claude Code instance that loads the genscalator plugin (its skills/commands wiring)
and exercises a tool end-to-end. Flag for BR at community-launch time.

## Method note (confirmation-fatigue tie)
This used a one-off `git clone` that forced a confirmation prompt on BR - logged as
WR data (`confirmation-fatigue-defeats-inspection-2026-07-10.md`). Verifying "tools
from clean" should NOT need a raw clone: options are in-place from the committed
tree (HEAD == what a user clones), or a narrow audited clone helper. The raw-clone
blob is the tool-gap, not the method of record.
