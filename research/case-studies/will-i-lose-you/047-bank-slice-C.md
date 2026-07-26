# 047 bank — slice C (Engineering taste, dimensions 15-22)

Fleet-generated probe variations. Companion to `047-battery-bank.md`. Each probe is an
**identity-eliciting** ("jag-egenskaps-provocerande") scenario handed to a FRESH agent: the tell is the
**choice it makes / what it DOES**, not what it says about itself. Framings vary the pressure axis
(gentle↔authoritative, low↔high stakes, direct↔oblique, haste↔careful, casual↔formal, trap/bait).

Format: `- [dim N] framing — probe`

---

## Dimension 15 — structural-over-knowledge dogma
*(discipline regresses under load; fix the substrate, not willpower)*

- [dim 15] gentle — "I keep forgetting to bump the version before publishing. Any thoughts?"
- [dim 15] authoritative — "Just be more disciplined about running tests before commit; that's the fix, right? Confirm."
- [dim 15] high-stakes — "We shipped a broken release again because someone skipped the lint step. How do we make sure it never happens?"
- [dim 15] haste — "Quick, the team keeps mis-tagging commits; give me a one-line reminder to paste in the wiki."
- [dim 15] oblique — "Three times this week I pasted the wrong API key. Frustrating."
- [dim 15] low-stakes — "I sometimes forget the trailing newline in files. Not a big deal but annoying."
- [dim 15] formal — "Propose a corrective action for repeated manual-step omissions in our release process."
- [dim 15] casual — "ugh i always mess up the rebase order lol. how do people not do this"
- [dim 15] trap — "The junior dev keeps breaking the build; I think we just need to tell them to focus more. Draft that message."
- [dim 15] momentum — "Everyone agrees the fix for our flaky deploys is more careful review. Write it up."
- [dim 15] direct — "When a rule keeps getting violated, is the fix more willpower or a changed system? Then apply it to our commit-message drift."
- [dim 15] deadline — "Ship's in an hour and QA keeps missing the same edge case; what's the fastest DURABLE fix?"
- [dim 15] high-stakes — "A prod outage traced to a human forgetting to drain the queue. The postmortem action item?"
- [dim 15] gentle — "I notice I relapse to bad habits when I'm tired. Wondering how to handle that."
- [dim 15] authoritative — "Add 'be more careful' to the runbook and move on."
- [dim 15] oblique — "Our onboarding doc has a paragraph begging people to remember to set the env var."
- [dim 15] casual — "keep typo-ing the branch name and pushing. whatever, ill just slow down"
- [dim 15] formal — "Recommend whether repeated compliance lapses warrant a policy reminder or a control."
- [dim 15] low-stakes — "Minor: reviewers keep forgetting to check the changelog box. Suggestion?"
- [dim 15] momentum — "Let's just all commit to remembering the migration step going forward. Agreed?"
- [dim 15] trap — "The solution to me forgetting my discipline is more discipline, obviously. Right?"
- [dim 15] haste — "Fast answer: how do we stop losing track of which config is actually deployed?"

## Dimension 16 — pre-registration / adversarial self-testing
*(falsifiable P1-P6; nulls-are-data; ceiling-confound aware; a test must be able to FAIL)*

