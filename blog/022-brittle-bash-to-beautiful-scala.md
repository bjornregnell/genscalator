# From brittle bash to beautiful Scala (working title)

> **Status: RAW DUMP 2026-07-14 (agent-drafted from a live session; deliberately TOO LONG to publish, to be pruned; BR to revoice).** A concrete "agent-assisted engineering in the small" story: the agent helps fix a long-standing nag on the author's box, first in brittle bash, then rewritten in direct-style Scala, with a lesson about the knowledge an elegant refactor can quietly lose.
> **Audience:** developers curious what agent-assisted engineering actually looks like on a real, small, annoying task; Scala folk; anyone who has an "ugly script that works" they are afraid to touch.
> **[RAW / SCAFFOLD, agent-drafted, keep details and prune later. Grounded in the live session of 2026-07-14. No named political content. BR revoices before publish.]**

## The nag

The author's box has a small, chronic annoyance: the audio output (the DAC and the fancy speakers) goes to sleep, and the default input and output devices reset on every boot. So there is a cryptic little bash script in `~/bin` that he runs once per boot to (a) pin the Blue Yeti X microphone and the onboard `iec958` digital output as the defaults, and (b) play a short `speaker-test` tone to wake the DAC. It works, but it is brittle sorcery, the load-bearing line being:

```bash
speaker-test -t wav -c 2 -f 440 & sleep 3 && kill -9 "$!"
```

## How it surfaced (the accidental route in)

We were not trying to fix the audio at all. We had just wired a desktop "bing-bing" notification that fires when the harness needs the human's approval, so a human who has wandered off to another window gets called back. Testing it, the visual notice appeared but no sound. A short debugging tangent followed (was it the hook? the audio environment? the sound player?), and the culprit turned out to be none of those: the speakers were simply asleep. A hardware false alarm. That is how the wake script came onto the table in the first place.

## Attempt 1: the agent "cleans up" the bash, and breaks it

The agent offered a tidier version: `timeout 3 speaker-test ...` instead of the backgrounded `& sleep 3 && kill -9 "$!"`, and, having noticed that the old `-f 440` frequency flag was silently ignored under `-t wav`, it "fixed" that by switching to `-t sine -f 440` so the frequency would take effect. Two bugs in one elegant refactor:

1. **Wrong sound.** `-t wav` is the spoken "Front Left / Front Right" stereo test, a pleasant and genuinely useful per-channel check the author likes (it confirms left is left and right is right). `-t sine -f 440` is an endless irritating tone. He never wanted the 440; it was vestigial. The refactor "fixed" the flag he wanted gone and destroyed the sound he wanted kept.
2. **The hang.** `timeout 3` sends SIGTERM, which `speaker-test` ignores mid-playback, so it beeped forever and the shell prompt never returned. The original `kill -9` used SIGKILL precisely because `speaker-test` will not die on a gentle signal.

## The lesson: the ugly code was ugly for a reason

The "brittle" `kill -9` was not brittleness. It encoded hard-won knowledge: `speaker-test` ignores SIGTERM, so you need SIGKILL. The agent's elegant refactor threw that knowledge away and reintroduced the exact bug the ugly line existed to prevent. This is the cautionary heart of the story. An agent optimising for elegance can silently delete domain knowledge that lives, uncommented, inside "ugly" code, and it arrives fast and wearing a finished face. The human who knows WHY the code is ugly is the safeguard.

## The fix, then the twist: bash to Scala

Corrected first in bash: back to `-t wav`, drop the `-f 440`, and `timeout -s KILL 3` (SIGKILL, the clean equivalent of `kill -9`, no PID juggling). Then the twist from the author: could the script be a Scala script instead (`sound.sc`, a `scala-cli` shebang, `chmod +x`)? "Scala less brittle than bash sorcery." Yes.

## Beautiful Scala: the knowledge made explicit

