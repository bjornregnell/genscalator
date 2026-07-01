package reqt 

object quiz:
  def generateQuestion(nbrCOncepts: Int = 5): (Seq[String], Seq[Char]) = 
    val n = nbrCOncepts
    
    val selected: Seq[((String, String), Int)]  = 
      util.Random.shuffle(meta.entityConcepts).take(n).zipWithIndex.toVector

    val secondShuffle: Seq[(String, Int, String)] = 
      val xs = util.Random.shuffle: 
        selected.map((p, i) => p._1 -> i)
      (0 until xs.length).map(i => (xs(i)._1, xs(i)._2, selected(i)._1._2))

    extension (i: Int) def toChoice: Char = ('a' + i).toChar

    val correct = secondShuffle.map(_._2.toChoice)

    val pad = secondShuffle.map(_._1.length).max 

    def doNotSpoil(s: String) = 
      val i = s.toLowerCase().indexOf("short for")
      if i > 0 then s.substring(0, i).trim else s

    val questLines = secondShuffle.zipWithIndex.map: 
      case ((concept, correctOrder, defInWrongPlace), i) =>
        s"${concept.padTo(pad, ' ')}  ${i.toChoice}: ${doNotSpoil(defInWrongPlace).takeWhile(_ != '.')}."

    (questLines, correct)
  end generateQuestion

  def countCorrect(answer: String, correct: Seq[Char]): Int =
    correct.zipWithIndex.map: (c, i) => 
      if Some(c) == answer.lift(i) then 1 else 0
    .sum