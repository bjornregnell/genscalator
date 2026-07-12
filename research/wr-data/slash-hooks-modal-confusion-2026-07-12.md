# WR specimen: /hooks nested-modal UX gives no clear "it stuck" signal (2026-07-12)

**Observation.** BR ran `/hooks` to load the newly-added `guardcheck` PreToolUse hook. He hit **a confusing
series of nested modals**, "never knowing if it really sticks," and the only way back was **ESC**, which
replayed the modal stack in reverse. The confirmation that the hook IS registered is **buried in an inner
"Hook details" panel**: Event `PreToolUse` / Matcher `Bash` / Type `command` / **Source: Local settings
(.claude/settings.local.json)** / Command `tt guardcheck hook`.

## Why it matters (trust-signal miscalibration, on a security action)
- `/hooks` is a **read-only drill-down browser** (Event → Matcher → Hook → Details), unwound by ESC. There is
  **no explicit save/confirm and no top-level "this hook is ACTIVE" affirmation**, so the user cannot tell the
  change stuck without digging to the leaf panel. For a **security** config (a guard firing on every Bash call),
  the missing "applied/active" signal is exactly where user trust erodes.
- **Sibling to the same day's opposite failure** ([[agent-can-self-edit-settings-no-prompt-2026-07-12]]): the
  settings EDIT was **too silent** (auto-allowed, no prompt); the `/hooks` VIEW is **too noisy / ambiguous** (many
  modals, no clear outcome). Both are trust-signal miscalibrations around the same security surface — one
  under-signals, one over-signals, neither says plainly "here is the guard, it is on."

## The reassurance (what actually is true)
The inner detail panel showing **Source: Local settings** IS the applied state. `/hooks` reads the persistent
settings file, so the hook **sticks across sessions** (it is not a session toggle); there is no save button
because the UI mirrors the file. Seeing the detail = it is live. **Empirical proof available:** run one harmless
banned shape (e.g. `echo a && echo b`) and watch guardcheck deny it — end-to-end confirmation the guard fires.

The panel footer BR pasted closes it: **"To modify or remove this hook, edit settings.json directly or ask Claude
to help. Esc to go back"** - i.e. the hook IS installed and the modify path is editing the settings file. So the
information the user needs ("it stuck; here is how to change it") exists, but only at the bottom of a deep
drill-down, not as a top-level "applied" signal.

Ties: `harness-ux`, [[agent-can-self-edit-settings-no-prompt-2026-07-12]], [[guardcheck-hook-structural-fix]],
SM022 (a live "active guards" indicator on the dashboard would close this gap), [[guard-against-forced-confirmations]].
