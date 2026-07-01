package reqt

import reqt.meta.isElemStart

sealed trait Path:
  type Value 

  type Dest = AttrType[Value] | Attr[Value] | Ent | EntType

  def links: Vector[Link]

  def firstLinkDropped: Path

  def takeLinksRight(n: Int): Path 

  def dest: Dest

  def depth: Int = links.size + (if hasDest then 1 else 0)
  
  def hasDest: Boolean

  def valueOpt: Option[Value] = if !hasDest then None else dest match
    case u: Undefined[?] => None
    case a: Attr[Value]  => Some(a.value)
    case _ => None
  
  def toVector: Vector[Link | Dest] = if !hasDest then links else links :+ dest 

  def toModel: Model = 
    if links.isEmpty then
      if !hasDest then Model() else dest match
        case e: Ent => Model(e)
        case a: Attr[?]  => Model(a)
        case at: AttrType[?] => Model(Undefined(at))
        case et: EntType => Model(et.apply(""))
    else
      val h = links.head
      Model(Rel(h.e, h.t, firstLinkDropped.toModel))

// case object Root:  // TODO: is this needed or is it just another unnecessary way of doing Path(...)
//   def /(link: Link): LinkPath = LinkPath(Vector(link))
//   def /[T](a: Attr[T]): AttrPath[T] = AttrPath[T](Vector(), a)
//   def /[T](a: AttrType[T]): AttrTypePath[T] = AttrTypePath[T](Vector(), a)
//   def /(e: Ent): EntPath = EntPath(Vector(), e)
//   def /(e: EntType): EntTypePath = EntTypePath(Vector(), e)

case object Path:
  val Root = LinkPath(Vector())
  def fromString(s: String): Option[Path] = 
    if s.isEmpty then None 
    else if s == "Path" || s == "Path()" then Some(Path())
    else if s.startsWith("(") && s.endsWith(")") then fromString(s.drop(1).dropRight(1))
    else if s.startsWith("Path(") && s.endsWith(")") then fromString(s.drop(5).dropRight(1))
    else
      import parseUtils.*
      import meta.*
      val parts = s.splitEscaped('/','"').toVector
      if parts.isEmpty then None else
        type ParsedUnion = Elem | ElemType | Link
        val parsed: Vector[(Option[ParsedUnion], String)] = parts.map(meta.parseConcept)
        val parsedParts: Vector[ParsedUnion] = parsed.collect{ case (Some(p), s) if s.isEmpty => p}
        if parsedParts.length != parts.length then None // overflow strings means malformed path 
        else
          val links: Vector[Link] = parsed.collect{ case (Some(Link(e,t)), s) => Link(e,t)}
          val last: ParsedUnion = parsedParts.last
          if parsedParts.dropRight(1).exists(pu => !pu.isInstanceOf[Link]) then None // malformed path if not starting with links
          else
            last match
              case _: Link        => Some(LinkPath(links))
              case a: Attr[?]     => Some(AttrPath(links, a))
              case a: AttrType[?] => Some(AttrTypePath(links, a))
              case e: Ent         => Some(EntPath(links, e))
              case e: EntType     => Some(EntTypePath(links, e))
              case _              => None // malformed path

  def apply(): LinkPath = Root // Empty path

  def apply(p: Path): Path = p  // recursively unpack Path(Path(Path(...)))
    
  def apply(link: Link): LinkPath = LinkPath(Vector(link))
  def apply[T](a: Attr[T]): AttrPath[T] = AttrPath[T](Vector(), a)
  def apply[T](a: AttrType[T]): AttrTypePath[T] = AttrTypePath[T](Vector(), a)
  def apply(e: Ent): EntPath = EntPath(Vector(), e)
  def apply(e: EntType): EntTypePath = EntTypePath(Vector(), e)

  extension (l1: Link) 
    def /(l2: Link): LinkPath = LinkPath(Vector(l1, l2))
    def /[T](a: Attr[T]): AttrPath[T] = AttrPath[T](Vector(l1), a)
    def /[T](a: AttrType[T]): AttrTypePath[T] = AttrTypePath[T](Vector(l1), a)
    def /(e: Ent): EntPath = EntPath(Vector(l1), e)
    def /(e: EntType): EntTypePath = EntTypePath(Vector(l1), e)

  extension (lp: LinkPath) 
    def /(l: Link): LinkPath = LinkPath(lp.links :+ l)
    def /[T](a: Attr[T]): AttrPath[T] = AttrPath[T](lp.links, a)
    def /[T](a: AttrType[T]): AttrTypePath[T] = AttrTypePath[T](lp.links, a)
    def /(e: Ent): EntPath = EntPath(lp.links, e)
    def /(e: EntType): EntTypePath = EntTypePath(lp.links, e)

  extension (ps: Seq[Path]) 
    def toModel: Model = ps.map(_.toModel).foldLeft(Model.empty)(_ :++ _)
end Path

final case class AttrTypePath[T](links: Vector[Link], dest: AttrType[T]) extends Path:
  type Value = T
  def hasDest: Boolean = true
  def firstLinkDropped: AttrTypePath[T] = copy(links = links.tail)
  def takeLinksRight(n: Int): AttrTypePath[T] = copy(links = links.takeRight(n))

final case class AttrPath[T](links: Vector[Link], dest: Attr[T]) extends Path:
  type Value = T
  def hasDest: Boolean = true
  def firstLinkDropped: AttrPath[T] = copy(links = links.tail)
  def takeLinksRight(n: Int): AttrPath[T] = copy(links = links.takeRight(n))

final case class EntTypePath(links: Vector[Link], dest: EntType) extends Path:
  type Value = Nothing
  def hasDest: Boolean = true
  def firstLinkDropped: EntTypePath = copy(links = links.tail)
  def takeLinksRight(n: Int): EntTypePath = copy(links = links.takeRight(n))


final case class EntPath(links: Vector[Link], dest: Ent) extends Path:
  type Value = Nothing
  def hasDest: Boolean = true
  def firstLinkDropped: EntPath = copy(links = links.tail)
  def takeLinksRight(n: Int): EntPath = copy(links = links.takeRight(n))

final case class LinkPath(links: Vector[Link]) extends Path:
  type Value = Nothing
  def dest = throw java.util.NoSuchElementException()
  def hasDest: Boolean = false
  def firstLinkDropped: LinkPath = copy(links = links.tail)
  def takeLinksRight(n: Int): LinkPath = copy(links = links.takeRight(n))
