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

## [figure: the before / after, the brittle bash one-liner beside the Scala `destroyForcibly` call]

## Further Reading

[TODO, verify each link resolves and is on-topic before shipping, per the blog link rule:
- speaker-test / ALSA utils
- SIGTERM vs SIGKILL (signal semantics; why a process can ignore SIGTERM but not SIGKILL)
- scala-cli shebang scripts
- pactl / PulseAudio-PipeWire default devices]
