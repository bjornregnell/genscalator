//> using scala 3.8.4
//> using dep com.lihaoyi::requests:0.9.0
//> using dep com.lihaoyi::ujson:4.0.2
//> using dep com.lihaoyi::os-lib:0.11.8

// modly-pilot — one-shot probe of the modly local-model axis: set a model, send task 001's braceless
// before+instruction, print the raw completion so we can see whether a local model returns gradable code.
//   scala-cli run modly-pilot.scala -- <model>
val Modly = sys.env.getOrElse("MODLY_URL", "http://bjornyx.local:8080")  // replicator: export MODLY_URL to override

@main def pilot(model: String): Unit =
  val dir = os.Path("/home/bjornr/git/berg/bjornregnell/genscalator/research/experiments/indent-vs-braces/tasks/001-wrap-dispatch-in-else")
  val before = os.read(dir / "before.braceless.scala")
  val instruction = os.read(dir / "instruction.md")
  val prompt =
    s"""You are editing Scala 3 code. Return ONLY the complete edited file inside one ```scala code block.
       |Keep significant-indentation (braceless) style — no optional braces.
       |
       |TASK:
       |$instruction
       |
       |FILE:
       |```scala
       |$before```
       |""".stripMargin
  requests.post(s"$Modly/set-model", data = ujson.write(ujson.Obj("model" -> model, "temperature" -> 0.4, "seed" -> 1)),
    headers = Map("Content-Type" -> "application/json"), readTimeout = 120000, connectTimeout = 5000)
  val r = requests.post(s"$Modly/generate", data = ujson.write(ujson.Obj("prompt" -> prompt, "temperature" -> 0.4, "seed" -> 1)),
    headers = Map("Content-Type" -> "application/json"), readTimeout = 180000, connectTimeout = 5000)
  val text = try ujson.read(r.text())("response").str catch case _: Throwable => r.text()
  println(s"===== $model raw completion =====")
  println(text)
