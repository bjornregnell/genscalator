package reqt

object Selection:
  extension (e: Ent)       def &(rt: RelType)  = Link(e,rt)
  extension (et: EntType)  def &(rt: RelType)  = LinkType(et,rt)

  type Expr = Term | Or
  type Term = Elem | ElemType | Link | LinkType

  case class Or(terms: Term*):
    override def toString = terms.mkString(" | ")

  extension (lhs: Or) 
    def |(rhs: Term): Or = Or((lhs.terms :+ rhs)*)
    def |(rhs: Or): Or = Or((lhs.terms ++ rhs.terms)*)
    def &(rhs: RelType): Or =
      val terms: Seq[Term] = lhs.terms.map: term =>
        (term, rhs) match
          case (e: Ent, rt: RelType) => Link(e, rt)
          case (et: EntType, rt: RelType) => LinkType(et, rt)
          case pair => pair._1
      Or(terms*)

  extension (lhs: Term) 
    def |(rhs: Term): Or = Or(lhs,rhs)
    def |(rhs: Or): Or = Or((lhs +: rhs.terms)*)

  def select(s: Expr, m: Model): Model = 
    val hasTerm = s match 
      case se: Or => se.terms.toSet
      case st: Term => Set(st)

    val pickedElems = m.elems.flatMap:
      elem => elem match
        case e: Ent     if hasTerm(e.t) || hasTerm(e) => Some(e) 

        case a: Attr[?] if hasTerm(a.t) || hasTerm(a) => Some(a)

        case r: Rel => 
          val sub = r.sub.keep(s)
          if sub.elems.nonEmpty then Some(Rel(r.e, r.t, sub)) 
          else if // TODO: think more about what this means
            hasTerm(r.t) ||  
            hasTerm(r.e) || 
            hasTerm(r.e & r.t) || 
            hasTerm(r.e.t) ||
            hasTerm(r.e.t & r.t) ||
            hasTerm(r) ||
            r.expandSubnodes.exists(rel => hasTerm(rel))
          then Some(Rel(r.e, r.t, sub))
          else None

        case _ => None

    Model(pickedElems)
  end select
end Selection

