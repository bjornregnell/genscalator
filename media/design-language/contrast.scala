//> using scala 3.8.4
// contrast — generate the WCAG 2.1 contrast table for the design-language palette (pure:
// compute → print markdown; paste output into README.md's Contrast section and keep the
// palette list below in sync with README.md's Colors block).
object Contrast {

  type Hex = String

  val palette: Vector[(String, Hex)] = Vector(
    "HIO hot-iron-orange"      -> "#ee582b",
    "VRO vivid-red-orange"     -> "#FA4616",
    "CHIO light-blue"          -> "#11a7d4",
    "CVRO bright-blue"         -> "#05b9e9",
    "TIP tempered-iron-purple" -> "#17193f",
    "CTIP bone-white"          -> "#e8e6c0",
    "ACG anvil-coal-graphite"  -> "#322b25",
    "CACG cold-gray"           -> "#cdd4da",
  )

  val backgrounds: Vector[(String, Hex)] = Vector(
    "ACG"   -> "#322b25",
    "TIP"   -> "#17193f",
    "CTIP"  -> "#e8e6c0",
    "CACG"  -> "#cdd4da",
    "white" -> "#ffffff",
  )

  def channel(v: Int): Double =
    val c = v / 255.0
    if c <= 0.04045 then c / 12.92 else math.pow((c + 0.055) / 1.055, 2.4)

  def luminance(hex: Hex): Double =
    val h = hex.stripPrefix("#")
    val (r, g, b) = (Integer.parseInt(h.substring(0, 2), 16),
                     Integer.parseInt(h.substring(2, 4), 16),
                     Integer.parseInt(h.substring(4, 6), 16))
    0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)

  def ratio(fg: Hex, bg: Hex): Double =
    val (l1, l2) = (luminance(fg) max luminance(bg), luminance(fg) min luminance(bg))
    (l1 + 0.05) / (l2 + 0.05)

  def badge(r: Double): String =
    if r >= 7.0 then "AAA" else if r >= 4.5 then "AA" else if r >= 3.0 then "AA-large" else "fail"

  def cell(fg: Hex, bg: Hex): String =
    if fg.equalsIgnoreCase(bg) then "-"
    else
      val r = ratio(fg, bg)
      f"$r%.2f ${badge(r)}"

  def table: String =
    val header = "| text color \\ background | " + backgrounds.map(_._1).mkString(" | ") + " |"
    val sep    = "|---|" + backgrounds.map(_ => "---|").mkString
    val rows = palette.map { (name, fg) =>
      s"| $name `$fg` | " + backgrounds.map((_, bg) => cell(fg, bg)).mkString(" | ") + " |"
    }
    (header +: sep +: rows).mkString("\n")
}

@main def designLanguageContrastTable(): Unit =
  println(Contrast.table)
  println()
  println("Legend: AAA >= 7.0 · AA >= 4.5 (normal text) · AA-large >= 3.0 (large text/UI) · fail < 3.0 (WCAG 2.1)")
