package reqt

trait Show[-A]:
  def show(a: A): String

object Show:
  extension [A](a: A)(using s: Show[A]) 
    def show: String = s.show(a)

  given showInt: Show[Int] with
    override def show(a: Int): String = a.toString

  given showString: Show[String] with
    override def show(a: String): String = 
      if a.contains('"') || a.contains('\n') then s"\"\"\"$a\"\"\"" else s"\"$a\""

  given showLink: Show[Link] with
    override def show(el: Link): String = s"${el.e.show}.${el.t.show}"

  given showNode: Show[Node] with
    override def show(n: Node): String = n match
      case Ent(et, id)     => s"${et.toString}(${id.show})"
      case u: Undefined[?] => u.toString
      case IntAttr(at, v)  => s"${at.toString}(${v.show})"
      case StrAttr(at, v)  => s"${at.toString}(${v.show})"

  given showLinkOrNode: Show[Link | Node] with
    override def show(ln: Link | Node): String = ln match
      case n: Node => n.show
      case l: Link => l.show

  given showEmptyPath: Show[Path.type] with 
    override def show(p: Path.type): String = "Path"

  given showPath: Show[Path] with
    override def show(p: Path): String =
      if p.links.isEmpty && !p.hasDest then "Path" else  
        val showLinks: Vector[String] = p.links.map(_.show)
        val showDest: String = if !p.hasDest then "" else 
          p.dest match
            case e: Ent          => e.show
            case a: Attr[?]      => a.show
            case at: AttrType[?] => at.show
            case et: EntType     => et.show
        val xs = if p.hasDest then (showLinks :+ showDest) else showLinks
        xs.mkString("Path(","/", ")")

  given showElem: Show[Elem] with 
    override def show(e: Elem): String = e match 
      case n: Node     => n.show
      case Rel(e, rt, sub) => s"""${e.show}.${rt.show}(${sub.elems.map(_.show).mkString(",")})"""

  given showElemVector: Show[Vector[Elem]] with
    override def show(es: Vector[Elem]): String = es.map(_.show).mkString("Vector(", ",", ")")
  
  given showModel: Show[Model] with 
    override def show(m: Model): String = 
      val sb = scala.collection.mutable.StringBuilder("Model(\n")
      def indent2(lvl: Int): String = "  " * lvl
      def loop(elems: Vector[Elem], indentLevel: Int): Unit = 
        if elems.isEmpty then () else
          elems.head match
            case Rel(e, rt, sub) => 
              sb.append(indent2(indentLevel))
              if sub == Model.empty then 
                sb.append(Link(e, rt).show) 
                sb.append("(),\n")
                loop(elems.drop(1), indentLevel)
              else if sub.size == 1 then // this Rel is a oneliner
                sb.append(e.show + " " + rt.show + " " + sub.elems.head.show + ",\n")
                loop(elems.drop(1), indentLevel)
              else 
                sb.append(Link(e, rt).show)
                sb.append("(\n")
                loop(sub.elems, indentLevel + 1)  // not tail-recursive
                sb.append(indent2(indentLevel))
                sb.append("),\n")
                loop(elems.drop(1), indentLevel)
            case e => 
              sb.append(indent2(indentLevel))
              sb.append(e.show)
              sb.append(",\n")
              loop(elems.drop(1), indentLevel)
      end loop
      loop(m.elems, 1)
      sb.append(")")
      sb.toString

  given showElemType: Show[ElemType] with
    override def show(et: ElemType): String = et.toString

  given showRelType: Show[RelType] with
    override def show(rt: RelType): String = s"$rt".deCapitalize
