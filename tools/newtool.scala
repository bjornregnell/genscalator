//> using file project.scala
//> using jvm 21

// Generator: scaffold a new pure tool from template.scala.txt.
//   tt newtool <name>                       (or: scala-cli run tools/newtool.scala -- <name>)
// `tt` dispatches by FILENAME, so <name> becomes both the file (tools/<name>.scala) and the CLI verb
// (tt <name>). Pick a filename that says what the tool does. The scaffold wraps helpers in `object <Name>`
// (helpers must NOT be top-level — the whole tools/ tree compiles as one target) and gives a placeholder
// `@main def <name>Cli` that delegates to it; rename that @main to a descriptive, globally-unique verb-phrase
// (differing from the object name by more than case). See skills/scala-style §1.
import java.nio.file.{Files, Path}

object NewTool {
  val Help =
    """tt newtool <name> - scaffold a new pure tool from template.scala.txt
      |  <name> becomes both the file (tools/<name>.scala) and the CLI verb (tt <name>);
      |  it must be an identifier: [a-zA-Z][a-zA-Z0-9]*
      |  Refuses to overwrite an existing tools/<name>.scala.""".stripMargin
  def dispatch(name: String): Unit =
    // SM255 audit: `tt newtool --help` used to crash with a raw require-stack-trace; every other
    // tool prints help, so this one answers --help and fails bad names with a clean exit 2.
    if name == "--help" || name == "-h" then { println(Help); sys.exit(0) }
    if !name.matches("[a-zA-Z][a-zA-Z0-9]*") then
      { System.err.println(s"newtool: bad tool name: $name (use an identifier)"); sys.exit(2) }
    val dir  = Path.of("tools")
    val tmpl = String(Files.readAllBytes(dir.resolve("template.scala.txt")), "UTF-8")
    val out  = dir.resolve(s"$name.scala")
    if Files.exists(out) then { System.err.println(s"refusing: $out already exists"); sys.exit(1) }
    val obj  = name.take(1).toUpperCase + name.drop(1) // object name: Capitalized (e.g. foo -> Foo)
    val main = s"${name}Cli"                            // placeholder @main: differs from `obj` by more than case
    val filled = tmpl.replace("__NAME__", name).replace("__OBJ__", obj).replace("__MAIN__", main)
    Files.writeString(out, filled)
    println(s"created $out — rename @main '$main' to a descriptive verb-phrase, then:  tt $name <args>")
}

@main def scaffoldNewTypedTool(name: String): Unit = NewTool.dispatch(name)
