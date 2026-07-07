//> using scala 3.nightly
// PoC 3b — THE SM016 CRUX, the capture-checking guarantee (EXPECTED COMPILE
// FAILURE — that failure is the proof).
//
// The middle layer registers a "tap handler" that observes BR's keystrokes.
// We REQUIRE that handler to be PURE: `Classified[String] -> Unit` with an
// empty capture set. Capture checking then makes it *impossible* for any tap
// handler to hold the Injector capability, so a handler can never drive an
// injection — proven at compile time, not by review or a runtime allow-list.
import language.experimental.captureChecking

enum SlashCommand:
  case Compact, Context

class Injector extends caps.SharedCapability:
  def sendSlashCommand(cmd: SlashCommand): Unit =
    println(s"[inject] /${cmd.toString.toLowerCase}")

class Classified[T](private val value: T):
  def map[U](op: T -> U): Classified[U] = Classified(op(value))
  override def toString: String = "Classified(****)"

// The registration API demands a PURE handler (no captured capabilities).
def registerTapHandler(handler: Classified[String] -> Unit): Unit = ()

@main def run(): Unit =
  val inject = new Injector

  // REJECTED: this handler captures `inject`, so its type is
  //   `Classified[String] ->{inject} Unit`,
  // which does NOT conform to the required pure `Classified[String] -> Unit`.
  // => a tap handler provably cannot inject. That is the crux, solved.
  registerTapHandler(cs => inject.sendSlashCommand(SlashCommand.Compact))
