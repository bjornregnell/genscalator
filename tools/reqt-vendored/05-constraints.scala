package reqt 

/** A Scala-embedded DSL for expressing integer constraint satisfaction problems. */
object csp: 
  def constraints(cs: Constr*): Seq[Constr] = cs.toSeq 

  def intVars(n: Int): Seq[Var] = for i <- 0 until n yield IntVar(i)
  def intVars(r: Range): Seq[Var] = for i <- r yield IntVar(i)
  def intVarIds[T](ids: T*): Seq[Var] = ids.map(IntVar.apply)

  def varsBy[T](n: Int)(id: Int => T): Seq[Var] = for i <- 0 until n yield IntVar(id(i))
  def varsBy[T](r: Range)(id: Int => T): Seq[Var] = for i <- r yield IntVar(id(i))

  def forAll[T](xs: Seq[T])(f: T => Constr): Seq[Constr] = xs.map(f(_))

  def forAll[T1, T2](x1s:Seq[T1], x2s: Seq[T2])(f: (T1, T2) => Constr): Seq[Constr] = 
    for (x1 <- x1s; x2 <- x2s) yield f(x1, x2)

  def forAll[T1, T2, T3](x1s:Seq[T1], x2s: Seq[T2], x3s: Seq[T3])(f: (T1, T2, T3) => Constr): Seq[Constr] = 
    for (x1 <- x1s; x2 <- x2s; x3 <- x3s) yield f(x1, x2, x3)

  def sumForAll[T](xs:Seq[T])(f: T => Var) = SumBuilder(xs.map(f(_)).toVector) 

  extension (cs: Seq[Constr])
    def variables: Seq[Var] = cs.flatMap(_.variables)

  transparent trait HasVariables: 
    def variables: Seq[Var]  

  trait Constr extends HasVariables

  trait Var:
    type Id
    type Value
    def id: Id
    def fromInt(i: Int): Value
    def toInt(value: Value): Int
    def ===(y: Var): XeqY                  = XeqY(this, y)
    def ===(value: Int): XeqC              = XeqC(this, value)
    def ===(value: Boolean): XeqBool       = XeqBool(this, value)
    def ===(sumThat: SumBuilder): SumEq    = SumEq(sumThat.vs, this)
    def ===(mulThat: MulBuilder): XmulYeqZ = XmulYeqZ(mulThat.x, mulThat.y, this)

    def >(y: Var): XgtY                    = XgtY(this, y)
    def >(value: Int): XgtC                = XgtC(this, value)

    def >=(y: Var): XgteqY                 = XgteqY(this, y)
    def >=(value: Int): XgteqC             = XgteqC(this, value)

    def <(y: Var): XltY                    = XltY(this, y)  
    def <(value: Int): XltC                = XltC(this, value)

    def <=(y: Var): XlteqY                 = XlteqY(this, y)  
    def <=(value: Int): XlteqC             = XlteqC(this, value)

    def =/=(y: Var): XneqY                 = XneqY(this, y)
    def =/=(value: Int): XneqC             = XneqC(this, value)
    def =/=(value: Boolean): XeqBool       = XeqBool(this, !value)
    
    def *(y: Var) = MulBuilder(this, y)  
    def +(y: Var) = PlusBuilder(this, y)  
  
  object Var:
    def apply[T](id: T): IntVar[T] = IntVar(id)
    def apply[T, U](id: T, values: Seq[U]) = EnumVar(id, values)
    def apply[T, U](id: T, values: Array[U]) = EnumVar(id, values)

  extension (i: Int) def toValueOf(v: Var): v.Value = v.fromInt(i)
  case class IntVar[T](id: T) extends Var:
    type Id = T
    type Value = Int
    override def fromInt(i: Int): Int = i
    override def toInt(value: Int): Int = value

    override def toString: String = s"IntVar(id=$id)"
  end IntVar

  case class EnumVar[T, U](id: T, values: Seq[U]) extends Var:
    type Id = T
    type Value = U
    override def fromInt(i: Int): U = values(i)
    lazy val indexOfValue: Map[U, Int] = values.zipWithIndex.toMap
    override def toInt(value: U): Int = indexOfValue(value)

    override def toString: String = s"EnumVar(id=$id,values=$values)"
  object EnumVar:
    def apply[T, U](id: T, values: Array[U]): EnumVar[T, U] = new EnumVar(id, values.toSeq)

  case class SumBuilder(vs: Vector[Var]): 
    def ===(y: Var): SumEq = SumEq(vs, y)
 
  case class MulBuilder(x: Var, y: Var):
    def ===(z: Var): XmulYeqZ = XmulYeqZ(x, y, z)

  case class PlusBuilder(x: Var, y: Var):
    def ===(z: Var): XplusYeqZ = XplusYeqZ(x, y, z)
    def <=(z: Var): XplusYlteqZ = XplusYlteqZ(x, y, z)

  def sum(v: Var, vs: Var *): SumBuilder = SumBuilder(v +: vs.toVector)
  def sum(vs: Seq[Var]): SumBuilder      = SumBuilder(vs.toVector)

  trait PrimitiveConstr extends Constr:  // constraints that can be used as arguments in logical and conditional constraints
    def <=>(y: Var): Reified = Reified(this, y)

  trait Constr1IntConst extends Constr: 
    val x: Var
    val c: Int
    val variables: Seq[Var] = Seq(x) 

  trait Constr1BoolConst extends Constr: 
    val x: Var
    val c: Boolean
    val variables: Seq[Var] = Seq(x) 

  trait Constr2 extends Constr: 
    val x: Var; val y: Var
    val variables: Seq[Var] = Seq(x, y) 

  trait Constr3 extends Constr: 
    val x: Var; val y: Var; val z: Var
    val variables: Seq[Var] = Seq(x, y, z) 
  trait ConstrSeq1 extends Constr: 
    val seq1: Seq[Var]
    val variables: Seq[Var] = seq1

  trait Constr1Seq1 extends Constr: 
    val x: Var
    val seq1: Seq[Var]
    val variables: Seq[Var] = Seq(x) ++ seq1 

  trait Constr1Seq1IntConst extends Constr: 
    val x: Var
    val seq1: Seq[Var]
    val c: Int
    val variables: Seq[Var] = Seq(x) ++ seq1 

  trait Constr2Seq1 extends Constr: 
    val x: Var; val y: Var
    val seq1: Seq[Var]
    val variables: Seq[Var] = Seq(x, y) ++ seq1

  trait ConstrSeq2ConstSeq1 extends Constr: 
    def seq1: Seq[Var]
    def seq2: Seq[Var]
    def constSeq1: Seq[Int]
    lazy val variables: Seq[Var] = seq1 ++ seq2

  trait ConstrMatrix extends Constr: 
    def matrix: Vector[Vector[Var]]
    val variables: Seq[Var] = matrix.flatten

  trait CompoundConstr extends Constr:
    val constrSeq: Seq[Constr]
    lazy val variables: Seq[Var]  = constrSeq.flatMap(_.variables).distinct

  trait CompoundConstr1 extends CompoundConstr:
    val c1: Constr
    val constrSeq = Seq(c1)

  trait CompoundConstr2 extends CompoundConstr:
    val c1: Constr
    val c2: Constr
    val constrSeq = Seq(c1, c2)
    
  trait CompoundConstr3 extends CompoundConstr:
    val c1: Constr
    val c2: Constr
    val c3: Constr
    val constrSeq = Seq(c1, c2, c3)

  trait CompoundConstr1Var1 extends CompoundConstr1:
    val x: Var
    override lazy val variables: Seq[Var]  = (constrSeq.flatMap(_.variables) :+ x).distinct

  case class Bounds(seq1: Seq[Var], domain: Seq[Range]) extends ConstrSeq1

  extension (v: Var)
    infix def in(r: Range): Bounds = Bounds(Seq(v), Seq(r))
    infix def in(rs: Seq[Range]): Constr = Bounds(Seq(v), rs)

  extension (vs: Seq[Var])
    infix def in(r: Range): Bounds = Bounds(vs, Seq(r))
    infix def in(rs: Seq[Range]): Bounds = Bounds(vs, rs)

  extension (b: Bounds)
    infix def in(r: Range): Bounds = b.copy(domain = b.domain :+ r)
    infix def in(rs: Seq[Range]): Bounds = b.copy(domain = b.domain ++ rs)

  case class AbsXeqY(x: Var, y: Var) extends Constr2, PrimitiveConstr

  case class AllDifferent(seq1: Seq[Var]) extends ConstrSeq1

  case class And(constrSeq: Seq[Constr]) extends CompoundConstr
  case object And: 
    def apply(c1: Constr, c2: Constr) = new And(Seq(c1, c2))

  case class Indexed(index: Var, varSeq: Seq[Var], valueAtIndex: Var) extends Constr2Seq1:
    val x = index
    val y = valueAtIndex
    val seq1 = varSeq

  case class SumEq(seq1: Seq[Var], x: Var) extends Constr1Seq1 
  case class Count(seq1: Seq[Var], x: Var, c: Int) extends Constr1Seq1IntConst
  case class XeqC(x: Var, c: Int) extends Constr1IntConst, PrimitiveConstr 

  case class XeqY(x: Var, y: Var) extends Constr2, PrimitiveConstr

  case class XdivYeqZ(x: Var, y: Var, z: Var) extends Constr3, PrimitiveConstr
  case class XexpYeqZ(x: Var, y: Var, z: Var) extends Constr3, PrimitiveConstr 
  case class XmulYeqZ(x: Var, y: Var, z: Var) extends Constr3, PrimitiveConstr 
  case class XplusYeqZ(x: Var, y: Var, z: Var) extends Constr3, PrimitiveConstr 
  case class XplusYlteqZ(x: Var, y: Var, z: Var) extends Constr3, PrimitiveConstr
  case class Distance(x: Var, y: Var, z: Var) extends Constr3, PrimitiveConstr

  case class XgtC(x: Var, c: Int) extends Constr1IntConst, PrimitiveConstr 
  
  case class XgteqC(x: Var, c: Int) extends Constr1IntConst, PrimitiveConstr
  case class XgteqY(x: Var, y: Var) extends Constr2, PrimitiveConstr
  case class XgtY(x: Var, y: Var) extends Constr2, PrimitiveConstr
  case class XltC(x: Var, c: Int) extends Constr1IntConst, PrimitiveConstr
  case class XlteqC(x: Var, c: Int) extends Constr1IntConst, PrimitiveConstr 
  case class XlteqY(x: Var, y: Var) extends Constr2, PrimitiveConstr
  case class XltY(x: Var, y: Var) extends Constr2, PrimitiveConstr
  case class XneqC(x: Var, c: Int) extends Constr1IntConst, PrimitiveConstr
  case class XneqY(x: Var, y: Var) extends Constr2, PrimitiveConstr
  case class XeqBool(x: Var, c: Boolean) extends Constr1BoolConst, PrimitiveConstr 

  case class IfThen(c1: PrimitiveConstr, c2: PrimitiveConstr) extends CompoundConstr2, PrimitiveConstr

  case class IfThenElse(c1: PrimitiveConstr, c2: PrimitiveConstr, c3: PrimitiveConstr) extends CompoundConstr3, PrimitiveConstr

  case class IfThenBool(x: Var, y: Var, z: Var) extends Constr3, PrimitiveConstr

  case class Reified(c1: PrimitiveConstr, x: Var) extends CompoundConstr1Var1

  case class Rectangle(x: Var, y: Var, dx: Var, dy: Var) extends HasVariables: // used in Diff2
    lazy val toVector: Vector[Var] = Vector(x, y, dx, dy)
    lazy val variables: Seq[Var] = toVector

  case class Diff2(rectangles: Vector[Vector[Var]]) extends ConstrMatrix:
    lazy val matrix = rectangles     
    assert(rectangles.map(_.size).distinct == Vector(4), "size of all rectangle vectors must be 4")
  object Diff2:
    def apply(rectangles: Rectangle *) = new Diff2(rectangles.toVector.map(_.toVector))

  case class Binpacking(item: Vector[Var], load: Vector[Var], size: Vector[Int]) 
      extends ConstrSeq2ConstSeq1:
    lazy val seq1 = item
    lazy val seq2 = load
    lazy val constSeq1 = size    


  object parseConstraints: 
    private object NotReadyShouldProbablyBeRewriteFromScratch:
      // This is work in progress... More complicated than first anticipated to parse all constr with Path etc
      // TODO: consider only parse simple rels such as x > y x > 1 x <= y x === 0
      import parseUtils.*
      object mk:
        type Param = Var | Int | Boolean

        type ConstrMaker = Seq[Param] => Constr
        
        private def bang(x: Param) = throw err.badParamType(x.toString)
        
        extension (x: Param)
          def asVar: Var      = x match { case x: Var     => x case _ => bang(x)}
          def asInt: Int      = x match { case x: Int     => x case _ => bang(x)}
          def asBool: Boolean = x match { case x: Boolean => x case _ => bang(x)}

        val constr: Map[String, ConstrMaker] = Map(
          "XeqY" -> (xs => XeqY(xs(0).asVar, xs(1).asVar)),   //should these be strings and xs(0).parseVar ???
          "XeqC" -> (xs => XeqC(xs(0).asVar, xs(1).asInt)),
        )

        val oper: Map[String, ConstrMaker] = Map(
          ">" -> {xs => XgtY(xs(0).asVar, xs(1).asVar)},
          "<" -> {xs => XltY(xs(0).asVar, xs(1).asVar)},
        )
      end mk

      val isConstrClass: Set[String] = mk.constr.keySet

      val isOperand: Set[String] = mk.oper.keySet

      def parseIdent(s: String): (String, String) = 
        val fw = s.initLetters
        val rest = s.stripPrefix(fw).trim
        if fw == "Path" then 
          ??? // (Path.fromString(s), rest) 
        else 
          if fw.isEmpty || !fw(0).isUnicodeIdentifierStart || !fw.drop(1).forall(_.isUnicodeIdentifierPart) 
          then throw err.badIdentifier(fw) 
          (fw, rest)

      def parseVar(s: String): (Var, String) = 
        val (inside, rest) = parseInsideParen(s.stripPrefix("Var"))
        val id: (String | Path) = 
          if inside.startsWith("Path") then Path.fromString(inside).getOrElse(throw err.illegalPath(inside))
          else inside
        (Var(id), rest)

      def parseVarList(s: String): Seq[Var] = 
        val xs = s.splitEscaped(',', '"')
        ???
        
      def parseOperator(s: String): (String, String) = 
        val op = s.takeWhile(!_.isWhitespace)
        (op,  s.stripPrefix(op).trim)

      def parseConstr(s: String): Constr =
        val fw = s.initLetters
        if isConstrClass(fw) then 
          val rest1 = s.stripPrefix(fw)
          val (inside, rest2) = parseInsideParen(rest1)
          if rest2.nonEmpty then throw err.unknownTrailing(rest2) else
            val vs = parseVarList(inside) 
            mk.constr(fw)(vs) 
          end if 
        else if fw == "Var" then 
          val (v1, rest1) = parseVar(s)
          val (op, rest2) = parseOperator(rest1) 
          if !isOperand(op) then throw err.operatorExpected(s"$op $rest2")
          val (v2, rest3) = parseVar(rest2)
          if rest3.nonEmpty then throw err.unknownTrailing(rest3)
          mk.oper(op)(Seq(v1, v2))
        else if fw == "Path" then 
          ???  // use Path.fromString
        else if isIdStart(s) then 
          val (i1, rest1) = parseIdent(s)
          val (op, rest2) = parseOperator(rest1) 
          if !isOperand(op) then throw err.operatorExpected(s"$op $rest2")
          val (i2, rest3) = parseIdent(rest2)
          if rest3.nonEmpty then throw err.unknownTrailing(rest3)
          mk.oper(op)(Seq(Var(i1), Var(i2)))
        else throw err.varExpected(s) 
    end NotReadyShouldProbablyBeRewriteFromScratch
    
    def parseLine(line: String): Constr = 
      /* TODO parse more here but perhaps 
         don't try to do too as much as in NotReadyShouldProbablyBeRewriteFromScratch */
      val parts = line.split(" ").map(_.trim).filter(_.nonEmpty)
      if parts.length != 3 then throw err.unknown(line)
      else
        val a = parts(0)
        val op = parts(1)
        val b = parts(2)
        op match  
          case ">" => XgtY(Var(a), Var(b))
          case "<" => XltY(Var(a), Var(b))
          case "=" => XeqY(Var(a), Var(b))
          case _ => throw err.unknown(line)

    def parseLines(s: String): util.Try[Seq[Constr]] = util.Try:
      val nonEmptyTrimmedLines = s.toLines.map(_.trim).filter(_.nonEmpty)
      nonEmptyTrimmedLines.map(parseLine).toSeq

    def apply(s: String): Either[String, Seq[Constr]] = parseLines(s) match
      case scala.util.Failure(exception) => Left(exception.getMessage)
      case scala.util.Success(value)     => Right(value)
    
    extension (s: String) def toConstr: Either[String, Seq[Constr]] = apply(s)
    extension (sa: StrAttr) def toConstr: Either[String, Seq[Constr]] = apply(sa.value)

  end parseConstraints

  object releasePlanningProblem: 
    val requiredEntityTypes = Seq(Release, Feature, Stakeholder, Resource) 

    def missingEntityTypes(m: Model): Seq[EntType] = 
      val ets = m.entTypes.toSet
      requiredEntityTypes.filterNot(et => ets.contains(et)) 
    
    def isValidReleasePlan(m: Model): Boolean = missingEntityTypes(m).isEmpty
      
    def apply(m: Model): Seq[Constr] = 
      if !isValidReleasePlan(m) then Seq() else

        val stakeholders = m.ents.filter(_.t == Stakeholder).distinct
        val features =     m.ents.filter(_.t == Feature).distinct
        val releases =     m.ents.filter(_.t == Release).distinct
        val resources =    m.ents.filter(_.t == Resource).distinct

        val featureOrder: Seq[Constr] = forAll(features) { f => Var(f.has/Order).in(1 to releases.size) }
        val releaseOrder: Seq[Constr] = forAll(releases) { r => Var(r.has/Order).in(1 to releases.size) }

        val weightedBenefit: Seq[Constr] = forAll(stakeholders, features): (s, f) => 
          Var(f.has/s.has/Benefit) ===  (Var(s.has/f.has/Benefit) * Var(s.has/Prio))
        
        val featureBenefitSum: Seq[Constr] = forAll(features): f => 
          Var(f.has/Benefit) === sumForAll(stakeholders)(s => Var(f.has/s.has/Benefit)) 

        val featureBenefitPerRelease: Seq[Constr] = forAll(releases, features) { (r, f) =>
          IfThenElse(Var(f.has/Order) === Var(r.has/Order),
            Var(r.has/f.has/Benefit) === Var(f.has/Benefit),
            Var(r.has/f.has/Benefit) === 0) }
        
        val benefitPerRelease: Seq[Constr] = forAll(releases): r =>
          Var(r.has/Benefit) === sumForAll(features)(f => Var(r.has/f.has/Benefit))
        
        val featureCostPerReleasePerResource: Seq[Constr] = 
          forAll(releases,features, resources): (r, f, res) =>
            IfThenElse(Var(f.has/Order) === Var(r.has/Order),
              Var(r.has/res.has/f.has/Cost) === Var(res.has/f.has/Cost),
              Var(r.has/res.has/f.has/Cost) === 0)
        
        val resourceCostPerRelease: Seq[Constr] = forAll(releases,resources): (r, res) =>
          Var(r.has/res.has/Cost) === sumForAll(features)(f => Var(r.has/res.has/f.has/Cost))
        
        val featureCostPerRelease: Seq[Constr] = forAll(releases,features): (r, f) =>
          Var(r.has/f.has/Cost) === sumForAll(resources)(res => Var(r.has/res.has/f.has/Cost)) 
        
        val costPerRelease: Seq[Constr] = forAll(releases): r =>
          Var(r.has/Cost) === sumForAll(features)(f => Var(r.has/f.has/Cost))
      
        val costLimitPerResource: Seq[Constr] = forAll(releases, resources): (r, res) =>
          Var(r.has/res.has/Cost) <= Var(res.has/r.has/Capacity)
        
        val totalCostPerRelease: Seq[Constr] = forAll(releases): r =>
          Var(r.has/Cost) === sumForAll(resources)(res => Var(r.has/res.has/Cost))
        
        val rs = m.rels
        
        val precedences = rs.collect:
          case Rel(e1, Precedes, Model(Vector(e2: Ent))) => Var(e1.has/Order) < Var(e2.has/Order) 
        
        val exclusions = rs.collect:
          case Rel(e1, Excludes, Model(Vector(e2: Ent))) => Var(e1.has/Order) =/= Var(e2.has/Order) 
          
        val couplings = rs.collect:
          case Rel(e1, Requires, Model(Vector(e2: Ent))) => Var(e1.has/Order) === Var(e2.has/Order)

        val inputConstraints: Seq[Constr] = m.paths.collect:
          case AttrPath(links, dest: IntAttr) => Var(AttrTypePath(links, dest.t)) === dest.value  

        Seq(inputConstraints, featureOrder, releaseOrder, weightedBenefit, featureBenefitSum, featureBenefitPerRelease, benefitPerRelease, featureCostPerReleasePerResource, resourceCostPerRelease, featureCostPerRelease, costPerRelease, costLimitPerResource, totalCostPerRelease, precedences, exclusions, couplings).flatten
      end if
    end apply
  end releasePlanningProblem

end csp