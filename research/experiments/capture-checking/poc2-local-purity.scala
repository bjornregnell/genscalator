//> using scala 3.nightly
// PoC 2 — LOCAL PURITY, the paper's core leak-prevention mechanism (Sec 2/3.3).
// Goal: prove the compiler REJECTS a classified-data leak by construction.
// A `Classified[T].map` accepts ONLY a pure function `T -> U` (empty capture
// set), so a closure that captures a side-effecting capability cannot be passed.
//
// Trial log: testing whether `caps.SharedCapability` is the current extendable
// marker for a capability class (since `caps.Capability` is sealed).
import language.experimental.captureChecking

// A capability that performs a side effect (here: "leaking" via output).
class Leak extends caps.SharedCapability:
  def send(msg: String): Unit = println(s"[LEAKED] $msg")

// The paper's Classified wrapper. `map` takes a PURE function `T -> U`.
class Classified[T](private val value: T):
  def map[U](op: T -> U): Classified[U] = Classified(op(value))
  override def toString: String = "Classified(****)"

@main def run(): Unit =
  val secret = Classified("password123")

  // (A) OK — a pure transformation captures nothing.
  val ok = secret.map(s => s.toUpperCase)
  println(s"pure map ok: $ok")

  // (B) Should be REJECTED at COMPILE time — the closure captures `leak`,
  //     so its type is `String ->{leak} String`, which does NOT conform to
  //     the pure `String -> String` that `map` demands. This is the leak
  //     the type system forbids by construction.
  val leak = new Leak
  val bad = secret.map(s => { leak.send(s); s })
  println(s"should never get here: $bad")
