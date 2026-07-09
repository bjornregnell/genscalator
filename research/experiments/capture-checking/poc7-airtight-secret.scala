//> using scala 3.10.0-RC1-bin-20260707-a4dab1a-NIGHTLY
// PoC 7 — AIRTIGHT: CC (contain the print EFFECT) composed with a Secret wrapper
// (label the DATA that crosses the def boundary). poc6 proved the RUNNER cannot
// leak, but it returns a raw String the caller could still print. Here `contain`
// returns Secret[String]: extracting the raw value needs an `Unseal` capability
// distinct from Console, granted only at a trusted declassify site — so a pure /
// logging context (which lacks Unseal) cannot emit the captured output at all.
//
// The split is the point (PB SM033): CC governs the CAPABILITY (who may print);
// Secret governs the DATA SENSITIVITY (an IFC label). CC alone cannot see that a
// plain String is a secret; Secret alone cannot stop a scope from holding the
// console. Composed, they close both halves.
import language.experimental.captureChecking

trait Console extends caps.SharedCapability:
  def emit(line: String): Unit
trait Subprocess extends caps.SharedCapability:
  def run(script: String): String

// The declassify capability. Like poc5's IOCapability, the trusted runtime grants
// it only at a declassify site; a pure/logging context is simply not given one.
trait Unseal extends caps.SharedCapability

// An opaque secret box. The raw value is private; it stays sealed under `map`,
// and extracting it requires an Unseal capability in scope.
class Secret[A](private val value: A):
  def map[B](f: A => B): Secret[B] = Secret(f(value))   // transform, still sealed
  def unseal(using Unseal): A = value                    // declassify: needs the cap

// Contained AND sealed: the runner holds no Console (poc6 containment) and hands
// back a Secret, so the captured output crosses the boundary already labelled.
def contain(proc: Subprocess^)(body: Subprocess^ -> String): Secret[String] =
  Secret(body(proc))

@main def run(): Unit =
  val console: Console^ = new Console:
    def emit(line: String) = println(line)
  val proc: Subprocess^ = new Subprocess:
    def run(script: String) = s"output-of[$script]-containing-a-SECRET"

  val boxed: Secret[String] = contain(proc) { p => p.run("deploy") }

  // The caller emits only its OWN synthesized summary; the secret stays sealed.
  console.emit("deployblog: done (1 file)")

  // To read the captured output at all, an Unseal capability must be granted here
  // (a trusted declassify site). A pure/logging context lacking Unseal cannot.
  given Unseal = new Unseal {}
  val raw: String = boxed.unseal
  val _ = raw
  console.emit("poc7 ok")
