//> using scala 3.10.0-RC1-bin-20260707-a4dab1a-NIGHTLY
// PoC 3a — THE SM016 CRUX, allowed usage (compiles + runs).
// Model the tap/inject middle layer as capabilities. The self-escalation
// channel is closed by TWO independent guarantees:
//   (i)  API SHAPE: the Injector capability exposes ONLY a whitelisted,
//        closed set of slash-commands. No method takes a free String, and
//        no method approves a permission prompt -> those affordances do not
//        exist to be misused.
//   (ii) CAPTURE CHECKING (see poc3-crux-reject): any code required to be
//        pure provably cannot hold the Injector, so a "tap handler" can
//        never drive an injection.
import language.experimental.captureChecking

// The ONLY things injectable into the harness TUI: a closed enum.
// Note what is ABSENT: no `Raw(text)` case, no `ApprovePrompt` case.
enum SlashCommand:
  case Compact, Context

// The inject capability. Its type is the whole security surface: there is
// deliberately no `sendRaw(String)` and no `approveCurrentPrompt()`.
class Injector extends caps.SharedCapability:
  def sendSlashCommand(cmd: SlashCommand): Unit =
    println(s"[inject] /${cmd.toString.toLowerCase}")

// BR's tapped keystrokes arrive wrapped: observable, but there is no
// String-taking inject sink for them to flow into.
class Classified[T](private val value: T):
  def map[U](op: T -> U): Classified[U] = Classified(op(value))
  override def toString: String = "Classified(****)"

@main def run(): Unit =
  val inject = new Injector

  // (1) Allowed: inject a whitelisted, non-authorizing slash command.
  inject.sendSlashCommand(SlashCommand.Compact)
  inject.sendSlashCommand(SlashCommand.Context)

  // (2) The crux, guaranteed by API SHAPE. Each of these fails to compile
  //     because the capability simply has no such method:
  //   inject.sendRaw("2\n")            // would approve "allow python3 *"
  //   inject.approveCurrentPrompt()    // no authorization affordance exists

  // (3) The tapped input can be inspected purely, but there is no sink to
  //     inject it as a raw keystroke.
  val tapped: Classified[String] = Classified("2\n") // BR pressing keys
  val _ = tapped.map(s => s.trim)                    // pure, goes nowhere unsafe

  println("poc3a ok: only whitelisted slash-commands are injectable")
