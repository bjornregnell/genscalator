//> using scala 3.nightly
// PoC 5 (v2) — a SCOPED injector harness: the SM016 kernel in pure Scala.
// Mirrors the paper's `requestFileSystem` lifetime pattern (Sec 2/3.6):
//   - `requestInject` creates an Injector capability, runs `op` with it; the
//     capability is meant to be invalid once the block returns (lifetime).
//   - a `using IOCapability` requirement makes `requestInject` IMPURE, so it
//     cannot be called inside a `Classified.map` / pure function (no opening
//     an inject scope to launder tapped data).
//   - the Injector enforces a runtime allow-list (like paths-outside-root ->
//     SecurityException), layered ON TOP of the compile-time API-shape guard.
import language.experimental.captureChecking

enum SlashCommand:
  case Compact, Context, Fast

// A capability the (trusted) runtime grants at the top level; gates the impure
// inject scope. In real TACIT the harness provides this, not agent code.
trait IOCapability extends caps.SharedCapability

class Injector(allowed: Set[SlashCommand]) extends caps.SharedCapability:
  def send(cmd: SlashCommand): Unit =
    if !allowed.contains(cmd) then
      throw SecurityException(s"/$cmd not in granted allow-list $allowed")
    println(s"[inject] /${cmd.toString.toLowerCase}")

def requestInject[T](allowed: Set[SlashCommand])(op: Injector^ => T)(using IOCapability): T =
  val inj = new Injector(allowed)
  op(inj)

@main def run(): Unit =
  given IOCapability = new IOCapability {}

  // (1) Allowed: inject whitelisted commands within the scope.
  requestInject(Set(SlashCommand.Compact, SlashCommand.Context)) { inj =>
    inj.send(SlashCommand.Compact)
    inj.send(SlashCommand.Context)
  }

  // (2) Runtime allow-list: a command outside the granted set is refused,
  //     even though it is a valid SlashCommand (defense in depth over (1)).
  try
    requestInject(Set(SlashCommand.Compact)) { inj => inj.send(SlashCommand.Fast) }
  catch case e: SecurityException => println(s"[refused] ${e.getMessage}")

  println("poc5 ok")
