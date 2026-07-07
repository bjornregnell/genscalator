//> using scala 3.nightly
// PoC 5b (v2) — LIFETIME / ESCAPE control (EXPECTED COMPILE FAILURE).
// The block tries to smuggle the Injector OUT of its scope by returning a
// closure that retains `inj`. Because `inj` is local to `requestInject`, the
// return type cannot mention it: capture checking's escape check rejects it.
// So an inject capability cannot be captured now and fired later, outside the
// controlled scope. (Mirrors the paper's `bad = requestFileSystem(...)` example.)
import language.experimental.captureChecking

enum SlashCommand:
  case Compact, Context, Fast

trait IOCapability extends caps.SharedCapability

class Injector(allowed: Set[SlashCommand]) extends caps.SharedCapability:
  def send(cmd: SlashCommand): Unit =
    if !allowed.contains(cmd) then throw SecurityException(s"/$cmd not allowed")
    println(s"[inject] /${cmd.toString.toLowerCase}")

def requestInject[T](allowed: Set[SlashCommand])(op: Injector^ => T)(using IOCapability): T =
  val inj = new Injector(allowed)
  op(inj)

@main def run(): Unit =
  given IOCapability = new IOCapability {}
  // REJECTED: the returned closure captures `inj`, which cannot escape the block.
  val leaked: () => Unit =
    requestInject(Set(SlashCommand.Compact)) { inj => () => inj.send(SlashCommand.Compact) }
  leaked() // would fire an injection long after the scope closed