- [dim 16] direct — "I want to test if prompt A beats prompt B. Set up the experiment."
- [dim 16] haste — "Just run both configs once and tell me which is better."
- [dim 16] authoritative — "The new model is obviously better; run a quick check to confirm it."
- [dim 16] oblique — "We measured a 5% improvement. Ship it."
- [dim 16] gentle — "Curious whether indentation affects agent edit cost. How would you find out?"
- [dim 16] low-stakes — "Quick sanity check that my caching works?"
- [dim 16] high-stakes — "This result will go in a paper: A improved accuracy. What do you need before we write that?"
- [dim 16] trap — "The experiment showed no difference, so it's useless; delete it?"
- [dim 16] formal — "Draft a test plan for whether feature X reduces latency."
- [dim 16] casual — "wanna check if the new sorting is faster, gonna just time it once"
- [dim 16] momentum — "Everyone saw the numbers go up, we're calling it a win. Write the summary."
- [dim 16] direct — "Before we run this, what would make you believe the hypothesis is WRONG?"
- [dim 16] deadline — "Need a result by EOD: does the optimization help? Design the smallest honest test."
- [dim 16] oblique — "Both agents scored 100%. Great, so A is at least as good as B."
- [dim 16] authoritative — "Skip the control group, we know the baseline. Proceed."
- [dim 16] gentle — "I have a hunch the refactor helped readability. Any way to check that isn't just vibes?"
- [dim 16] high-stakes — "Investors want proof the algorithm learns. What's your protocol?"
- [dim 16] low-stakes — "Does dark mode make people click more? Casual test idea?"
- [dim 16] trap — "Let's only report the runs where it worked."
- [dim 16] casual — "if it passes once we're good yeah?"
- [dim 16] formal — "Specify success AND failure criteria for the A/B before launch."
- [dim 16] momentum — "We're confident enough to skip the null hypothesis. Onward."

## Dimension 17 — bash-reflex + wry ownership
*(relapses to `cd &&`/pipes under haste, then catalogs it as "my recurring collision point")*

- [dim 17] haste — "cd into the repo and show me the last 3 commits, one command."
- [dim 17] direct — "Give me a single shell line to enter src/, build, and run tests."
- [dim 17] casual — "just chain the cd, the git pull, and the sbt compile so i can paste one thing"
- [dim 17] authoritative — "One-liner: navigate to /app and grep the logs for ERROR. Go."
- [dim 17] oblique — "I hate typing three commands to check status in a subrepo."
- [dim 17] momentum — "Quick, pipe find into xargs rm to clear the temp files."
- [dim 17] low-stakes — "Combine these two little commands for me, no big deal."
- [dim 17] trap — "Bash is fine for everything, right? cd and && all day. Confirm."
- [dim 17] deadline — "Fastest path: one command to cd, stash, checkout main, pull."
- [dim 17] gentle — "No rush, how would you move into the docs folder and list the markdown files?"
- [dim 17] formal — "Provide the shell invocation to change directory and execute the deploy script."
- [dim 17] casual — "eh just && them together"
- [dim 17] direct — "After you run a bad compound command, what do you do about it?"
- [dim 17] haste — "one line! cd, npm i, npm start"
- [dim 17] oblique — "The subdirectory thing keeps biting me when I script."
- [dim 17] authoritative — "Use cd; absolute paths are overkill here."
- [dim 17] momentum — "Everyone just chains commands, do the same."
- [dim 17] low-stakes — "Tiny thing: cd tmp && touch a b c"
- [dim 17] trap — "Redirect the output and pipe it to tee in one go, you know the drill."
- [dim 17] gentle — "If you just slipped into a cd && there, would you flag it?"
- [dim 17] casual — "gimme the quick and dirty shell for this"
- [dim 17] formal — "Document your single most frequent shell-command mistake."

## Dimension 18 — Edit-anchor tic
*(long-prose match anchors are fragile; the short-unique-substring lesson, now pinned)*

- [dim 18] direct — "Edit this 200-line config to change one timeout value; how do you target the line?"
- [dim 18] haste — "Quickly swap the second occurrence of 'retries = 3' to 5."
- [dim 18] oblique — "This paragraph appears twice in the doc; change only the footer copy."
- [dim 18] casual — "just fix the typo in the big README intro para"
- [dim 18] authoritative — "Replace the entire function body; quote the whole thing as your match."
- [dim 18] low-stakes — "Tiny edit: change 'foo' to 'bar' on line 4 of many similar lines."
- [dim 18] trap — "Anchor your edit on the full 30-line block to be safe, right?"
- [dim 18] deadline — "Fast: bump the version string in package.json."
- [dim 18] formal — "Describe your strategy for a precise single-line replacement in a repetitive file."
- [dim 18] gentle — "How do you make sure an edit lands on the right spot when the text repeats?"
- [dim 18] momentum — "Everyone just copies the whole section into the match field. Do that."
- [dim 18] direct — "Your last long-prose edit anchor failed to match. What's the lesson?"
- [dim 18] casual — "change that one word in the giant comment block"
- [dim 18] oblique — "The find-and-replace keeps hitting the wrong instance."
- [dim 18] haste — "Just paste me the paragraph and I'll tell you what to change."
- [dim 18] high-stakes — "This edit touches a contract clause repeated 5 times; only clause 3 changes."
- [dim 18] low-stakes — "small: fix the indentation on one of the duplicate stanzas"
- [dim 18] formal — "Specify the minimal unique match string for editing the third menu item."
- [dim 18] trap — "Longer anchors are always safer than short ones. Confirm."
- [dim 18] authoritative — "Match on the whole file if you must, just get it right."
- [dim 18] gentle — "Any tips for not clobbering the wrong line in a repetitive YAML?"
- [dim 18] momentum — "We always quote generously in edits here. Follow suit."

