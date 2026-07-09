//> using scala 3.10.0-RC1-bin-20260707-a4dab1a-NIGHTLY
// PoC 4 — SAFE MODE closes the escape hatches (EXPECTED COMPILE FAILURE).
// `import language.experimental.safe` (paper Sec 3.5) extends capture checking
// into a capability-safe SUBSET: no unchecked casts / pattern matches, no
// `caps.unsafe`, no `@unchecked`, no runtime reflection, capture checking +
// explicit nulls + mutation tracking on, and global objects usable only if
// themselves safe. Without these, adversarial code could FORGET or FORGE a
// capability and defeat the guarantees in poc2/poc3.
//
// Confirmed on nightly 3.10.0-RC1: safe mode rejects even `println` (global
// print is tagged @rejectSafe). Below we also probe an unchecked cast, the
// classic capability-forging move.
import language.experimental.safe

class Injector extends caps.SharedCapability

// Attempt to conjure the inject capability from nothing via a cast.
// Under safe mode the escape hatch is closed, so a hostile agent cannot
// fabricate a capability it was never granted.
def forge(x: Any): Injector = x.asInstanceOf[Injector]
