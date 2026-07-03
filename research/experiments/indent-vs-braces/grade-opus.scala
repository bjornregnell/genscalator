//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8

// grade-opus — grade the Opus subagent candidate files (written by the anchor workflow) with the SAME harness
// as the local sweep, and append rows (model = opus-4.8) to results-main.tsv.
//   scala-cli run grade-opus.scala -- <dir-of-candidates>
// Candidate filename: opus__<task>__<style>__<run>.scala  (robust to an accidental ```scala fence).
val Root = os.Path("/home/bjornr/git/berg/bjornregnell/genscalator/research/experiments/indent-vs-braces")

def extractCode(s: String): String =
  val i = s.indexOf("```")
  if i < 0 then s.trim
  else
    val after = s.substring(i + 3).dropWhile(_ != '\n').drop(1)
    val end = after.indexOf("```"); (if end < 0 then after else after.substring(0, end)).trim

def emissionConforms(code: String, style: String): String =
  val braces = code.count(_ == '{')
  style match
    case "braces"    => if braces >= 2 then "conform" else "nonconform"
    case "braceless" => if braces == 0 then "conform" else "nonconform"
    case _           => "na"

@main def gradeOpus(dir: String): Unit =
  val out = Root / "results-main.tsv"
  val cands = os.list(os.Path(dir)).filter(p => p.last.startsWith("opus__") && p.ext == "scala").sorted
  val tmp = os.temp.dir(prefix = "opus-grade")
  var n = 0
  for cand <- cands do
    val name = cand.last.stripSuffix(".scala").stripPrefix("opus__")
    name.split("__") match
      case Array(task, style, run) =>
        n += 1
        val taskDir = Root / "tasks" / task
        val expected = os.read(taskDir / "expected.txt").trim
        val code = extractCode(os.read(cand))
        val clean = tmp / s"$name.scala"; os.write.over(clean, code)
        val emitted = emissionConforms(code, style)
        val graded = scala.util.Try(os.proc("scala-cli", "run", clean.toString, (taskDir / "probe.scala").toString)
          .call(check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 120000)) match
            case scala.util.Success(r) if r.exitCode == 0 => if r.out.text().trim == expected then "PASS" else "FAIL_MISSCOPE"
            case scala.util.Success(_) => "FAIL_COMPILE"
            case _ => "FAIL_TIMEOUT"
        val diff = scala.util.Try(os.proc("diff", (taskDir / s"before.$style.scala").toString, clean.toString)
          .call(check = false, stdout = os.Pipe).out.text().linesIterator.count(l => l.startsWith("<") || l.startsWith(">"))).getOrElse(-1)
        os.write.append(out, s"$task\t$style\topus-4.8\t$run\t$emitted\t$graded\t${code.length / 4}\t$diff\n")
        println(s"$task $style run$run -> emit=$emitted grade=$graded")
      case _ => println(s"skip (bad name): ${cand.last}")
  println(s"graded $n opus cells -> $out")
