//> using scala 3.8.4
//> using jvm 21

// limitstore — the SHARED, MAINLESS store logic behind `tt limit` (writer/CLI) and `tt statusline`
// (reader/renderer). Split out like minijson.scala: a tool file with a @main cannot be `using file`-
// included by another tool (two main classes break the launcher's single-file fallback path).
// Store: ~/.claude/gs-limits.json — GLOBAL, not per-session (account limits are account-global;
// the SM208 principle: session-scoped modes, global budget store). Shape per label:
//   {"f5":{"used_percentage":84,"resets_at_ms":1785222000000,"declared_at_ms":1784900000000}}

object LimitStore:
  def defaultFile: java.nio.file.Path =
    java.nio.file.Path.of(sys.props.getOrElse("user.home", "."), ".claude", "gs-limits.json")

  /** "3d20h" / "5h" / "90m" -> ms. None on anything else. PURE. */
  def durToMs(s: String): Option[Long] =
    val m = "^(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?$".r
    s.trim match
      case m(d, h, min) if d != null || h != null || min != null =>
        def part(v: String, unit: Long) = Option(v).map(_.toLong * unit).getOrElse(0L)
        Some(part(d, 86400_000L) + part(h, 3600_000L) + part(min, 60_000L))
      case _ => None

  final case class Decl(label: String, usedP: Double, resetsAtMs: Long, declaredAtMs: Long)

  /** Read all declarations; unreadable/absent file -> empty (never crash a caller). */
  def read(file: java.nio.file.Path): Vector[Decl] =
    try
      if !java.nio.file.Files.isRegularFile(file) then Vector.empty
      else
        MiniJson.parse(String(java.nio.file.Files.readAllBytes(file), "UTF-8")).flatMap(_.obj) match
          case None => Vector.empty
          case Some(o) =>
            o.toVector.flatMap: (label, v) =>
              for
                m <- v.obj
                p <- m.get("used_percentage").flatMap(_.num)
                r <- m.get("resets_at_ms").flatMap(_.num)
              yield Decl(label, p, r.toLong, m.get("declared_at_ms").flatMap(_.num).map(_.toLong).getOrElse(0L))
    catch case _: Throwable => Vector.empty

  /** Serialize + write (tiny fixed shape; labels sorted for a stable diff). */
  def write(file: java.nio.file.Path, decls: Vector[Decl]): Unit =
    val body = decls.sortBy(_.label).map { d =>
      s"\"${d.label}\":{\"used_percentage\":${d.usedP.round.toInt},\"resets_at_ms\":${d.resetsAtMs},\"declared_at_ms\":${d.declaredAtMs}}"
    }.mkString("{", ",", "}")
    Option(file.getParent).foreach(java.nio.file.Files.createDirectories(_))
    java.nio.file.Files.writeString(file, body + "\n")

  def fmtLeft(ms: Long): String =
    if ms <= 0 then "past reset"
    else
      val mins = ms / 60000L
      if mins < 60 then s"${mins}m left"
      else if mins < 1440 then s"${mins / 60}h left"
      else s"${mins / 1440}d left"
