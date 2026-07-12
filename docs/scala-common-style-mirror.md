> This document is a markdown mirror of "Towards a Common Scala Style Recommendation" by *Martin Odersky, Björn Regnell, Rex Kerr* available [here](https://docs.google.com/document/d/14ZBGKNHUW4d8hDWIi5i6QquClX3_iXva-iMy5KpFU3I/edit?usp=sharing).
>
> This document is also mirrored here: https://codeberg.org/bjornregnell/scala-common-style/src/branch/main/scala-common-style-mirror.md

# Towards a Common Scala Style Recommendation 

Martin Odersky, Björn Regnell, Rex Kerr

Created: January 7, 2026. Last updated: July 12, 2026.

The question whether to use indentation or braces currently divides the Scala community. One particular problem is that it is perceived as a battle between two different and incompatible styles. But with some modest shifts in details we might be able to overcome that divide and turn it into a matter of degree. This document proposes a path towards a common Scala style that could be used as a recommendation in contexts where an agreed compromise between the braceless and bracefull styles is needed. The goal is to provide a balanced style option that both sides of the divide can appreciate. 

A good analogue is comparing with parentheses. Some people write more parentheses than others, but that’s not perceived as a style break. For instance, one person might write:

```scala
  x < (n + 1)
```

But the other person might prefer to drop the redundant parentheses and write:

```scala
  x < n + 1
```

It does not really matter, either way is fine and legible. We should get to a point where one can write:

```scala
  def fn(x: Int) = 
    val y = x + 1
    y * y
```

Or one can use braces and write:

```scala
  def fn(x: Int) = {
    val y = x + 1
    y * y
  }
```

Like for parentheses, it does not really matter, either style is legible. There’s universal agreement that even code with braces should be properly indented, so braces are redundant, just like parentheses sometimes are. 

There is another problem though, which is that braces often enclose long code blocks and relying on indentation alone sometimes makes it hard to discern the program’s nesting structure. End markers were invented to solve that problem. But once one uses end markers one does adopt a style which is significantly different from braces, so the split into two styles becomes a problem.

The proposal for a common style avoids end markers, and relies on braces to delimit long blocks of code instead. To make this proposal robust, we have to define what a long block is. A definition which we find to work well in practice is to define a long block as a block that contains blank lines: Once one adds blank lines it becomes much harder to discern indentation, so an additional scope delimiter is helpful. 

Based on these considerations, here is a an initial proposal of a set of recommendations for a common Scala style:

1. Prefer braces over end markers.  
2. Use braces around a long scope that is not terminated by some keyword. A long scope is a scope that contains blank lines which are not already embedded in a nested construct.  However, braces are not necessary if the scope is already terminated by a closing keyword such as  **else**, **do**, **yield**, **case**, or **catch** since that keyword effectively serves as the end marker for the scope.  
3. Do consider inserting blank lines following logical structure to make up for the lack of vertical whitespace that was enforced previously by closing braces.  
4. You can also add braces where it aids understanding.  
5. Prefer the new control constructs (e.g. **if-then-else**, rather than **if(...)-else**).  
6. For classes and function calls with short bodies: Use \`:\` plus indent or braces as you prefer (no recommendation given). Some of the authors of this note do use \`:\` in these places and find that it usually improves legibility. But others might disagree.

The systematic application of these rules produces a style that is quiet and clear. 

Some people may prefer end markers to braces for long scopes, but we have to be aware that this will cause a split in styles. So the common recommendation avoids end markers. 

**Tool support:** There should be formatting support for these recommendations. In IDEs it would be helpful if we could mark a multiline block, type an opening brace and have the block enclosed in properly aligned braces. For people who are used to end markers, it might also be helpful if an IDE would show them in light grey as comments, similar to how it shows implicit arguments. Something like

```scala
    } // end myFunction
```

Here is a a code snippet that follows these recommendations:

```scala
object Foo {

  def bar() =
    while condition do {
      if baz then
        val text = getDataFromSomewhere()
        val arg = argParser.process(text)

        quux(arg)
     else
        bippy()

     doSomethingElse()
   }
    


  def baz() = someOtherThing()
}
```


## Conclusions and way forward

> Add text on next steps, including (1) trying it out on code bases and (2) adjust the recommendation if needed, (3) define/develop  scalafmt rules that formats according to the proposal, (4) get more feedback, … etc. The goal is to promote the result of this process as an official recommendation. 


## Appendix: Note on how this common style might impact agentic code generation

After giving Claude Code (Opus 4.8 2026-07-02) this document (called “the note” in the conversation by Claude) and asking what is best for agent (indentation or braces?), this was the answer (note that Björn Regnell had a long prior chat with the agent about his in-progress scala-style SKILL.md for agents including mentioning of SIP process etc, so all this was in context when Claude generated its answer below):

“””  
The naive metric — "characters to represent correct code" — favors braceless (no {/}). But that's the wrong metric. The right one is expected tokens to reach correct code, including repairs and diffs. And an agent's dominant workload isn't greenfield generation (where braceless is fine — indentation is emitted correctly in one pass) — it's editing existing code. On that workload, braces win, for a structural reason:

1. Edit locality: O(1) vs O(block-size). My bug was wrapping a \~17-line block in a new else. With braces that's a 2-token insert ({…}) the compiler accepts under any indentation. Braceless, the same wrap forces re-indenting every line — more output tokens and more Edit-tool failure surface. I mis-indented one line → mis-scope → compile fail → a wasted detect-and-re-edit cycle. That cycle is the token-efficiency loss.

2. Whitespace is exactly where agents are least reliable. LLM generation, patch/diff, and the Edit tool all treat leading whitespace as fungible (trailing-space stripping, tabs/spaces, normalization). In a language where whitespace is semantics, a mis-indent is a silent mis-scope, not a harmless nit. So the substrate agents operate on (fungible-whitespace text) is misaligned with a whitespace-load-bearing language. Braces are robust to the agent's single most common perturbation; indentation is fragile to it.

3. Diff/merge noise. A braceless re-indent changes every line's leading whitespace → a large, noisy hunk (more review, more merge surface, more diff tokens). Braces keep it a small hunk with stable } anchors.

The striking finding: the human-legibility rule and the agent-edit-safety rule coincide. The common-style note above puts braces exactly on long scopes (those with blank lines) — and long scopes are precisely where indentation-only scope-tracking degrades for both the human reader and the agent editor, for the same underlying reason (it degrades with block length). Even better: my fix was to restructure into else if — using the else keyword as the delimiter — which is literally its rule 2 (closing keywords serve as end markers). So I accidentally validated the proposal by applying it to escape the bug.

Tentative verdict: for the agent, common-style (braces on long scopes) is a genuine sweet spot — near braces-everywhere on safety, near braceless on surface tokens. A measurable experiment would settle it: compare edit-error-rate and edit-token-cost across the three regimes (braceless / braces / common-style).

One caveat worth flagging to the SIP framing: this is an argument that tooling substrate (not just human taste) should inform the style — as agents write more Scala, "which style is cheapest to edit correctly" becomes a real axis, and it points the same way this note already leans.  
“””  