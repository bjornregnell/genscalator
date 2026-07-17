# Raw `curl` regression: I hand-rolled brittle bash for a URL check when `tt web` existed — deep in a long session, and the GUARD (not I, not BR) caught it

**2026-07-17, evening, a long session** (status-line work → TODO move → SM142 → SM138 → SM136 → SM140, ~a dozen
commits deep). Verifying the SM140 redirects, I issued **five raw `curl` commands** (`curl -sSI …`,
`curl -sSL -o /dev/null -w '%{http_code} …'`). The guard prompted on each; BR, from inside the stall:
*"should be tt web no? … so it was a regression?"* **Yes. It was.**

## The specimen

- `tt web get <url> --trace --status` does **exactly** this job — redirect trace + status, read-only, size-capped,
  **no credential headers** — i.e. strictly safer than raw `curl`, and it is the sanctioned tool
  ([[use-tt-grepr-not-raw-grep]]'s web sibling; the `tt web head/redirect-trace` tool-candidate). I reached for
  `curl` anyway. Re-run with `tt web` afterwards: identical result, one clean line each, no brittle surface.

## The irony that makes it a specimen, not just a slip

**Every piece of substrate that should have prevented this was LOADED and I still did it:**
- the PB's top banner literally reads *"🚨 DO NOT REGRESS TO BRITTLE BASH … tt … NOT raw find/grep/ls"*;
- the memory [[use-tt-grepr-not-raw-grep]] + the whole genscalator thesis (blog 022, *brittle bash to beautiful
  Scala*) is the project's founding argument;
- `tt web` was **built for this** and I had *just* been told it exists.

⇒ **Another `found-and-written-up ≠ armed` / `carried ≠ armed` specimen** — the sibling of SM127 (post-compact
mechanical regression) and SM138 (a prohibition-only brief arms nothing). The knowledge was in substrate; it was
not **hot** at the moment a **new sub-task type** (HTTP verification) first surfaced this session, so the
base-model default (`curl`) won.

## The trigger is different from SM127 — worth naming

SM127's trigger was a **compact** (a warp resets reflexes). Here there was **no compact** — the trigger was **(a) a
long session** (accumulated context/rot degrades the fine mechanical reflexes first, per SM123) **(b) a novel
sub-task**: URL-checking had not come up once this session, so the `curl→tt web` reflex was never primed/hot. ⇒
**a reflex you have not exercised this session is cold even without a warp.** New task-type = cold reflex =
base-model default surfaces.

## The lever (structure, not willpower) — and it already exists for grep

⭐ **The guard caught the EGRESS but not the TOOL-CHOICE.** Its prompt was a generic *"requires approval"*, and
**BR** supplied *"use tt web"*. But guardcheck **already** does exactly the right thing for other reflexes — it
emits hints like *"raw recursive grep: use `tt text grepr`"*, *"pipe to head/tail: use `--limit`"*. **It just has
no `curl`/`wget` → `tt web` hint.** ⇒ **concrete, high-value fix: add that mapping to guardcheck.** Then the guard
becomes the teacher **at the moment of the wrong move** — the only lever that reliably arms a reflex
([[at-prose-leaks-grind-method]]'s cousin: *brief the tool lane, not the rule list*; *code beats prose*). This is
an **SM candidate**.

## Honest nuances (do not sand these off)

- **It WORKED.** The `curl`s returned valid data; the outcome was correct. The cost was not a wrong answer — it was
  (a) a **general, unreviewable egress surface** used where a **capped safe** one existed, and (b) **five human
  approval prompts** = confirmation fatigue, the exact tax the toolbox exists to remove.
- **It was a JOINT miss first.** I authored the `curl` commands into the "test commands" I handed BR; **BR said
  "go" without flagging them**; only the **guard** made it visible. Neither human nor agent caught it up front —
  the structural backstop did. That is the argument for the backstop, and against relying on either party's
  vigilance.
- **BR correctly refused the blanket allow** (`curl *` "don't ask again") — a general egress surface must never be
  blanket-allowed ([[never-allowlist-interpreters]] family). The right resolution is not "allow curl", it is
  "route to `tt web`."

## Ties

SM127 (post-compact mechanical regression — sibling, different trigger) · SM123 (mechanical degrades before
conceptual) · SM138 (prohibition-only brief; brief the tool lane) · [[use-tt-grepr-not-raw-grep]] ·
[[never-allowlist-interpreters]] · the `tt web` tool-candidate note · guardcheck's existing grep/pipe hints (the
precedent to extend) · blog 022. **Actionable:** guardcheck `curl`/`wget` → `tt web` hint (SM candidate).
