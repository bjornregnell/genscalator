# SM079 prototype: agent board (canonical reqT-lang store)

* Term: band has
  * Spec: Attention band encoded as the int attribute Prio. 1 = red (needs BR now), 2 = yellow (watch), 3 = green (rolling fine).
* Term: status has
  * Spec: Lifecycle encoded as a Comment of the form status=X where X is one of open, in-review, investigate, done.

* Feature: sm077 has
  * Gist: gs warm + gs init built
  * Comment: status=done
  * Prio: 3
* Feature: sm084 has
  * Gist: filter-viewer PRD drafted for review
  * Comment: status=in-review
  * Prio: 3
* Feature: sm085 has
  * Gist: terminology: minion + rename delta/D
  * Comment: status=open
  * Prio: 2
* Feature: sm086 has
  * Gist: auto-derived warming mode
  * Comment: status=open
  * Prio: 2
* Feature: sm088 has
  * Gist: bing-bing on approval stall (BR's hand)
  * Comment: status=open
  * Prio: 1
* Feature: sm090 has
  * Gist: gs <lang> language-scoped fleet
  * Comment: status=investigate
  * Prio: 2

* Feature: sm090 requires Feature: sm085
* Feature: sm086 requires Feature: sm077
