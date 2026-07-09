//> using scala 3.10.0-RC1-bin-20260707-a4dab1a-NIGHTLY
// PoC 1 — capture-checking basics. Iterating to find the CURRENT idiom.
// Trial log (learn-by-doing, per BR):
//   v1: `class Logger extends caps.Capability` -> ERROR: Capability is a
//       sealed trait, cannot extend from user code.
//   v2 (this): plain class + the `^` tracked marker on the parameter type.
//       Question: does `Logger^` track without extending anything?
import language.experimental.captureChecking

// A plain class that owns an effect.
class Logger:
  def log(msg: String): Unit = println(s"[log] $msg")

// `Logger^` = "a Logger capturing the root capability cap". Does the
// compiler accept `^` on an ordinary class type here?
def greet(log: Logger^): Unit =
  log.log("hello from a tracked capability")

@main def run(): Unit =
  val logger = new Logger
  greet(logger)
  println("poc1 ok")
