package reqt

import scala.collection.mutable
import reqt.PrintUtils.PrettyPrinter

/** Operations of companion to trait `Model` **/
transparent trait ModelCompanion:
  self: Model.type =>

  /** Create a Model from a sequence of elems */
  def apply(elems: Elem*): Model = Model(elems.toVector)

  /** Return the empty model. */
  val empty: Model = Model()

  given defaultModelPrettyPrinter: PrettyPrinter[Model] with
    extension (m: Model) def pp: Unit = println(m.show)

  /** Generate a random model with size <= maxSize and elems.length == nbrTopLevelElems */ 
  def random(
    nbrTopLevelElems: Int = 5, 
    relationProb: Double = 0.25, 
    attrProb: Double = 0.25,
    maxSize: Int = 10000,
  ): Model =
    require(nbrTopLevelElems > 0, "nbrTopLevelElems must be > 0")
    require(attrProb + relationProb >= 0.0 && attrProb + relationProb <= 1.0, "attrRate+probRate must be in [0.0, 1.0]")

    import scala.util.Random.{nextDouble, nextInt}
    extension [T](xs: Array[T]) def pick: T = xs(nextInt(xs.length))
    val words = Array("Do", "Re", "Mi", "Fa", "So", "La", "Ti", "Do")
    def rndEnt() = EntType.values.pick.apply((1 to 3).map(i => words.pick).mkString)

    var tot = 0

    def loop(nbrElems: Int): Model =
      val n = 0 max ((maxSize - tot) min nbrElems)
      tot += n
      val elems = Vector.fill(n):
        val rnd = nextDouble
        if rnd < attrProb then 
          if nextDouble() < 0.5 
          then IntAttrType.values.pick.apply(nextInt(200) - 100)
          else StrAttrType.values.pick.apply((1 to 3).map(i => words.pick).mkString("", " ", "."))
        else if tot + 1 <= maxSize && rnd < relationProb + attrProb then 
          val subModel = loop(nextInt(1 max ((n - 1) min tot + 1)))
          Rel(rndEnt(), RelType.values.pick, subModel)
        else rndEnt()
      
      Model(elems)
    end loop
    if nbrTopLevelElems > 0 then loop(nbrTopLevelElems) else Model.empty

  extension (elems: Seq[Elem]) 
    /** Create a model from a sequence of elements. */
    def toModel = Model(elems.toVector)

    /** Merge adjacent string attributes of same type sat into one single attribute 
     * of type `sat` with all string values concatenated using delim as in-between separator. */
    def concatAdjacent(sat: StrAttrType, delim: String): Vector[Elem] =
      if elems.length < 2 then elems.toVector else
        val result = scala.collection.mutable.Buffer.empty[Elem]
        var currentIndex = 0
        inline def stringOf(elem: Elem): String = elem match
          case sa@StrAttr(at, value) if at == sat => value
          case _ => null  // ugly optimization to avoid allocation

        while currentIndex < elems.length do
          val current: Elem = elems(currentIndex)
          val currentStr: String = stringOf(current)
          if currentStr == null then 
            result.append(current)
            currentIndex += 1
          else
            currentIndex += 1
            var continue: Boolean = currentIndex < elems.length
            if !continue then result.append(current) else
              val sb = mutable.StringBuilder(currentStr)
              while continue do
                val nextStr = stringOf(elems(currentIndex))
                if nextStr == null then
                  continue = false
                else 
                  sb.append(s"$delim$nextStr")
                  currentIndex += 1
                  continue = currentIndex < elems.length
              end while
              result.append(sat.apply(sb.toString))
            end if
        end while
        result.toVector
