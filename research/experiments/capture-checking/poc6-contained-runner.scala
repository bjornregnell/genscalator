//> using scala 3.10.0-RC1-bin-20260707-a4dab1a-NIGHTLY
// PoC 6 — a CC-PROVEN contained subprocess runner (SM033).
// Takes the shipped `deployblog.sc` `runContained` pattern (own a credential-
// bearing subprocess, CAPTURE its output locally, emit only a self-synthesized
// secret-free summary) and makes the containment COMPILE-TIME PROVEN with capture
// checking: the runner scope provably does NOT hold the console capability, so it
// cannot leak the captured output to the terminal.
//
// The enabling move (agent/BR reasoning, PB SM033): CAPTURING (not inheriting) the
// output pulls the secret bytes INTO the typed world where CC can govern who may
// emit them. "A def that never prints" == "a def that does not capture the console
// capability" — and CC checks exactly that (a thin `->` arrow = empty capture set).
import language.experimental.captureChecking

// Capability-typed facades over the (non-cap-typed) Java IO.
trait Console extends caps.SharedCapability:
  def emit(line: String): Unit
trait Subprocess extends caps.SharedCapability:
  /** Run a credential-bearing subprocess and CAPTURE its merged output into a
    * local String (never inherits the parent's stdout). */
  def run(script: String): String

// `contain` runs `body` with the subprocess capability. `body` has the PURE
// function type `Subprocess^ -> String` (thin arrow, empty capture set), so it may
// not capture ANY outer capability — in particular not Console. That is the
// compile-time containment: whatever `body` does with the captured output, it
// cannot emit it, because it does not hold the console capability.
def contain(proc: Subprocess^)(body: Subprocess^ -> String): String = body(proc)

@main def run(): Unit =
  val console: Console^ = new Console:
    def emit(line: String) = println(line)
  val proc: Subprocess^ = new Subprocess:
    def run(script: String) = s"output-of[$script]-containing-a-SECRET"

  // Contained runner: the body captures ONLY its subprocess param `p` (pure wrt
  // Console), so it type-checks; it returns the captured output to the caller.
  val captured: String = contain(proc) { p => p.run("deploy") }

  // The TRUSTED caller holds Console and emits only its OWN synthesized, secret-
  // free summary — never the raw `captured` (which still bears the credential).
  console.emit("deployblog: done (1 file, 0 errors)")

  // `captured` is in scope and the caller could still print it: CC has proven the
  // RUNNER cannot leak, not that the String is unprintable. Sealing the return
  // value against the caller too is the airtight variant (poc7, CC + Secret).
  val _ = captured
  console.emit("poc6 ok")
