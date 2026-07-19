//> using scala 3.8.4
// slashdot.scala — ONE-OFF (BR ask 2026-07-19): in the two media/img/awareness-*.png terminal
// screenshots, replace every rendered '/' glyph in the footer with a '·', matching the statusline's
// separator change, so the screenshots stay current without re-staging the fake tableaux.
// Method: pure raster ops. (1) background = the image's most frequent colour; ink = colour-distance
// from it. (2) 8-connected ink components. (3) a '/' = thin, tall, single diagonal stroke with a
// strongly NEGATIVE x/y correlation (y grows downward) and a NARROW top row (kills '7', whose top
// bar is wide, and 'z'/'x'/'❯' by corr). A '%'-owned slash is skipped by proximity: any small
// blob (its rings) within 3px of the diagonal's bbox. (4) the '·' STAMP is cloned from a real
// middot glyph found in the same image (small ~round blob), used as an alpha mask and recoloured
// to the erased slash's own ink colour — the retouch reuses the font's own pixels.
// Safety: each image must yield EXACTLY the expected slash count (9) or NOTHING is written.
// Usage: scala-cli run slashdot.scala -- report <png>...    (detect + list, no write)
//        scala-cli run slashdot.scala -- write  <png>...    (apply in place; git holds originals)
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

object SlashDot:
  val ExpectedSlashes = 9

  final case class Comp(pixels: Vector[(Int, Int)]):
    val x0: Int = pixels.iterator.map(_._1).min
    val x1: Int = pixels.iterator.map(_._1).max
    val y0: Int = pixels.iterator.map(_._2).min
    val y1: Int = pixels.iterator.map(_._2).max
    def w: Int = x1 - x0 + 1
    def h: Int = y1 - y0 + 1
    def n: Int = pixels.size
    def cx: Int = (x0 + x1) / 2
    def cy: Int = (y0 + y1) / 2

  def colDist(a: Int, b: Int): Double =
    val dr = ((a >> 16) & 255) - ((b >> 16) & 255)
    val dg = ((a >> 8) & 255) - ((b >> 8) & 255)
    val db = (a & 255) - (b & 255)
    math.sqrt((dr * dr + dg * dg + db * db).toDouble)

  /** Pearson correlation of pixel x vs y. A '/' stroke (up-right to down-left) is strongly negative. */
  def corrXY(c: Comp): Double =
    val n = c.n.toDouble
    val mx = c.pixels.iterator.map(_._1).sum / n
    val my = c.pixels.iterator.map(_._2).sum / n
    var sxy = 0.0; var sxx = 0.0; var syy = 0.0
    c.pixels.foreach: (x, y) =>
      sxy += (x - mx) * (y - my); sxx += (x - mx) * (x - mx); syy += (y - my) * (y - my)
    if sxx == 0 || syy == 0 then 0.0 else sxy / math.sqrt(sxx * syy)

  def components(img: BufferedImage, isInk: (Int, Int) => Boolean): Vector[Comp] =
    val w = img.getWidth; val h = img.getHeight
    val seen = Array.ofDim[Boolean](w, h)
    val out = Vector.newBuilder[Comp]
    for y <- 0 until h; x <- 0 until w do
      if isInk(x, y) && !seen(x)(y) then
        val q = scala.collection.mutable.ArrayDeque((x, y)); seen(x)(y) = true
        val px = Vector.newBuilder[(Int, Int)]
        while q.nonEmpty do
          val (a, b) = q.removeHead(); px += ((a, b))
          var dx = -1
          while dx <= 1 do
            var dy = -1
            while dy <= 1 do
              val nx = a + dx; val ny = b + dy
              if nx >= 0 && nx < w && ny >= 0 && ny < h && isInk(nx, ny) && !seen(nx)(ny) then
                seen(nx)(ny) = true; q.append((nx, ny))
              dy += 1
            dx += 1
        out += Comp(px.result())
    out.result()

  def isSlash(c: Comp, all: Vector[Comp]): Boolean =
    val shapeOk =
      c.h >= 8 && c.h <= 22 && c.w >= 3 && c.w <= 11 && c.h >= (1.3 * c.w) &&
      c.n.toDouble / (c.w * c.h) <= 0.6 && corrXY(c) <= -0.55 &&
      c.pixels.count((_, y) => y <= c.y0 + 1) <= 6 // narrow top rows: excludes '7' (wide top bar, topw >= 10 measured)
    def nearPercentRing = all.exists: o =>
      (o ne c) && o.w <= 8 && o.h <= 8 && o.n <= 40 &&
        o.x1 >= c.x0 - 3 && o.x0 <= c.x1 + 3 && o.y1 >= c.y0 - 2 && o.y0 <= c.y1 + 2
    shapeOk && !nearPercentRing

  /** A real '·' glyph to clone: small, compact, near-square blob. Prefer the densest candidate. */
  def findMiddot(all: Vector[Comp]): Option[Comp] =
    all.filter(c => c.w >= 2 && c.w <= 6 && c.h >= 2 && c.h <= 6 && c.n >= 4 &&
        c.n.toDouble / (c.w * c.h) >= 0.5 && (c.h - c.w).abs <= 2)
      .sortBy(c => -c.n).headOption

  def run(path: String, write: Boolean): Boolean =
    val file = java.io.File(path)
    val img = ImageIO.read(file)
    val w = img.getWidth; val h = img.getHeight
    val counts = scala.collection.mutable.Map[Int, Long]().withDefaultValue(0L)
    for y <- 0 until h; x <- 0 until w do counts(img.getRGB(x, y) & 0xFFFFFF) += 1
    val bg = counts.maxBy(_._2)._1
    def rgb(x: Int, y: Int): Int = img.getRGB(x, y) & 0xFFFFFF
    val comps = components(img, (x, y) => colDist(rgb(x, y), bg) > 60.0)
    val slashes = comps.filter(isSlash(_, comps))
    val dot = findMiddot(comps)
    println(s"$path: bg=#${bg.toHexString} comps=${comps.size} slashes=${slashes.size} middot=${dot.map(d => s"(${d.cx},${d.cy} ${d.w}x${d.h})").getOrElse("NONE")}")
    slashes.sortBy(c => (c.y0, c.x0)).foreach(c =>
      println(f"  slash at (${c.cx}%4d,${c.cy}%4d) bbox ${c.w}x${c.h} n=${c.n} corr=${corrXY(c)}%.2f"))
    if slashes.size != ExpectedSlashes then
      println(s"  !! expected $ExpectedSlashes slashes — NOT writing"); return false
    if dot.isEmpty then
      println("  !! no middot template found — NOT writing"); return false
    if !write then return true
    val d = dot.get
    // the template's alpha mask: per-pixel ink strength relative to the strongest pixel of the glyph
    val dMax = d.pixels.iterator.map((x, y) => colDist(rgb(x, y), bg)).max
    val mask = d.pixels.map((x, y) => (x - d.cx, y - d.cy, colDist(rgb(x, y), bg) / dMax))
    slashes.foreach: c =>
      val fg = c.pixels.maxBy((x, y) => colDist(rgb(x, y), bg))
      val fgCol = rgb(fg._1, fg._2)
      // erase the stroke + its anti-alias halo (a softer threshold, bbox+1, not claimed by another comp)
      val owned = c.pixels.toSet
      val others = comps.iterator.filter(_ ne c).flatMap(_.pixels).filter((x, y) =>
        x >= c.x0 - 1 && x <= c.x1 + 1 && y >= c.y0 - 1 && y <= c.y1 + 1).toSet
      for y <- (c.y0 - 1) to (c.y1 + 1); x <- (c.x0 - 1) to (c.x1 + 1) do
        if x >= 0 && x < w && y >= 0 && y < h && !others((x, y)) &&
          (owned((x, y)) || colDist(rgb(x, y), bg) > 25.0) then img.setRGB(x, y, bg)
      // stamp the recoloured middot clone at the stroke's centre
      mask.foreach: (dx, dy, a) =>
        val x = c.cx + dx; val y = c.cy + dy
        if x >= 0 && x < w && y >= 0 && y < h then
          def mix(sh: Int) = (((bg >> sh) & 255) + a * (((fgCol >> sh) & 255) - ((bg >> sh) & 255))).round.toInt
          img.setRGB(x, y, (mix(16) << 16) | (mix(8) << 8) | mix(0))
    ImageIO.write(img, "png", file)
    println(s"  wrote $path")
    true

