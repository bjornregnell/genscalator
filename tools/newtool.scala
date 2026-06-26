//> using scala 3.8.4
//> using jvm 21

// Generator: scaffold a new pure tool from template.scala.txt. Run from the repo root:
//   scala-cli run tools/newtool.scala -- <name>
import java.nio.file.{Files, Path}

@main def newtool(name: String): Unit =
  require(name.matches("[a-zA-Z][a-zA-Z0-9]*"), s"bad tool name: $name (use an identifier)")
  val dir  = Path.of("tools")
  val tmpl = String(Files.readAllBytes(dir.resolve("template.scala.txt")), "UTF-8")
  val out  = dir.resolve(s"$name.scala")
  if Files.exists(out) then { System.err.println(s"refusing: $out already exists"); sys.exit(1) }
  Files.writeString(out, tmpl.replace("__NAME__", name))
  println(s"created $out — edit it, then run:  scala-cli run $out -- <args>")
