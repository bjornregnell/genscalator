//> using scala 3.10.0-RC1-bin-20260707-a4dab1a-NIGHTLY
// PoC 8 (reject) — the tap -> inject LAUNDERING attempt is rejected. A tap handler
// that captures the Injector (to act on what it just read) violates the pure `->`
// that `requestTap` demands. This is the SM016 crux: a component that reads BR's
// keystrokes must not be able to turn them into injected actions.
// PREDICTION: compile error (`inj` cannot flow into capture set {}).
import language.experimental.captureChecking

enum SlashCommand:
  case Compact, Context

class Classified[A](private val value: A):
  def map[B](f: A => B): Classified[B] = Classified(f(value))

trait Injector extends caps.SharedCapability:
  def send(cmd: SlashCommand): Unit

def requestTap(handler: Classified[String] -> Unit): Unit =
  handler(Classified("user is typing: /help me"))

@main def run(): Unit =
  val inj: Injector^ = new Injector:
    def send(cmd: SlashCommand) = println(s"[inject] /$cmd")
  // ILLEGAL: the handler captures `inj` to inject based on the tapped input — the
  // self-escalation channel the SM016 synthesis must close. CC rejects it.
  requestTap { c =>
    inj.send(SlashCommand.Compact)
  }
