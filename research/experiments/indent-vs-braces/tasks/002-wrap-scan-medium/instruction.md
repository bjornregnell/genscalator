Edit `scan` so the character transformation only happens when `upper` is true.

Inside the `while` loop, wrap the existing character dispatch — the whole
`if c == 'a' … else sb += c` chain **and** the `i += 1` that follows it — so it runs
only when `upper` is true. When `upper` is false, append the character unchanged and
advance instead (`sb += c` then `i += 1`).

Behavior after the edit:
- `upper == true`  → 'a'→"A", 'b'→"B", 'c'→"C", 'd'→"D", 'e'→"E", any other char unchanged.
- `upper == false` → every character appended unchanged.

Keep the code in the SAME style as the file you were given.
