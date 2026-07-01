//> using scala 3.8.4
//> using jvm 21

// Generator: scaffold a new pure tool from template.scala.txt.
//   tt newtool <name>                       (or: scala-cli run tools/newtool.scala -- <name>)
// Note: `tt` dispatches by FILENAME, so <name> becomes both the file (tools/<name>.scala) and the CLI verb
// (tt <name>). Pick a filename that says what the tool does. The generated `@main` is named after <name>
// too; if that would clash with an import or be generic, rename the `@main` to a descriptive, globally-unique
// verb-phrase (the whole tools/ tree compiles as one target — see skills/scala-style §1).
import java.nio.file.{Files, Path}

@main def scaffoldNewTypedTool(name: String): Unit =
  require(name.matches("[a-zA-Z][a-zA-Z0-9]*"), s"bad tool name: $name (use an identifier)")
  val dir  = Path.of("tools")
  val tmpl = String(Files.readAllBytes(dir.resolve("template.scala.txt")), "UTF-8")
  val out  = dir.resolve(s"$name.scala")
  if Files.exists(out) then { System.err.println(s"refusing: $out already exists"); sys.exit(1) }
  Files.writeString(out, tmpl.replace("__NAME__", name))
  println(s"created $out — edit it, then run:  scala-cli run $out -- <args>")
