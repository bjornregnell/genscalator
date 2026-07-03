//> using scala 3.8.4
//> using jvm 21

// typo — a keyboard-aware typo classifier for the human-fatigue gauge (BR's idea, 2026-07-02): the human's
// typical typo is hitting a key ADJACENT to the intended one on the Swedish keyboard. Given a typed word and the
// intended word, classify the single-edit error using the Swedish-QWERTY letter adjacency, so the gauge can
// distinguish MOTOR fatigue (adjacency-heavy) from timing (drop/transpose) from cognitive (a real wrong word).
// PURE: reads args, computes, prints.
//   tt typo adjacent <a> <b>              are two single chars keyboard-neighbors on Swedish QWERTY? (yes/no)
//   tt typo classify <typed> <intended>   classify the typo type
// Swedish letter rows (v1: numbers/punctuation omitted): qwertyuiopå / asdfghjklöä / zxcvbnm.
// Adjacency ≈ Chebyshev distance 1 on a (row,col) grid — the staggered rows are approximated as aligned columns
// (a good-enough first model; refine with true stagger offsets later).

// Helpers (the keyboard model + adjacent/classify) scoped in this object so their generic names don't collide
// with other tools when the toolbox compiles together. Only the @main entry is top-level. See skills/scala-style.
object Typo {
  val kbRows = Vector("qwertyuiopå", "asdfghjklöä", "zxcvbnm")
  val kbPos: Map[Char, (Int, Int)] =
    (for (row, r) <- kbRows.zipWithIndex; (ch, c) <- row.zipWithIndex yield ch -> (r, c)).toMap

  /** Are two chars keyboard-neighbors (incl. diagonal)? Case-insensitive; false for unknown chars or equal chars. */
  def adjacent(a: Char, b: Char): Boolean =
    val (x, y) = (a.toLower, b.toLower)
    (kbPos.get(x), kbPos.get(y)) match
      case (Some((r1, c1)), Some((r2, c2))) => x != y && math.max(math.abs(r1 - r2), math.abs(c1 - c2)) == 1
      case _                                => false

  /** Classify a typed-vs-intended pair as one edit type (v1 assumes ~edit-distance-1; else "complex"). */
  def classify(typed: String, intended: String): String =
    val t = typed.toLowerCase; val g = intended.toLowerCase
    if t == g then "match"
    else if t.length == g.length then
      t.indices.filter(i => t(i) != g(i)).toVector match
        case Vector(i)                                                 => if adjacent(t(i), g(i)) then "adjacency" else "substitution-far"
        case Vector(i, j) if j == i + 1 && t(i) == g(j) && t(j) == g(i) => "transposition"
        case _                                                         => "complex"
    else if math.abs(t.length - g.length) == 1 then
      val (shorter, longer) = if t.length < g.length then (t, g) else (g, t)
      var i = 0
      while i < shorter.length && shorter(i) == longer(i) do i += 1 // first divergence
      if shorter.substring(i) == longer.substring(i + 1) then       // one extra char in longer, rest aligns
        if t.length > g.length then "insertion" else "deletion"
      else "complex"
    else "complex"

  def dispatch(args: String*): Unit =
    args.toList match
      case "adjacent" :: a :: b :: Nil if a.length == 1 && b.length == 1 =>
        println(if adjacent(a(0), b(0)) then "yes" else "no")
      case "classify" :: typed :: intended :: Nil =>
        println(classify(typed, intended))
      case _ =>
        println("""typo — keyboard-aware typo classifier (Swedish QWERTY; for the fatigue gauge)
          |  typo adjacent <a> <b>              are two single chars keyboard-neighbors? (yes/no)
          |  typo classify <typed> <intended>   classify: match / adjacency / substitution-far /
          |                                      transposition / deletion / insertion / complex""".stripMargin)
        sys.exit(2)
}

@main def typoClassify(args: String*): Unit = Typo.dispatch(args*)
