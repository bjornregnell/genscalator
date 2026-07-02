//> using scala 3.8.4
//> using dep com.lihaoyi::os-lib:0.11.8

// grade — the deterministic grader for the indent-vs-braces experiment.
// Compiles [candidate, probe] together with scala-cli, runs, compares stdout to expected.
//   scala-cli run grade.scala -- <candidate.scala> <probe.scala> <expected.txt>
// Prints exactly one grade on stdout: PASS | FAIL_COMPILE | FAIL_MISSCOPE
// (compile/timeout error -> FAIL_COMPILE; compiles+runs but wrong behavior -> FAIL_MISSCOPE, the silent hazard).
@main def grade(candidate: String, probe: String, expected: String): Unit =
  val exp = os.read(os.Path(expected, os.pwd)).trim
  val r = os.proc("scala-cli", "run", candidate, probe)
    .call(check = false, stdout = os.Pipe, stderr = os.Pipe, timeout = 120000)
  if r.exitCode != 0 then println("FAIL_COMPILE")
  else
    val got = r.out.text().trim
    if got == exp then println("PASS")
    else
      println("FAIL_MISSCOPE")
      System.err.println(s"expected:\n$exp\n---\ngot:\n$got")