@main def slashdot(args: String*): Unit =
  args.toList match
    case "probe" :: path :: x0 :: x1 :: y0 :: y1 :: Nil =>
      val img = ImageIO.read(java.io.File(path))
      val counts = scala.collection.mutable.Map[Int, Long]().withDefaultValue(0L)
      for y <- 0 until img.getHeight; x <- 0 until img.getWidth do counts(img.getRGB(x, y) & 0xFFFFFF) += 1
      val bg = counts.maxBy(_._2)._1
      val comps = SlashDot.components(img, (x, y) => SlashDot.colDist(img.getRGB(x, y) & 0xFFFFFF, bg) > 60.0)
      comps.filter(c => c.cx >= x0.toInt && c.cx <= x1.toInt && c.cy >= y0.toInt && c.cy <= y1.toInt)
        .sortBy(_.x0).foreach(c => println(
          f"comp (${c.cx}%4d,${c.cy}%4d) bbox ${c.w}x${c.h} n=${c.n} corr=${SlashDot.corrXY(c)}%.2f topw=${c.pixels.count((_, y) => y <= c.y0 + 1)}"))
    case mode :: paths if paths.nonEmpty && (mode == "report" || mode == "write") =>
      val ok = paths.map(p => SlashDot.run(p, write = mode == "write")).forall(identity)
      sys.exit(if ok then 0 else 1)
    case _ => println("usage: slashdot report|write <png>... | probe <png> x0 x1 y0 y1"); sys.exit(2)