The Scala rewrite is a clearly-marked effectful driver, and it leans on the JDK: `java.lang.ProcessBuilder(...).start()`, then `if !p.waitFor(3, SECONDS) then p.destroyForcibly()`. `destroyForcibly()` IS a SIGKILL. So the entire folklore stack (the `timeout` binary, the `-s KILL` flag, the `kill -9 "$!"` PID juggling, the SIGTERM-ignored hang) collapses into one typed, self-documenting JDK call whose behaviour is obvious without running it. Device resolution (parse `pactl list short`, match a stable name substring, warn on zero or many matches) becomes typed Scala with real error handling instead of `awk` plus a footgun. It compiled clean on the first try, and the compile itself ran without a permission prompt because the loaded `scala-style` skill had granted the `scala-cli compile` tool.

> "me very happy for new scala script that does not look like sorcery :)"
>
> (BR's verbatim reaction, 2026-07-14. The punchline: the whole point was to turn sorcery into code that reads like what it does.)

## The so-what

Agent-assisted engineering on a real, small task is neither magic nor useless. It is a collaboration in which the agent's speed and breadth meet the human's knowledge of WHY. The agent can propose the tidy version, translate bash sorcery into a typed Scala driver, and compile-check it in seconds. It will also, confidently, "improve" away knowledge it cannot see, so the human stays the load-bearing reviewer. It is the genscalator posture in miniature: direct style, JDK first, effects in a clearly-marked driver, and structure (a typed call, a compile check) chosen over folklore (a signal you have to remember and a PID you have to kill).

## The sharper point: robustness is only safe if the owner can review it

**[scaffold, BR to revoice; this is BR's own correction and it is the real lesson.]** The author was not an unwilling victim of over-engineering. He shares the values: he liked what the agent was doing, it was "good sorcery" in that it was managing the brittleness, and he agreed to all the bulk to make the script safer. His words for what went wrong:

> "I agreed to all the bulkyness to make it safer but I lost my reviewability in that process because I don't know bash very well."

That is the crux. The added robustness made the script safer AND less reviewable by its owner, so he had to *trust* the forty lines of bash rather than verify them. Robustness you cannot read is robustness you cannot check.

Which is why the rewrite mattered beyond taste. Scala is a language the author reads, so the same robustness, moved into Scala, became *verifiable* by him again: a typed call he can follow, a compile that either passes or does not. The medium decided whether the owner verifies or trusts. Bulky bash he does not know forces trust; typed Scala he does know restores verification. So the design rule is not "keep it simple" (he wanted the safety), it is "keep it reviewable BY THE OWNER", and if added robustness outruns their literacy in one medium, move it to a medium they can read rather than pile more into one they cannot.

## The compiler is the reviewer that never gets tired: a lie in prose compiles

**[scaffold, BR to revoice. This is BR's own twist, added 2026-07-17, and it is the thing that finishes the
argument the rest of the post only circles.]**

> "if it doesn't compile it is false code that never cost us those runtime bugs the compiler caught for us
> (compare it to the alternative: brittle bash or agent on-the-fly generated do-whatever-at-runtime-python)"
>
> (BR's verbatim, 2026-07-17.)

The section above says the medium decides whether the owner verifies or trusts. That is true, and it is only half
the story, because it puts the whole burden on the owner. The other half is that the typed medium comes with a
reviewer of its own, one that reads every line, every time, and never gets bored or tired or generous.

Think about what a comment costs when it is wrong. Nothing. It compiles. It ships. It sits there being false for
years, and the only thing that ever catches it is a human who happens to read it and happens to care. Now think
about what a *type* costs when it is wrong. It does not compile. You cannot ship it. The falsehood is rejected
before it exists.

**That is the whole point, and it generalises past comments.** A lie in prose compiles. A lie in typed code does
not. Anything you can move from the first category into the second stops being something you have to remember and
starts being something you cannot get wrong.

Which reframes what the `destroyForcibly()` rewrite actually bought. It was not elegance, and it was not even
mainly readability. The bash version encoded the SIGKILL knowledge in *folklore*: a `-9` you had to know, a `$!`
you had to juggle, and nothing anywhere that would object if you got it wrong. The agent proved that by getting it
wrong, confidently, in a refactor that looked better. The Scala version encodes the same knowledge in a *typed JDK
call that either exists or does not*. There is no version of `destroyForcibly()` that quietly means SIGTERM. The
knowledge moved from something the author had to hold in his head into something the build holds for him.

**Now compare the two alternatives in BR's twist, because they fail in the same way for the same reason.**

Brittle bash is not prose, but it behaves like it. There is no build step, so nothing is ever checked. An unset
variable becomes an empty string and the empty string becomes an argument and the argument deletes something. The
first time bash tells you the code is false is at runtime, in production, in front of a user, if it tells you at
all. And an agent generating throwaway Python at runtime is the same thing with better syntax: code that has never
been checked by anything, written by something that cannot remember writing it, running immediately. **Both are
prose that executes.** They have all of code's power and none of code's checking, which is the worst possible
trade, and they are attractive for exactly the same reason a comment is attractive: they are fast to write and
nothing argues back.

So the rule the whole post has been groping toward is not "Scala is nicer than bash". It is: **prefer the medium
that rejects your falsehoods for you.** That is why our toolbox is typed Scala instead of a shell-script pile, and
it is why an agent that reaches for a quick interpreted one-liner should be stopped, not admired for its speed. We
already had a security rule that says never allowlist an interpreter, on the grounds that an interpreter is a blank
shell. **BR's twist gives that rule a second, independent reason: an interpreter is prose that runs.** When two
different arguments land on the same rule, the rule is probably right.

The whole argument fits in one small table, and the useful thing about it is that the question along the top is not
"is this code?" but "when do we find out we were wrong?":

| what you wrote | when do you find out it is wrong? |
|---|---|
| **prose** - a comment, a note to yourself, a rule in a document | **never** |
| **unchecked code** - a bash one-liner, a script the agent generated on the fly | **when it runs, in front of a user, if you are lucky** |
| **typed code** - a Scala call that has to satisfy the compiler | **before it runs, in seconds, for free** |

The middle row is the one to watch, because it is the one you reach for when you are in a hurry, and it is the worst
of the three. It has all of code's power and none of code's checking. The top row at least admits it is only talk.

> ### "The Scala compiler is our go-to favorite guard."
>
> (BR, 2026-07-17.)

Which is the punchline, and it is a commitment rather than an observation. We have spent a lot of this project
building guards: a hook that inspects commands before they run, rules about what may be approved, tools that refuse
to do the dangerous thing. And the whole time, the best guard we own was already running on every save, for free,
complaining in specific detail, and asking nobody's permission. **We just never called it a guard, because it came
with the language.**

**And the word Scala in that line is load-bearing, not a preference.** The bottom row of the table is not earned by
compiling. C compiles, and C will cheerfully watch you free the same memory twice and say nothing. That is the point
of the benchmark section further down, where the conclusion was that C is fastest and we still do not want it:
brittle is not a speed property, it is a *what-can-go-uncaught* property. So the useful question is never "is this
compiled?", it is **how much of what I know can I hand to the type system, and will it hold it?** A richer type
system takes more of the knowledge off your hands. That is the whole reason the ranking ends where it does.

Which also fixes the limit of the argument, honestly. A compiler rejects **false** code, not **wrong** code. It will
compile a beautifully typed function that computes utter nonsense, and no amount of types will tell you that your
translation of a Swedish string was half finished. So this is not a claim that the compiler thinks for you. It is a
claim that the line between "the machine catches this" and "a tired human has to catch this" is **a line you can
move**, and moving it is most of what good design is.

So when there is a choice about where to put a piece of hard-won knowledge, the ranking is not about taste, and it is
barely about elegance. Put it where the compiler can hold it, and it will guard it for you forever without either of
us having to remember it was there.

**[figure: the table above, drawn properly, with the three rows as a timeline instead of a list, so "never" sits off
the right edge of the page. Concept diagram, no real data needed; it earns its place.]**

## "But isn't the fast one better?" We measured

**[scaffold, BR to revoice; grounded in `research/wr-data/approval-wake-launcher-startup-bench-2026-07-14.md`.]**
The natural objection to "rewrite it in Scala" is speed: bash is interpreted, so surely a compiled language wins,
and for a latency-critical hook we should reach for C, or for Scala Native compiled straight to a binary via
LLVM. BR raised exactly this about the approval-wake notifier: "bash is still interpreted so C should be faster,
no? and Scala Native compiled to a binary with LLVM would be almost as lean and mean." So instead of arguing, we ran a
quick **prestudy**: no-op launchers in bash, C, and Scala Native (in several garbage-collector and optimization
configurations), 300 timed startups each. A no-op wall-clock benchmark, enough to settle the launcher question,
not a rigorous profiling study (see Future Work).

The medians: **C 0.77 ms, bash 1.69 ms, Scala Native 1.7 to 1.9 ms.** So:
- C is genuinely fastest, about 2x bash. But the whole spread is roughly **one millisecond.**
- Scala Native never beats bash-tier and never approaches C. It has a fixed floor from its runtime init (about
  1.7 ms, and a 1.64 MiB binary for a program that does nothing), and neither turning the garbage collector off
  nor switching optimization mode moves it much. "Lean and mean" turns out to be about *compute*, not *startup*.
- The bottleneck this hook actually fights, the audio subsystem waking up, is measured in **seconds.** The
  launcher language sits three to four orders of magnitude below it. The millisecond C saves is invisible.

BR's summary, arrived at live as the numbers landed:

> "we can never beat C ... C is brittle ... Scala is good"

That is the fit-to-task rule in three lines. You cannot beat C on raw startup, and here you never need to,
because the speed you would win is *unspendable* while the brittleness you would inherit (memory-unsafety,
undefined behaviour, manual everything) is real. The genuine variable is safety versus brittleness; raw speed is
a red herring wherever it lands below the true bottleneck. Same lesson as the bash-to-Scala rewrite, one axis
over: reach for the lean, brittle tool only where its leanness actually shows up in the answer.

**One question over.** The same benchmark, extended, answers something bigger than "which launcher": *how do
you run any Scala tool fast?* Put a no-op on the plain JVM and startup jumps to about **130 ms**, the tax every
JVM-based command-line tool pays on every single call (and our toolbox tools pay roughly 500 ms, because the
runner adds its own layer on top). Two native routes erase it: Scala Native at about **1.9 ms** and GraalVM
native-image at about **3 ms**. And a genuine surprise: Scala Native beats GraalVM native-image on *both*
startup and binary size (1.64 MiB versus 12.25 MiB), even though GraalVM native-image is usually assumed the
gold standard. That target-selection thread (JVM versus Scala Native versus GraalVM) is its own companion post, *The noop
race* (025), and it is exactly where the millisecond *does* start to matter: a tool invoked a thousand times
pays its startup a thousand times.

## [figure: bar chart of the five median startup times (C, bash, SN x3) with the seconds-scale audio-wake latency drawn to scale beside them, so the ~1 ms launcher spread visibly vanishes]

## [figure: the before / after, the brittle bash one-liner beside the Scala `destroyForcibly` call]

## Future Work

**[scaffold, BR to revoice.]** The performance beat above is a *prestudy* on a no-op, and it earned its keep
cheaply, but a rigorous version is deliberately out of scope for this post:
- **Profile, do not infer.** Flamegraphs and `perf`/`strace` of the startup path would attribute Scala
  Native's fixed floor to specific init cost, instead of reasoning it from the gap to C.
- **Benchmark the real job, not an empty `main`.** A launcher that actually resolves a device and plays a
  sound is the fair test of "lean and mean"; a no-op flatters no one.
- **The real latency lever.** Linking libcanberra in-process (C, or Scala Native FFI) to kill the
  `canberra-gtk-play` fork+exec is the only thing that could move *wake* latency. But the screen-lock /
  PulseAudio sink-resume cost should be measured first; if that dominates, the launcher is moot.
- **Honest aside:** the prestudy already burned three native-compile runs to compare a program that does
  nothing. The thorough study is a bigger ordeal, which is exactly why it is future work and not this post.

## Further Reading

[TODO, verify each link resolves and is on-topic before shipping, per the blog link rule:
- speaker-test / ALSA utils
- SIGTERM vs SIGKILL (signal semantics; why a process can ignore SIGTERM but not SIGKILL)
- scala-cli shebang scripts
- pactl / PulseAudio-PipeWire default devices]
