//> using scala 3.8.4
//> using dep com.lihaoyi::requests:0.9.0
//> using dep com.lihaoyi::ujson:4.0.2
//> using dep com.lihaoyi::os-lib:0.11.8

// sweep — the indent-vs-braces experiment runner (local-model axis via modly).
// For each (task × regime × model × run): prompt the model to do the edit in the target regime, extract the
// code, grade it (compile + behavioral probe), append a row to results-raw.tsv. Fully autonomous: pure
// scala-cli (allowlisted); every model call is HTTP to modly from inside this program (no gated shell cmd).
//   scala-cli run sweep.scala -- [R=6] [model,model,...]
import scala.util.Try

val Root  = os.Path("/home/bjornr/git/berg/bjornregnell/genscalator/research/experiments/indent-vs-braces")
val Modly = "http://bjornyx.local:8080"
val Regimes = List("braceless", "braces", "common")
val DefaultModels = List("qwen2.5:3b", "qwen2.5:7b", "qwen2.5-coder:7b", "gemma2:9b")
val Temp = 0.4

val regimeDirective = Map(
  "braceless" -> "Keep significant-indentation (braceless) Scala 3 style — no optional braces, no `end` markers.",
  "braces"    -> "Use braces on every multi-line block (braces-everywhere style).",
  "common"    -> "Use common style: braces around long scopes (those containing blank lines), braceless for short scopes with the closing keyword (else/do/case/catch) as the delimiter.")

/** Extract the first fenced code block; if none, use the whole completion (will likely FAIL_COMPILE — fair). */
def extractCode(completion: String): String =
  val i = completion.indexOf("```")
  if i < 0 then completion.trim
  else
    val afterFence = completion.substring(i + 3).dropWhile(_ != '\n').drop(1) // drop optional lang tag line
    val end = afterFence.indexOf("```")
    (if end < 0 then afterFence else afterFence.substring(0, end)).trim

def setModel(model: String, seed: Int): Unit =
  Try(requests.post(s"$Modly/set-model",
    data = ujson.write(ujson.Obj("model" -> model, "temperature" -> Temp, "seed" -> seed)),
    headers = Map("Content-Type" -> "application/json"), readTimeout = 120000, connectTimeout = 5000))

def generate(prompt: String, seed: Int): Option[String] =
  Try {
    val r = requests.post(s"$Modly/generate",
      data = ujson.write(ujson.Obj("prompt" -> prompt, "temperature" -> Temp, "seed" -> seed)),
      headers = Map("Content-Type" -> "application/json"), readTimeout = 180000, connectTimeout = 5000)
    try ujson.read(r.text())("response").str catch case _: Throwable => r.text()
  }.toOption

/** Compile [candidate, probe] via scala-cli, run, compare stdout to expected. */
def grade(candidate: os.Path, probe: os.Path, expected: String): String =
  Try(os.proc("scala-cli", "run", candidate.toString, probe.toString)
    .call(check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 120000)) match
    case scala.util.Success(r) if r.exitCode == 0 => if r.out.text().trim == expected then "PASS" else "FAIL_MISSCOPE"
    case scala.util.Success(_) => "FAIL_COMPILE"
    case scala.util.Failure(_) => "FAIL_TIMEOUT"

def diffLines(a: os.Path, b: os.Path): Int =
  Try(os.proc("diff", a.toString, b.toString).call(check = false, stdout = os.Pipe).out.text()
    .linesIterator.count(l => l.startsWith("<") || l.startsWith(">"))).getOrElse(-1)

def buildPrompt(before: String, instruction: String, regime: String): String =
  s"""You are editing Scala 3 code. Return ONLY the complete edited file inside one ```scala code block.
     |${regimeDirective(regime)}
     |
     |TASK:
     |$instruction
     |
     |FILE:
     |```scala
     |$before```
     |""".stripMargin

@main def sweep(args: String*): Unit =
  val R = args.headOption.flatMap(_.toIntOption).getOrElse(6)
  val models = args.drop(1).headOption.map(_.split(",").toList).getOrElse(DefaultModels)
  val tasks = os.list(Root / "tasks").filter(os.isDir).sorted
  val out = Root / "results-raw.tsv"
  if !os.exists(out) then os.write(out, "task\tregime\tmodel\trun\tgraded\tout_tokens\tdiff_lines\n")
  val tmp = os.temp.dir(prefix = "ivb-sweep")
  var n = 0
  for
    task <- tasks
    regime <- Regimes
    beforeF = task / s"before.$regime.scala"
    if os.exists(beforeF) && os.exists(task / "probe.scala") && os.exists(task / "expected.txt")
    model <- models
    run <- 1 to R
  do
    n += 1
    val res = Try {
      val before = os.read(beforeF)
      val instruction = os.read(task / "instruction.md")
      val expected = os.read(task / "expected.txt").trim
      setModel(model, run)
      generate(buildPrompt(before, instruction, regime), run) match
        case None => ("FAIL_NORESP", 0, -1)
        case Some(completion) =>
          val cand = tmp / s"c_${task.last}_${regime}_${model.replaceAll("[^a-zA-Z0-9]", "_")}_$run.scala"
          os.write.over(cand, extractCode(completion))
          (grade(cand, task / "probe.scala", expected), completion.length / 4, diffLines(beforeF, cand))
    }.getOrElse(("FAIL_ERROR", 0, -1))
    os.write.append(out, s"${task.last}\t$regime\t$model\t$run\t${res._1}\t${res._2}\t${res._3}\n")
    println(s"[$n] ${task.last} $regime $model run$run -> ${res._1} (out~${res._2} diff=${res._3})")
  println(s"done: $n cells -> $out")
