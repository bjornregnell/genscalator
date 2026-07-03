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

### Permission-model observations — the guard calibrating *well* (2026-07-03, BR)
Two positive data points (not friction — evidence the confirmation model is well-placed, the flip side of the CF hazard):
- **Version/capability probes (`gh --version`, `tea --version`) should be safe** — read-only, no side effects, just capability detection. Candidate: **allowlist the *specific* binaries we probe** (`gh`, `tea`, `scala-cli`, `scalex`, `tt`) for `--version`/`--help`. CAVEAT: a blanket `Bash(* --version)` is **not** strictly safe — it still *executes an arbitrary binary*, and a malicious `foo` can ignore `--version` and do anything. So narrow per-binary probe entries, not a universal wildcard. (Safe-by-design principle: the *rule* must be provably safe, not just the intended call — same lesson as the curl entry below.)
- **Reading OUTSIDE the allowed working dirs correctly prompted** (`/etc/os-release`), and BR judged the prompt **appropriate** — a meaningful boundary crossing, not rubber-stamp noise. Positive signal that the **directory-scope guard is well-calibrated**: it fires on genuine out-of-tree access (where the human *wants* a say) without drowning in-tree work in prompts. This is the *good* end of the CF spectrum — few, meaningful approvals — that safe-by-design aims for.

### curl / HTTP GET → `tt web` + `tt forge` (2026-07-03)
A different WR flavour: not a *bundling* prompt but a **dual-use-binary allowlisting** hazard. Fetching a
public API with `curl` is fine *as a call*, but the safe-by-design question is about the **allowlist rule**,
not the call: `Bash(curl *)` would approve exfiltration (`-d @secret`), `curl | sh` RCE, and SSRF to
internal hosts. The genscalator answer is the usual one — **replace the un-allowlistable dual-use tool with
a narrow typed tool that declares its effects**: `tt web get` (read-only, capped, host-allowlistable) for the
generic case, and `tt forge` (Gitea/Codeberg releases/tags) for the domain need that surfaced here (creating
the missing v0.8.0 release without hand-curling a token). Same pattern as `tt verify` replacing the
`cd && … > log; echo $?` bundle: a bare binary the guard can't prove safe → a typed command it can.