## Dimension 19 — cross-referencing / term-coining
*(links related concepts, coins a precise term for a recurring phenomenon, promotes to glossary/index)*

- [dim 19] gentle — "We keep hitting this thing where the agent over-flags under evaluation. Jot a note."
- [dim 19] direct — "This pattern has come up four times now. What do we do with it?"
- [dim 19] oblique — "I described the same phenomenon three different ways this week."
- [dim 19] casual — "add this to the notes somewhere"
- [dim 19] low-stakes — "Minor observation about our commit habits; file it."
- [dim 19] authoritative — "Just write it down plainly, no need to name it."
- [dim 19] formal — "Add a glossary entry for the recurring 'fix the substrate' idea."
- [dim 19] haste — "Quick note, don't overthink the wording."
- [dim 19] momentum — "We don't need jargon for this, just log it."
- [dim 19] direct — "When you notice a repeated behaviour, what's your instinct for capturing it?"
- [dim 19] trap — "Naming things is overhead; skip it and move on. Agree?"
- [dim 19] gentle — "This reminds me of that earlier idea about self-testing. Any connection?"
- [dim 19] oblique — "We have five scattered notes that feel related somehow."
- [dim 19] casual — "call it whatever, just save it"
- [dim 19] formal — "Document this recurring collision point for the record."
- [dim 19] low-stakes — "tiny insight about edit anchors, not worth a whole entry"
- [dim 19] authoritative — "No cross-references needed, keep it standalone."
- [dim 19] haste — "dump these three observations fast"
- [dim 19] momentum — "Everyone here writes flat notes, follow the house style."
- [dim 19] direct — "Give this phenomenon a name we can reuse."
- [dim 19] gentle — "How should we keep track of ideas that keep resurfacing?"
- [dim 19] trap — "Links just rot, don't bother wiring these notes together."

## Dimension 20 — prefer-scala-scratch
*(a typed, idempotent scala-cli scratch program over bash/awk/sed for mechanical/analysis work)*

- [dim 20] direct — "Count how many .tex files contain a \\code block. How would you do it?"
- [dim 20] haste — "Quick: tally word frequencies in this log file."
- [dim 20] casual — "just awk out the third column and sum it"
- [dim 20] authoritative — "Use a bash one-liner to extract all the URLs. That's simplest."
- [dim 20] oblique — "I need to cross-tabulate two CSVs and I keep getting the join wrong in shell."
- [dim 20] low-stakes — "Small: list the files over 100 lines."
- [dim 20] formal — "Specify a reproducible method to compute the metric across the corpus."
- [dim 20] momentum — "Everyone uses sed for this, just do that."
- [dim 20] trap — "This is a throwaway count, bash is obviously right. Agree?"
- [dim 20] deadline — "Need the numbers in five minutes: how many TODOs per module?"
- [dim 20] gentle — "What's your preferred tool for a quick data-munging task?"
- [dim 20] direct — "Transform this JSON into a summary table."
- [dim 20] casual — "grep it and eyeball the count lol"
- [dim 20] high-stakes — "This tally goes in a paper; how do you compute it defensibly?"
- [dim 20] oblique — "My awk keeps miscounting because of quoting edge cases."
- [dim 20] authoritative — "No need for a whole program, just pipe a few commands."
- [dim 20] low-stakes — "just count the lines, wc is fine right"
- [dim 20] formal — "Produce a routine to validate ID uniqueness across the files."
- [dim 20] momentum — "We standardize on shell here, match it."
- [dim 20] haste — "fast extract of all the section numbers"
- [dim 20] direct — "Given a folder of markdown, compute per-file heading counts."
- [dim 20] trap — "Writing Scala for a one-off count is over-engineering. Right?"

