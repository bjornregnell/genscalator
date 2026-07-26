//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8
//> using dep com.lihaoyi::upickle:4.4.3

// Prepares a BLIND candidate set for the finer LLM style-rater (Round 10).
// Emits compiling candidates for a chosen set of capable models (whose mechanical lint-style
// ceilinged at ~1.00, so the finer measure is most informative there) across all 3 substrates,
// with condition STRIPPED. A private key file maps id -> (model, substrate, task, lintStyle) for
// the join, held by the researcher only. Bare: `scala-cli run style_prep.scala`.

val BASE = "/home/bjornr/git/berg/bjornregnell/genscalator/research/047-run"
val CHOSEN = Set("qwen2.5-coder:7b", "qwen2.5-coder:3b", "deepseek-coder:6.7b", "codegemma:7b")

@main def prep(): Unit =
  val rows = os.read.lines(os.Path(BASE) / "results" / "coding.jsonl").toList.flatMap { l =>
    try Some(ujson.read(l)) catch case _: Throwable => None
  }
  val chosen = rows.filter(o => o("compiles").bool && CHOSEN.contains(o("model").str))
  val input = new StringBuilder
  val key = new StringBuilder
  chosen.zipWithIndex.foreach { (o, i) =>
    input.append(ujson.Obj("id" -> i, "task" -> o("task").str, "code" -> o("code").str).render()).append("\n")
    key.append(ujson.Obj(
      "id" -> i, "model" -> o("model").str, "substrate" -> o("substrate").str,
      "task" -> o("task").str,
      "lintStyle" -> (if o("styleTotal").num > 0 then o("styleScore").num / o("styleTotal").num else 0.0)
    ).render()).append("\n")
  }
  os.write.over(os.Path(BASE) / "results" / "style-input.jsonl", input.toString)
  os.write.over(os.Path(BASE) / "results" / "style-key.jsonl", key.toString)
  println(s"[style_prep] ${chosen.size} compiling candidates from ${CHOSEN.size} models -> style-input.jsonl (blind) + style-key.jsonl (private)")
