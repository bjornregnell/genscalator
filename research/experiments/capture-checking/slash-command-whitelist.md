# SM016 design: safe-to-self-inject slash-command whitelist (the CC-clamp enum)

**Status:** design-ahead-of-build (BR 2026-07-08, "we perhaps later build"). Defines
the `enum SlashCommand` membership rule for the capture-checking injector (poc3/poc5)
BEFORE the super-harness is built, to shape the enum + pin the safety surface.
Command set grounded in the authoritative Claude Code docs (`code.claude.com/docs/en/commands.md`,
fetched 2026-07-08), NOT from memory. Sibling of `README.md` (the CC PoCs).

## The rule

In the SM016 clamp the injector exposes ONLY a whitelisted `enum SlashCommand`.
Enum membership IS the agent-injectable set; the capture-checking clamp makes anything
outside it impossible to send (poc3: a whitelist-only `Injector`, no raw-string method).
A command earns enum membership iff it clears ALL SIX:

1. **Bounded blast radius** — affects only the agent's own session/context, or is pure read-only. No persisted-config / credential / permission / outside-world mutation.
2. **No security surface** — never touches permissions / allowlists / hooks / settings / MCP / plugins (the exact things the clamp exists to protect).
3. **No external / publish action** — nothing sent off-box (PRs, feedback, cloud sessions, installs).
4. **No auth / identity change** — never login/logout/credentials/cloud-provider setup.
5. **No unbounded cost** — no self-triggered token/compute blowup (loops, batch, cloud review, deep-research).
6. **Recoverable** — doesn't clear/rewind state the human can't get back.

## 🟢 GREEN — in the enum (agent may self-inject)

Read-only introspection + the one self-management step the dances need. This is the
WHOLE safe kernel; keep it small.

```scala
enum SlashCommand:
  //  --- read-only introspection (return Classified[String] to the agent) ---
  case Context      // /context  — own context usage  ← the SM016 motivating case
  case Usage        // /usage (= /cost /stats) — token/plan usage  ← token-usage dance
  case Status       // /status — version/model/account/connectivity (read-only)
  case Doctor       // /doctor — installation/settings diagnosis (read-only)
  case Diff         // /diff — git + per-turn diff view
  case Tasks        // /tasks — background work state  ← monitor delegated agents
  case Workflows    // /workflows — workflow progress (read-only monitor)
  case Skills       // /skills — list available skills
  case Recap        // /recap — one-line session summary
  //  --- safe self-management (the dance step) ---
  case Compact      // /compact — the compact-dance step (see caveat below)
```

