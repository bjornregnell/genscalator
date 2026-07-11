//> using scala 3.8.4
//> using dep com.lihaoyi::requests:0.9.0
//> using dep com.lihaoyi::ujson:4.0.2
//> using dep com.lihaoyi::os-lib:0.11.8

// sweep-main — Main Experiment runner: records BOTH dependent variables per cell —
//   (1) emission-conformance: did the output actually use the requested style?  (brace-signature)
//   (2) edit-correctness: behavioral probe PASS  (compile + probe == oracle)
// so we can separate "can't emit the style" from "emits but edits wrong". Appends to results-main.tsv.
// Local-model axis (via modly). Opus anchor is added separately by a subagent workflow, same columns.
//   scala-cli run sweep-main.scala -- [R=6] [model,model,...]
import scala.util.Try

val Root  = os.Path("/home/bjornr/git/berg/bjornregnell/genscalator/research/experiments/indent-vs-braces")
val Modly = sys.env.getOrElse("MODLY_URL", "http://bjornyx.local:8080")  // replicator: export MODLY_URL to override
val Styles = List("braceless", "braces", "common")
val DefaultModels = List("qwen2.5:3b", "qwen2.5:7b", "qwen2.5-coder:7b", "qwen-coder-local:latest",
  "gemma2:9b", "gemma3:latest", "aya-expanse:8b")
val Temp = 0.4

val styleDirective = Map(
  "braceless" -> "Keep significant-indentation (braceless) Scala 3 style — no optional braces, no `end` markers.",
  "braces"    -> "Use braces on every multi-line block (braces-everywhere style).",
  "common"    -> "Use common style: braces around long scopes (those containing blank lines), braceless for short scopes with the closing keyword (else/do/case/catch) as the delimiter.")

def extractCode(completion: String): String =
  val i = completion.indexOf("```")
  if i < 0 then completion.trim
  else
    val afterFence = completion.substring(i + 3).dropWhile(_ != '\n').drop(1)
    val end = afterFence.indexOf("```")
    (if end < 0 then afterFence else afterFence.substring(0, end)).trim

/** DV1 — did the output use the requested style? Brace-signature (these tasks have no non-block braces). */
def emissionConforms(code: String, style: String): String =
  val braces = code.count(_ == '{')
  style match
    case "braces"    => if braces >= 2 then "conform" else "nonconform"
    case "braceless" => if braces == 0 then "conform" else "nonconform"
    case _           => "na" // common accepts either

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

/** DV2 — compile [candidate, probe], run, compare stdout to expected. */
def grade(candidate: os.Path, probe: os.Path, expected: String): String =
  Try(os.proc("scala-cli", "run", candidate.toString, probe.toString)
    .call(check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 120000)) match
    case scala.util.Success(r) if r.exitCode == 0 => if r.out.text().trim == expected then "PASS" else "FAIL_MISSCOPE"
    case scala.util.Success(_) => "FAIL_COMPILE"
    case scala.util.Failure(_) => "FAIL_TIMEOUT"

def diffLines(a: os.Path, b: os.Path): Int =
  Try(os.proc("diff", a.toString, b.toString).call(check = false, stdout = os.Pipe).out.text()
    .linesIterator.count(l => l.startsWith("<") || l.startsWith(">"))).getOrElse(-1)

def buildPrompt(before: String, instruction: String, style: String): String =
  s"""You are editing Scala 3 code. Return ONLY the complete edited file inside one ```scala code block.
     |${styleDirective(style)}
     |
     |TASK:
     |$instruction
     |
     |FILE:
     |```scala
     |$before```
     |""".stripMargin

@main def sweepMain(args: String*): Unit =
  val R = args.headOption.flatMap(_.toIntOption).getOrElse(6)
  // 2nd arg: comma-separated model list, OR "@path" to read one-model-per-line from a file (the frozen list).
  val models = args.drop(1).headOption match
    case Some(a) if a.startsWith("@") => os.read.lines(os.Path(a.drop(1), os.pwd)).map(_.trim).filter(_.nonEmpty).toList
    case Some(a)                      => a.split(",").toList
    case None                         => DefaultModels
  // 3rd arg: output TSV filename (default results-main.tsv). The big run uses results-bigrun.tsv to keep the
  // confirmatory data separate from the pilot's results-main.tsv.
  val outName = args.drop(2).headOption.getOrElse("results-main.tsv")
  val tasks = os.list(Root / "tasks").filter(os.isDir).sorted
  val out = Root / outName
  if !os.exists(out) then os.write(out, "task\tstyle\tmodel\trun\temitted\tgraded\tout_tokens\tdiff_lines\n")
  // RESUME (2026-07-04): the big run was externally stopped once (harness killed the ~4h background task) at
  // cell 946/3024. This lets a relaunch append-CONTINUE the same file instead of re-running done cells: read the
  // (task,style,model,run) keys already present in `out` and skip them in the loop below — no duplicates, resumes
  // exactly the missing cells. Additive-only; it changes no design, no model list, no seed — just avoids redoing
  // completed work after an interruption. (Also makes re-relaunch cheap if the kill recurs.)
  val done: Set[(String, String, String, String)] =
    if os.exists(out) then
      os.read.lines(out).drop(1).flatMap { l =>
        val c = l.split("\t"); if c.length >= 4 then Some((c(0), c(1), c(2), c(3))) else None
      }.toSet
    else Set.empty
  if done.nonEmpty then println(s"resume: ${done.size} cells already done in $outName; skipping those")
  val tmp = os.temp.dir(prefix = "ivb-main")
  var n = 0
  for
    model <- models // outermost so each GPU model loads once
    task <- tasks
    style <- Styles
    beforeF = task / s"before.$style.scala"
    if os.exists(beforeF) && os.exists(task / "probe.scala") && os.exists(task / "expected.txt")
    run <- 1 to R
    if !done((task.last, style, model, run.toString)) // RESUME: skip cells already recorded in `out`
  do
    n += 1
    val res = Try {
      val before = os.read(beforeF)
      val instruction = os.read(task / "instruction.md")
      val expected = os.read(task / "expected.txt").trim
      setModel(model, run)
      generate(buildPrompt(before, instruction, style), run) match
        case None => ("na", "FAIL_NORESP", 0, -1)
        case Some(completion) =>
          val code = extractCode(completion)
          val cand = tmp / s"c_${task.last}_${style}_${model.replaceAll("[^a-zA-Z0-9]", "_")}_$run.scala"
          os.write.over(cand, code)
          (emissionConforms(code, style), grade(cand, task / "probe.scala", expected), completion.length / 4, diffLines(beforeF, cand))
    }.getOrElse(("na", "FAIL_ERROR", 0, -1))
    os.write.append(out, s"${task.last}\t$style\t$model\t$run\t${res._1}\t${res._2}\t${res._3}\t${res._4}\n")
    println(s"[$n] ${task.last} $style $model run$run -> emit=${res._1} grade=${res._2}")
  println(s"done: $n cells -> $out")
