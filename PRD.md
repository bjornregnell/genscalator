# Product Requirements Document (PRD) for genscalator

This document includes requirements for the genscalator product including all typed tools (TT or tt) and other agent stuff (what word to use here?) such asskills, plugins, and other things (often in markdown or Scala code) that help **escalate human-agent code generation to the next level**.

*TAP:* To Agent Plan: refactor this whole repo so that things that belong here are moved here

## META

This document includes specs in reqT-lang, a markdown-subset for expressing requirements using meta-level concepts of ENT (requirement entities), REL (requirements relations), ATTR (requirements attributes). 

**Abstract example** of concrete syntax with abstract terms:

* ENT: id
* ENT: id REL
  * ENT: id
  * ATTR: text
  * ENT: id

**Concrete example:**

* Comment: A string attribute with informative text.
* Feature: yyy has Prio: 42
* Feature: yyy requires Feature: xxx
* Feature: xxx has 
  * Prio: 12
  * Spec: A longer textual specification
      that is spanning several lines. A longer textual specification
      that is spanning several lines.
  * UseCase: zzz has
    * Prio: 23

**Convention:**

* **abbreviations** reqt and reqts is short for just requirement(s) and reqT-lang the language (and [reqT](https://reqt.github.io/) is a desktop tool not used here (yet), we stick with the languag in md files for now)
* identifiers (id) are camelCase
* We only use this subset of reqT-lang (may be extended by agnet or human as soon as we see fit):
  - ENT: Goal, Feature, Function, Stakeholder, ...
  - REL: has, requires, ...
  - ATTR: Spec, ...

*TAP:* To Agent Plan: investigate what more entities, relations, attributes agent thinks we need from the reqT-lang meta model

**reqT-lang language specification:**

* In prose: https://github.com/reqT/reqT-lang/blob/main/docs/langSpec-GENERATED.md

* In code:
  - Class hierarchy for abstract syntax tree (generated): https://github.com/reqT/reqT-lang/blob/main/src/main/scala/03-model-GENERATED.scala
  - Informal semantics (see strings): https://github.com/reqT/reqT-lang/blob/main/src/main/scala/02-meta-model.scala
  - Syntax: https://github.com/reqT/reqT-lang/blob/main/src/main/scala/05-MarkdownParser.scala


## General goals (stable over time)

* Goal: tokenEfficiency has
  * Spec: TODO
* Goal: safeGeneration has
  * Spec: TODO
* Goal: jointHumanAgentProductivity has
  * Spec: TODO

## FUTURE

### Roadmap

### Release v0.1.0

(this will be the first release, all whats-to-come-in-this-release genscalator reqts in reqT-lang go here)

## PAST 

Here are requirements that are either implemented or cancelled. Move requirements from FUTURE to PAST as the move on.

### IMPLEMENTED

### Release v0.0.1

(there will be no v0.0.1 this is just to show how headings are use)

### CANCELLED

