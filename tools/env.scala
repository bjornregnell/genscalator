//> using file project.scala
//> using jvm 21
//> using file secrets.scala

// env — read the process environment WITHOUT spilling it. The typed replacement for `printenv` / `env`.
//
//   tt env list [regex]        variable NAMES only, never values          <- the default question
//   tt env has <NAME>          exit 0 if set, 1 if not; prints NOTHING
//   tt env get <NAME>          ONE variable; the value is REDACTED if it looks like a credential
//   tt env get <NAME> --reveal the real value, deliberately and one at a time
//
// BORN FROM A REAL LEAK (2026-07-25). The agent wanted ONE variable — whether Claude Code exposes a
// session id — and ran a bare `printenv`. That printed the whole environment into a durable transcript,
// including two live API tokens, which then had to be rotated. Note what did NOT save it: `printenv` is
// not a cd, not a pipe, not a redirect, so guardcheck saw nothing; and the NOTE tier built that same
// morning listed interpreters, not bulk environment reads.
//
// THE SHAPE FOLLOWS THE FAILURE. "What is in the environment" is almost always a question about NAMES,
// so names is the default and the whole-environment dump does not exist as a verb at all. Values are
// singular, opt-in, and redacted unless the caller says --reveal for that one variable. You cannot ask
// this tool for everything, because that request is the hazard.
//
// READ-ONLY IS NOT SAFE: for an agent the risk of a read is set by where the OUTPUT lands. A transcript
// is durable, copied and quoted, so a bulk read of a credential-bearing surface is a disclosure act.
//
// Allowlisting note: `Bash(tt env list *)` and `Bash(tt env has *)` are safe to grant — neither can emit
// a value. `tt env get` should stay shown-gated, and `--reveal` is a deliberate, visible act.

object EnvTool {

  val Help: String =
    """tt env — read environment variables without spilling them (the audited `printenv` replacement)
      |
      |Usage:
      |  tt env list [regex]          variable NAMES only, never values (optional case-insensitive filter)
      |  tt env has <NAME>            exit 0 if set AND non-blank, 1 otherwise; prints nothing
      |  tt env get <NAME>            one variable; value REDACTED if it looks like a credential
      |  tt env get <NAME> --reveal   print the real value, one variable, deliberately
      |
      |There is deliberately NO verb that prints the whole environment. That request is the hazard this
      |tool exists to remove: a bare `printenv` once put two live tokens into a transcript, which is
      |durable, copied and quoted. Read-only is not the same as safe.
      |
      |A value is withheld when the NAME looks credential-bearing (token, secret, password, api_key,
      |auth, credential, ...), when the VALUE matches a known credential shape (gh*_, AKIA, xox*, AIza,
      |PEM), or when it is long and high-entropy. Redaction shows the first 4 chars and the length, which
      |is enough to tell a placeholder from a real prefix without being usable.
      |
      |Examples:
      |  tt env list CLAUDE           # which CLAUDE_* variables exist
      |  tt env has GITHUB_TOKEN      # is it set, without saying anything about it
      |  tt env get CLAUDE_CODE_SESSION_ID
      |
      |Exit: list/get 0 ok, 2 usage or unknown name; has 0 set / 1 unset.
      |
      |Full reference: tools/README.md""".stripMargin

  /** PURE: the names to show, sorted, optionally filtered by a case-insensitive regex. */
  def selectNames(env: Map[String, String], filter: Option[String]): Either[String, Vector[String]] =
    filter match
      case None => Right(env.keys.toVector.sorted)
      case Some(p) =>
        try
          val rx = ("(?i)" + p).r
          Right(env.keys.toVector.filter(k => rx.findFirstIn(k).isDefined).sorted)
        catch case e: Throwable => Left(s"bad regex '$p': ${e.getMessage}")

  /** PURE: the one-line rendering for `get`. */
  def renderGet(name: String, value: String, reveal: Boolean): String =
    val shown = Secrets.show(name, value, reveal)
    val note  = if !reveal && Secrets.looksSecret(name, value) then "   (redacted; --reveal to print it)" else ""
    s"$name=$shown$note"

  private def fail(msg: String): Nothing =
    System.err.println(s"tt env: $msg")
    sys.exit(2)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then
      println(Help); sys.exit(0)

    val env = sys.env
    args.toList match
      case "list" :: rest =>
        val filter = rest.find(a => !a.startsWith("--"))
        selectNames(env, filter) match
          case Left(msg)    => fail(msg)
          case Right(names) =>
            names.foreach(println)
            System.err.println(s"tt env: ${names.size} name(s); values withheld by design — use `get <NAME>`")
            sys.exit(0)

      // EMPTY COUNTS AS ABSENT, and that is not pedantry. A credential-helper export such as
      // `export TOK="$(keyring get svc acct)"` yields an EMPTY string when the keyring is locked or the
      // helper is missing — a silent failure. Reporting "set" there is false reassurance about exactly the
      // setup this tool is meant to support. `tt forge` already treats blank as absent
      // (`.map(_.trim).find(_.nonEmpty)`), so this matches it rather than inventing a second semantic.
      case "has" :: name :: Nil => sys.exit(if env.get(name).exists(_.trim.nonEmpty) then 0 else 1)
      case "has" :: _           => fail("has takes exactly one <NAME>")

      case "get" :: name :: rest =>
        val reveal = rest.contains("--reveal")
        rest.find(a => a.startsWith("--") && a != "--reveal").foreach(o => fail(s"unknown option '$o'"))
        env.get(name) match
          case Some(v) => println(renderGet(name, v, reveal)); sys.exit(0)
          case None    => fail(s"not set: $name")
      case "get" :: Nil => fail("get needs a <NAME>")

      case other :: _ => fail(s"unknown verb '$other' (expected: list, has, get). " +
        "There is no whole-environment dump on purpose — that is the hazard this tool removes.")
      case Nil => fail("usage: tt env <list|has|get> [args]  (tt env --help)")
}

@main def envRead(args: String*): Unit = EnvTool.dispatch(args*)
