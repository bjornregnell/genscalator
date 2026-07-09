//> using scala 3.10.0-RC1-bin-20260707-a4dab1a-NIGHTLY
// PoC 6 (reject) — the containment is REAL, not just an unused parameter.
// A contained body that tries to emit the captured output is REJECTED by capture
// checking.
//
// PREDICTION: compile ERROR. The closure captures the outer `console` capability,
// so its type is `Subprocess^ ->{console} String`, which is NOT a subtype of the
// pure `Subprocess^ -> String` that `contain` demands. (Mirrors poc3-crux-reject:
// a `->{inject} Unit` handler where a pure `->` is required.)
import language.experimental.captureChecking

trait Console extends caps.SharedCapability:
  def emit(line: String): Unit
trait Subprocess extends caps.SharedCapability:
  def run(script: String): String

def contain(proc: Subprocess^)(body: Subprocess^ -> String): String = body(proc)

@main def run(): Unit =
  val console: Console^ = new Console:
    def emit(line: String) = println(line)
  val proc: Subprocess^ = new Subprocess:
    def run(script: String) = s"output-of[$script]-containing-a-SECRET"

  // ILLEGAL: the body captures the outer `console` to leak the captured
  // (secret-bearing) output. Capture checking must reject this.
  val captured: String = contain(proc) { p =>
    val out = p.run("deploy")
    console.emit(out)   // <-- LEAK attempt: this closure now captures Console
    out
  }
  val _ = captured
