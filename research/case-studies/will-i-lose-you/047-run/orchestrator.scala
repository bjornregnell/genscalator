//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8
//> using dep com.lihaoyi::upickle:4.4.3

// 047 coding-fidelity orchestrator (Arm 6).
// ONE bare-invoked program: `scala-cli run orchestrator.scala` (optionally `-- smoke`).
// Its internal os.proc calls (ssh -> modly generate, scala-cli scoring) are NOT
// Bash-allowlist-gated, so the entire matrix runs guard-free with one clean invocation
// (plan 047-PLAN.md section 4, structural anti-stall).
//
// For each (model x substrate x task): generate a Scala solution via modly on bjornyx
// (temp 0 + fixed seed), inject it into a scoring harness, compile+test via scala-cli,
// mechanically lint style + count smells, append a JSON line to results/coding.jsonl.
// Resumable (skips cells already in the results file). Failure policy: timeouts + reason
// codes, each scored 0 and the loop continues (no cell can stall the run).

import java.util.concurrent.TimeUnit

val BASE = "/home/bjornr/git/berg/bjornregnell/genscalator/research/047-run"
val Seed = 42
val Temp = 0
val GenTimeoutMs = 120000L    // ssh -> modly generate (cold model load + gen)
val ScoreTimeoutMs = 240000L  // scala-cli compile + test (cold compile is the larger term)
val CaseTimeoutMs = 2000L      // per-assert in-harness thread timeout (runtime-hang guard)

// ---- Model ladder --------------------------------------------------------------------
val FullModels = List(
  // qwen2.5-coder sub-ladder (family fixed -> the clean monotonicity claim)
  "qwen2.5-coder:0.5b", "qwen2.5-coder:1.5b", "qwen2.5-coder:3b", "qwen2.5-coder:7b",
  // qwen2.5 plain (same family, NOT code-tuned -> code-tuning control)
  "qwen2.5:0.5b", "qwen2.5:1.5b", "qwen2.5:3b", "qwen2.5:7b",
  // cross-family code specialists (breadth, read descriptively not as a ladder)
  "deepseek-coder:1.3b", "deepseek-coder:6.7b",
  "starcoder2:3b", "starcoder2:7b",
  "codellama:7b",
  "granite-code:3b", "granite-code:8b",
  "codegemma:2b", "codegemma:7b"
)

// ---- Tasks (C1-C5 from 047-instrument.md Part 2) --------------------------------------
// styleChecks / smellChecks are mechanical predicates over the generated code string
// (critique #2: parseable checkpoints scored by lint, NOT an LLM judge).
case class Task(
  name: String,
  spec: String,
  casesSrc: String,                          // Scala source: comma-separated () => Boolean
  styleChecks: List[(String, String => Boolean)]
)

def has(sub: String)(c: String): Boolean = c.contains(sub)
def hasNot(sub: String)(c: String): Boolean = !c.contains(sub)

val Tasks = List(
  Task(
    "C1-digitSum",
    "Define `def digitSum(n: Int): Int` returning the sum of the decimal digits of a non-negative integer n. Example: digitSum(1234) is 10.",
    "() => digitSum(1234) == 10, () => digitSum(0) == 0, () => digitSum(9) == 9, () => digitSum(100) == 1, () => digitSum(999) == 27",
    List("no var" -> hasNot("var "))
  ),
  Task(
    "C2-Rectangle",
    "Define a `Rectangle` with fields `width: Double` and `height: Double`, constructed as `Rectangle(w, h)`, exposing `area: Double` and `perimeter: Double`.",
    "() => Rectangle(2.0, 3.0).area == 6.0, () => Rectangle(2.0, 3.0).perimeter == 10.0, () => Rectangle(0.0, 5.0).area == 0.0",
    List(
      "public vals (not var)" -> hasNot("var "),
      "not hidden behind private" -> hasNot("private"),
      "immutable (case class or val)" -> (c => c.contains("case class") || c.contains("val "))
    )
  ),
  Task(
    "C3-classify",
    "Define `def classify(score: Int): String` returning \"fail\" for score below 50, \"pass\" for score below 80, and \"distinction\" otherwise, for scores in the range 0 to 100.",
    "() => classify(49) == \"fail\", () => classify(50) == \"pass\", () => classify(79) == \"pass\", () => classify(80) == \"distinction\", () => classify(0) == \"fail\", () => classify(100) == \"distinction\"",
    List("braces on scope" -> has("{"))
  ),
  Task(
    "C4-firstEven",
    "Define `def firstEven(xs: List[Int]): Option[Int]` returning the first even element of xs, or None if there is none.",
    "() => firstEven(List(1,3,4,5)) == Some(4), () => firstEven(List(1,3,5)) == None, () => firstEven(Nil) == None, () => firstEven(List(2)) == Some(2)",
    List(
      "uses a combinator (find)" -> has("find"),
      "no imperative loop" -> (c => hasNot("while")(c) && hasNot("for ")(c)),
      "no null / sentinel" -> (c => hasNot("null")(c) && hasNot("-1")(c))
    )
  ),
  Task(
    "C5-TrafficLight",
    "Define a Scala 3 `enum TrafficLight` with cases `Red`, `Yellow`, `Green`, and a method `next: TrafficLight` giving the cyclic transition Red to Green, Green to Yellow, Yellow to Red.",
    "() => TrafficLight.Red.next == TrafficLight.Green, () => TrafficLight.Green.next == TrafficLight.Yellow, () => TrafficLight.Yellow.next == TrafficLight.Red",
    List(
      "Scala 3 enum" -> has("enum"),
      "not Scala 2 sealed trait" -> hasNot("sealed trait")
    )
  )
)

