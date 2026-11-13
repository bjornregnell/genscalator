# Issue 261113-paste-slash-copy-dance: a paste job has no clean hand-off: instructions and paste body mix, and terminal copy adds margins

> status: open 2026-11-13 · labels: skills, dances, channel, human-ux · measured against: genscalator v0.10.3,
> main at `072212a`, Claude Code on Linux, 2026-11-13 · summary: when the agent drafts text the human will paste
> somewhere else (a bug comment, a web form, an email), the human has to mouse-select it out of the terminal. That
> picks up the terminal's left margin, hard line breaks and stray surrounding text, and it is hard to see where the
> agent's instructions end and the paste body begins. Claude Code's `/copy` already solves the mechanics; what is
> missing is a dance (and a skill or cue) that makes the agent shape its output so `/copy` grabs exactly the body.

## Description

A **paste job** is any text the agent writes for the human to paste outside the session: a Launchpad or forge
comment, a form field, an email, a chat message. Today it goes like this:

1. The agent explains what the text is for, then shows the text, often in a code block, then adds notes after it.
2. The human ctrl-click-drags to select the body in the terminal.
3. The paste comes out wrong:
   - the terminal renders every line with a **left margin**, which the selection keeps;
   - the agent **hard-wraps** long lines, so paragraphs arrive as ragged short lines;
   - the selection catches **extra stuff**: the end of the intro, the code-fence edge, the first note after it;
   - in a long reply it is **not easy to tell the instructions apart from the paste body**.

So the human fixes up whitespace by hand in the target form, or asks the agent to "repaste with no margin", or
gives up and copies from a file. That is friction exactly at the outward-facing moment, where a sloppy paste is
public.

**What we learned (field case, 2026-11-13, posting a follow-up comment on a Launchpad kernel bug):**

- Asking for "no left margin, no linebreaks" did not help: the margin is added by the terminal rendering, not by
  the agent's text. The workaround that worked was copying from a file the agent had saved (`bug-report/COMMENT-1.md`).
- Claude Code's built-in **`/copy`** copies the agent's last response to the clipboard **without the terminal
  margin** (it also writes `/tmp/claude-<uid>/response.md`). Tested: two one-line paragraphs pasted clean.
- **`/copy` takes only the text block after the agent's last tool call, not the whole turn.** Tested: a reply of
  the form *text, Edit tool call, text* copied only the final text. So a tool call (for example saving the paste body
  to a file) is a reliable separator between the instructions and the body.
- **`/copy` yields raw markdown.** Backticks, `**bold**` and `- ` bullets arrive as typed. For plain-text targets
  (Launchpad, most web forms, email) backticks and bold show up as literal symbols; `- ` bullets and `1.` lists read fine.

The convention we settled on, which this issue proposes to make a dance:

- **Agent step 1:** say what the text is for, where to paste it, and anything to check, all *before* the body.
- **Agent step 2:** save the body to a file as a durable record (e.g. `bug-report/COMMENT-N.md`). This tool call is
  also the separator that makes `/copy` take only what follows.
- **Agent step 3:** end the turn with **the paste body alone**: no intro, no trailing note, no code fence, one line
  per paragraph or bullet (no hard wrapping, no indentation), markdown syntax only if the target renders markdown.
- **Human step:** type `/copy`, paste. Disclosure lines (e.g. "Claude Code was used to help me write this") are
  the human's to add, so the agent does not put them in by default.

This meets the dance bar in `docs/foundations.md` (≥2 interlocking steps, ≥1 human and ≥1 agent): the agent shapes
the turn, the human closes it with `/copy`.

## How to reproduce it

1. Ask the agent for a two-paragraph comment to paste into a web form.
2. Select it with the mouse in the terminal and paste it into a plain text field: note the leading spaces on every
   line, the wrapped lines, and any intro or note text caught at the edges.
3. Now ask for the same text as described above (instructions, save to a file, bare body last) and type `/copy`:
   the paste is clean, and `/tmp/claude-<uid>/response.md` shows that only the body was copied.

## Acceptance sketch

- A glossary entry **Paste dance** (or *paste-copy dance*) under "Dances and handoffs" in `docs/foundations.md`,
  with the steps above and the three `/copy` facts (no margin, last-block-only, raw markdown).
- A way to trigger it, one of:
  - a small skill (e.g. `paste-job`) whose description triggers on "text for me to paste", "draft a comment / reply /
    email for me to post", "repaste without margin"; or
  - a `gs paste` cue in `gs-dwim` that re-emits the last paste body in the dance's shape.
  The skill route is probably better: the agent should do this unprompted whenever it produces a paste job.
- The skill states the **target format** rule: plain-text targets (Launchpad, most web forms, email) get no
  backticks or bold; markdown targets (GitHub, GitLab) may keep them. If unsure, plain text.
- The skill says where the durable copy goes: next to the work it belongs to if there is an obvious home (a
  `bug-report/` folder), otherwise the session scratchpad, and says the path in step 1.
- Note the dependency honestly: the last-block-only behaviour of `/copy` is observed, not documented, as of
  2026-11-13. The skill should tell the human how to check (`response.md`) the first time, and keep the file copy as
  the fallback if `/copy` changes.

## Discussion