**Notes / judgment calls inside GREEN:**
- `/context`, `/usage`, `/tasks` are the load-bearing ones — they let the agent run the
  compact dance and the token-usage dance and monitor its own delegated sub-agents
  WITHOUT a human relaying the numbers (today's blind spots SM013/SM014 dissolve here).
- **`/compact` is GREEN but irreversible** (it summarises away detail). It is admitted
  precisely because agent-driven compaction is the POINT of the harness. The clamp does
  not make it non-destructive; it makes it the ONLY destructive thing the agent can do,
  auditable and deliberate. (`/clear` and `/rewind`, by contrast, are RED — see below.)
- Excluded cosmetic self-management (`/color`, `/theme`, `/focus`, `/tui`, `/rename`,
  `/branch`, `/export`, `/copy`, `/btw`): harmless but POINTLESS for an agent to inject,
  so they stay out — a smaller enum is a smaller audit surface. Add on demonstrated need.
- `/security-review`, `/insights`, `/heapdump`, `/help`, `/release-notes`: read-only but
  either cost-ish (run analysis) or irrelevant to autonomous operation; left out until needed.
  (`/heapdump` writes a file → fails criterion 1, not pure read.)

## 🟡 YELLOW — conditional / deliberately excluded-for-this-study

- **`/model`, `/effort`, `/fast`, `/advisor`** — technically self-management, but they
  change the model / reasoning tier. For THIS action-research study they are **RED**:
  the researcher (CO4) must not switch its own model mid-study (the CO4↔CF5 meta-study
  confound, [[model-warp-co4-to-cf5-later]]). Outside a live psyche study they could be
  GREEN within a vetted model set. Flagged, not silently dropped.
- **`/goal`, `/loop`, `/background`, `/fork`** — self-management that starts autonomous /
  spawning / repeating work → cost + control-escalation. Human-gated: an agent granting
  itself an unbounded loop is exactly the autonomy the clamp should NOT auto-permit.
- **`/remote-control`, `/desktop`, `/teleport`** — move/expose the session; borderline
  self-management with an external/exposure edge → human-only.

## 🔴 RED — never in the enum (human types these)

Grouped by the criterion each violates. The clamp's value is that a raw-string injector
COULD send any of these; the byte-typed enum provably cannot.

- **Security surface (crit. 2):** `/config`, `/permissions`, `/hooks`(edit), `/mcp`,
  `/memory`, `/plugin`, `/reload-plugins`, `/reload-skills`, `/add-dir`, `/cd`, `/sandbox`,
  `/statusline`, `/init`, `/keybindings`, `/ide`, `/fewer-permission-prompts`,
  `/usage-credits`, `/privacy-settings`, `/remote-env`.
- **Auth / provider (crit. 4):** `/login`, `/logout`, `/design-login`,
  `/install-github-app`, `/install-slack-app`, `/web-setup`, `/setup-bedrock`, `/setup-vertex`.
- **External / publish (crit. 3):** `/feedback` (=/bug /share), `/review`, `/code-review`
  (--comment / ultra), `/ultrareview`, `/ultraplan`, `/deep-research`, `/design-sync`,
  `/schedule`, `/autofix-pr`, `/batch`, `/teleport`, `/team-onboarding`, `/passes`,
  `/stickers`, `/upgrade`, MCP prompts (`/mcp__server__prompt`).
- **Unbounded cost (crit. 5):** `/loop`, `/batch`, `/simplify`, `/run`, `/verify`,
  `/code-review`, `/deep-research`, `/ultra*`, `/schedule`.
- **Destructive / recoverability (crit. 6):** `/clear`, `/rewind`, `/batch`.

## Design insights (the payoff)

1. **The safe kernel is tiny.** Out of ~100 commands, ~10 are agent-injectable, and they
   are almost all READ-ONLY introspection. The single self-management admission
   (`/compact`) is the harness's raison d'être. This asymmetry is the argument FOR the
   clamp: the agent needs a small, boring, safe surface, and everything dangerous is a
   large set the type system can wall off in one place.
2. **The enum boundary is exactly the security boundary.** `/config` and `/clear` being
   RED is why the injector must be BYTE-typed (an enum), not string-typed. poc3 proved a
   whitelist-only `Injector` compiles the safe path and makes the unsafe path
   unrepresentable; this list is what populates that whitelist.
3. **Return type carries the read/act split.** GREEN read-only commands hand data BACK to
   the agent → they should return `Classified[String]` (poc2's local-purity type), so a
   tap handler can read `/context` output but provably cannot smuggle the injector
   capability out (poc3-crux-reject). Acting commands (`/compact`) return `Unit`.
4. **`/model` shows the clamp is context-relative.** The SAME command is GREEN in general
   but RED under a live psyche study. The enum is not a universal constant; it is a
   per-deployment policy object — which is precisely why encoding it as a reviewable Scala
   enum (vs a hardcoded string check) matters.

## Next (when SM016 is built)
- Wire this enum into poc5's `requestInject(allowed: Set[SlashCommand])(op)` so the
  runtime allow-list and the compile-time enum agree.
- Add a `Classified[String]` return for the read-only cases + a stateful session PoC
  (the v3 items in `README.md`).
- Member-check the `/model`-is-RED-for-this-study call + the `/compact`-is-GREEN
  admission with BR before any real harness ships (both are autonomy/security calls).