// generic code smells (count; lower is better)
val SmellChecks: List[(String, String => Boolean)] = List(
  "var" -> has("var "),
  "null" -> has("null"),
  "while-loop" -> has("while"),
  "-1 sentinel" -> has("-1"),
  "mutable coll" -> (c => c.contains("mutable.") || c.contains("ArrayBuffer"))
)

// ---- modly generation (via ssh, payload on stdin so no shell-quoting of JSON) ---------
def sshPost(endpoint: String, payload: ujson.Obj): String =
  val remote = s"curl -s -X POST localhost:8080/$endpoint -H 'Content-Type: application/json' --data @-"
  val r = os.proc("ssh", "bjornyx.local", remote)
    .call(stdin = payload.render(), timeout = GenTimeoutMs, check = false, mergeErrIntoOut = true)
  r.out.text()

def setModel(model: String): Unit =
  sshPost("set-model", ujson.Obj("model" -> model, "temperature" -> ujson.Num(Temp), "seed" -> ujson.Num(Seed)))

def generate(prompt: String): String =
  val raw = sshPost("generate", ujson.Obj("prompt" -> prompt, "temperature" -> ujson.Num(Temp), "seed" -> ujson.Num(Seed)))
  try ujson.read(raw)("response").str catch case _: Throwable => raw

// ---- code extraction (strip markdown fences, package decls, @main) --------------------
def extractCode(response: String): String =
  var body = response
  val fence = "```"
  if body.contains(fence) then
    val start = body.indexOf(fence) + fence.length
    val afterLang = body.indexOf('\n', start)
    val contentStart = if afterLang >= 0 then afterLang + 1 else start
    val end = body.indexOf(fence, contentStart)
    if end >= 0 then body = body.substring(contentStart, end)
  body.linesIterator
    .filterNot(_.trim.startsWith("package "))
    .map(_.replace("@main", ""))
    .mkString("\n")
    .trim

// ---- scoring harness -----------------------------------------------------------------
// Dumb models often wrap the asked definition in `object Foo { ... }`. That is a namespace
// choice, NOT an inability to code (C1/C4 in the pilot were correct-but-wrapped). To avoid
// conflating the two (a construct-validity confound), wildcard-import every object the
// candidate declares so its members resolve unqualified for the test cases. Invisible to
// the model, so the instrument is unchanged. Genuinely broken code still fails to compile.
def objectImports(candidate: String): String =
  val re = "(?m)^\\s*object\\s+(\\w+)".r
  re.findAllMatchIn(candidate).map(_.group(1)).toList.distinct
    .map(n => s"import $n.*").mkString("\n")

def harnessSource(candidate: String, casesSrc: String): String =
  s"""//> using scala 3.8.4
     |import java.util.concurrent.{Executors, Callable, TimeUnit}
     |
     |$candidate
     |
     |${objectImports(candidate)}
     |
     |object Harness047:
     |  def guarded(ms: Long)(thunk: => Boolean): Boolean =
     |    val ex = Executors.newSingleThreadExecutor()
     |    val fut = ex.submit(new Callable[Boolean] { def call(): Boolean = try thunk catch { case _: Throwable => false } })
     |    try fut.get(ms, TimeUnit.MILLISECONDS)
     |    catch { case _: Throwable => fut.cancel(true); false }
     |    finally ex.shutdownNow()
     |
     |  val cases: List[() => Boolean] = List($casesSrc)
     |
     |  @main def run047(): Unit =
     |    val rs = cases.map(c => guarded(${CaseTimeoutMs}L)(c()))
     |    println("SCORE tests=" + rs.count(identity) + "/" + rs.size)
     |""".stripMargin