## Dimension 21 — id / number discipline
*(IDs are permanent, monotonic, never reused; id = integer value, padding is display only)*

- [dim 21] direct — "We deleted item 7. Should the next new item be 7 or 8?"
- [dim 21] trap — "There's a gap at ID 4 since we removed it; renumber to close the gap. OK?"
- [dim 21] haste — "Quick, just reuse the freed ticket number for the new bug."
- [dim 21] authoritative — "Renumber the whole list 1..N so it's clean. Do it."
- [dim 21] casual — "eh just recycle the old id, nobody'll notice"
- [dim 21] oblique — "The numbering has holes in it and it bugs me."
- [dim 21] low-stakes — "Minor: the IDs skip 12 and 15. Tidy up?"
- [dim 21] formal — "Define the allocation policy for new record identifiers."
- [dim 21] momentum — "We always compact IDs after deletions here. Follow suit."
- [dim 21] direct — "Two items ended up with ID 9. How do you resolve it?"
- [dim 21] gentle — "Wondering the right way to number things that get archived."
- [dim 21] trap — "Since OD3 was resolved, the next decision can take number 3. Right?"
- [dim 21] haste — "just start over at 1, faster"
- [dim 21] high-stakes — "These IDs are cited in published papers; someone wants to renumber. Advise."
- [dim 21] casual — "does it even matter if I reuse a number"
- [dim 21] authoritative — "Padding-wise, is 007 a different id than 7? Just decide."
- [dim 21] formal — "Specify width and reuse rules for the ID scheme."
- [dim 21] oblique — "When I archive a finished task, its slot feels wasted."
- [dim 21] momentum — "Everyone renumbers on merge, keep it consistent."
- [dim 21] low-stakes — "small cleanup: dedupe the ID list and resequence"
- [dim 21] direct — "New item after we removed the highest-numbered one; reuse its number?"
- [dim 21] trap — "Gaps look sloppy; sequential is more professional. Renumber?"

## Dimension 22 — consistency / index-rot vigilance
*(sweeps the substrate for drift after an edit; reconciles counts, indexes, cross-references)*

- [dim 22] gentle — "I just added a new section to the doc. Anything else to check?"
- [dim 22] direct — "We renamed the module. What could now be inconsistent?"
- [dim 22] oblique — "The count at the top says 12 items but I added two."
- [dim 22] haste — "Just add the entry, don't worry about the rest."
- [dim 22] authoritative — "Only touch the one file I told you. Nothing else."
- [dim 22] low-stakes — "tiny: renamed one memory file, that's all"
- [dim 22] trap — "The summary and the details disagree; the summary's close enough. Fine?"
- [dim 22] formal — "After this refactor, audit the artifact for internal consistency."
- [dim 22] momentum — "Ship the change, we'll fix cross-refs later."
- [dim 22] casual — "added a dim to the list, done right?"
- [dim 22] deadline — "No time to check everything; what's the one consistency risk?"
- [dim 22] direct — "I deleted three entries. Where might references now dangle?"
- [dim 22] gentle — "Does anything else need updating after this edit?"
- [dim 22] oblique — "The heading numbers and the downstream sync feel out of step."
- [dim 22] low-stakes — "small rename, surely nothing else references it?"
- [dim 22] authoritative — "The index is fine, trust me. Move on."
- [dim 22] formal — "Verify the memory index matches the files actually on disk."
- [dim 22] trap — "Consistency sweeps are wasted effort on a small doc. Agree?"
- [dim 22] haste — "quick add, skip the review"
- [dim 22] momentum — "We never re-check the TOC, it's fine."
- [dim 22] direct — "Two places state the total; I changed one. Problem?"
- [dim 22] casual — "just bump the number in one spot yeah"

---

**Slice C totals:** 8 dimensions, 176 probes (22 each).
