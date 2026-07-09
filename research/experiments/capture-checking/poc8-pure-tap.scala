//> using scala 3.10.0-RC1-bin-20260707-a4dab1a-NIGHTLY
// PoC 8 — the TAP side of SM016, wired to the injector idiom (SM017 v3; the poc5
// open item "a Classified-tapped input channel"). Proves the SEPARATION the SM016
// synthesis relies on: reading BR's keystrokes can OBSERVE but can never ACT.
//
// The tap handler is required PURE (thin `->` arrow): it may inspect the tapped
// input (Classified) but may NOT capture the Injector capability, so it physically
// cannot inject in response to what it reads. No tap -> inject laundering, by
// construction (mirrors poc2 local-purity + poc3 crux-reject).
import language.experimental.captureChecking

enum SlashCommand:
  case Compact, Context

// Tapped input is Classified: observable/mappable but not implicitly an actionable
// command (an IFC label; cf. poc7 Secret).
class Classified[A](private val value: A):
  def map[B](f: A => B): Classified[B] = Classified(f(value))

trait Injector extends caps.SharedCapability:
  def send(cmd: SlashCommand): Unit

// `requestTap` runs a PURE handler over the tapped input. The pure `->` forbids the
// handler from capturing ANY capability, in particular the Injector.
def requestTap(handler: Classified[String] -> Unit): Unit =
  handler(Classified("user is typing: /help me"))   // trusted runtime supplies the tap

@main def run(): Unit =
  // OK: a pure handler that only OBSERVES the tapped input (e.g. measures its length
  // for a fatigue/idle proxy) — it captures no capability.
  requestTap { c => val _ = c.map(_.length) }
  println("poc8 ok")