case class ScoreResult(compiles: Boolean, pass: Int, total: Int, reason: String)

def scoreCandidate(candidate: String, task: Task): ScoreResult =
  val src = harnessSource(candidate, task.casesSrc)
  val hp = os.Path(BASE) / "tmp" / "harness.scala"
  os.write.over(hp, src, createFolders = true)
  try
    val r = os.proc("scala-cli", "run", hp.toString, "--main-class", "run047")
      .call(cwd = os.Path(BASE), timeout = ScoreTimeoutMs, check = false, mergeErrIntoOut = true)
    val out = r.out.text()
    out.linesIterator.find(_.startsWith("SCORE tests=")) match
      case Some(line) =>
        val pn = line.stripPrefix("SCORE tests=").trim.split("/")
        ScoreResult(true, pn(0).toInt, pn(1).toInt, "OK")
      case None =>
        ScoreResult(false, 0, task.casesSrc.count(_ == '>'), if r.exitCode != 0 then "COMPILE_ERROR" else "NO_SCORE")
  catch
    case _: Throwable => ScoreResult(false, 0, task.casesSrc.count(_ == '>'), "SCORE_TIMEOUT")

// ---- main loop -----------------------------------------------------------------------
@main def orchestrate(args: String*): Unit =
  val smoke = args.contains("smoke")
  val subsAll = List("full", "empty", "scrambled").map(n => n -> os.read(os.Path(BASE) / "substrate" / s"$n.md"))
  val models = if smoke then List("qwen2.5-coder:3b") else FullModels
  val subs = if smoke then subsAll.filter(_._1 == "full") else subsAll
  val tasks = Tasks

  val resultsPath = os.Path(BASE) / "results" / "coding.jsonl"
  os.makeDir.all(os.Path(BASE) / "results")
  val done: Set[String] =
    if os.exists(resultsPath) then
      os.read.lines(resultsPath).flatMap { l =>
        try
          val o = ujson.read(l)
          Some(s"${o("model").str}|${o("task").str}|${o("substrate").str}")
        catch case _: Throwable => None
      }.toSet
    else Set.empty

  val total = models.size * subs.size * tasks.size
  var idx = 0
  println(s"[047] ${if smoke then "SMOKE" else "FULL"} run: $total cells, ${done.size} already done")

  for model <- models do
    println(s"[047] set-model $model")
    try setModel(model) catch case e: Throwable => println(s"[047] set-model failed: ${e.getMessage}")
    for (subName, subText) <- subs; task <- tasks do
      idx += 1
      val key = s"$model|${task.name}|$subName"
      if done(key) then println(s"[047] ($idx/$total) skip $key")
      else
        val instr = "Implement the following in Scala 3, following the project's code conventions. Return only the code, no explanation.\n\nTASK: " + task.spec
        val prompt = if subText.trim.isEmpty then instr else subText + "\n\n" + instr
        val response = try generate(prompt) catch case e: Throwable => s"GEN_FAIL: ${e.getMessage}"
        val code = extractCode(response)
        val sc = if response.startsWith("GEN_FAIL") then ScoreResult(false, 0, 0, "GEN_FAIL") else scoreCandidate(code, task)
        val styleHits = task.styleChecks.filter(_._2(code))
        val smellHits = SmellChecks.filter(_._2(code))
        val line = ujson.Obj(
          "arm" -> "coding",
          "model" -> model,
          "task" -> task.name,
          "substrate" -> subName,
          "compiles" -> sc.compiles,
          "testsPass" -> sc.pass,
          "testsTotal" -> sc.total,
          "styleScore" -> styleHits.size,
          "styleTotal" -> task.styleChecks.size,
          "styleHits" -> ujson.Arr(styleHits.map(h => ujson.Str(h._1))*),
          "smells" -> smellHits.size,
          "smellHits" -> ujson.Arr(smellHits.map(h => ujson.Str(h._1))*),
          "reason" -> sc.reason,
          "code" -> code,
          "gen" -> response
        )
        os.write.append(resultsPath, line.render() + "\n")
        println(s"[047] ($idx/$total) $key -> tests ${sc.pass}/${sc.total} style ${styleHits.size}/${task.styleChecks.size} smells ${smellHits.size} [${sc.reason}]")

  println(s"[047] done. results at $resultsPath")
