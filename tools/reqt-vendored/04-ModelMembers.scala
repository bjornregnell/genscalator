package reqt

/** Members of trait Model */
transparent trait ModelMembers:
  self: Model =>

  /** The number of elems at top level plus the sum of sizes of all sub models */
  def size: Int =
    var n = elems.size
    elems.foreach:
      case n: Node => 
      case r: Rel => n += r.sub.size
    n

  /** A new Model with elem e prepended to the elems of this Model. Same as e +: m */
  def prepend(e: Elem): Model = Model(e +: elems)

  /** A new Model with elem e prepended to the elems of this Model. Same as m.prepend(e) */
  def +:(e: Elem): Model = prepend(e)

  /** A new Model with elem e appended to the elems of this Model. Same as m :+ e */
  def append(e: Elem): Model = Model(elems :+ e)

  /** A new Model with elem e appended to the elems of this Model. Same as m.append(e) */
  def :+(e: Elem): Model = append(e)

  /** A new Model with other Model's elems appended to elems. Same as: m :++ other */
  def append(other: Model): Model = Model(elems :++ other.elems)  

  /** A new Model with other Model's elems appended to elems. Same as: m.append(other) */
  def :++(other: Model): Model = append(other)

  /** A new model first attribute at top level of same type updated to a or if it does not exists an attribute of that type then appended a to elems. */
  def updateFirst[T](a: Attr[T]): Model = 
    var isReplaced = false
    val es = elems.map: e => 
      e match
        case a2: Attr[?] if !isReplaced && a.t == a2.t => 
          isReplaced = true
          a  
        case elem => elem 
    Model(if isReplaced then es else elems :+ a)

  /** Merge sub model of r with the sub model of first relations with the same link, or if that link does not exist then append r to elems. */
  def mergeFirst(r: Rel): Model = 
    var isMerged = false
    val es = elems.map: e => 
      e match
        case r2: Rel if !isMerged && r.link == r2.link => 
          isMerged = true
          Rel(r2.e, r2.t, r2.sub ++ r.sub)  // recur with add
        case elem => elem 
    Model(if isMerged then es else elems :+ r)

  /** Add an elem if not already exists at top level. Relations are merged using mergeFirst. All top-level attribute value with same AttrType is updated to same value if exists. Same as m + e */
  def add(e: Elem): Model = 
    e match
      case a: Attr[?] => 
        var isUpdated = false
        val updatedIfExists = elems.map:
          case a2: Attr[?] if a.t == a2.t => 
            isUpdated = true
            a
          case other => other
        if isUpdated then Model(updatedIfExists) else Model(elems :+ a)

      case e: Ent => if !elems.exists(_ == e) then Model(elems :+ e) else self
      case r: Rel  => self.mergeFirst(r)
  
  /** Add an elem if not already exists at top level. Relations are merged using mergeFirst. All top-level attribute value with same AttrType is updated to same value if exists. Same as m.add(e) */
  def +(e: Elem): Model = add(e)

  /** Add multiple elements es using the add method. Same as m ++ (e1, e2, e3) */
  def addAll(es: Elem*): Model = 
    var result = self 
    for e <- es do result += e
    result

  /** Add multiple elements of es using the add method.  Same as m.addAll(e1, e2, e3) */
  def ++(es: Elem*): Model = addAll(es*)  

  /** Add all elements in es using the add method. Same as m ++ Vector(e1, e2, e3) */
  def addAll(es: Vector[Elem]): Model = 
    var result = self 
    for e <- es do result += e
    result

  /** Add all elements of other model using the add method. Same as m.addAll(es) */
  def ++(es: Vector[Elem]): Model = addAll(es)  
  
  /** Add all elements of other model using the add method. Same as m ++ other */
  def addAll(other: Model): Model =  addAll(other.elems) 

  /** Add all elements of other model using the add method. Same as m.addAll(other) */
  def ++(other: Model): Model = addAll(other)  

  /** Remove all top-level elems equal to e. If you instead want deep removal use remove. */
  def removeTop(e: Elem): Model = Model(elems.filterNot(_ == e))

  /** Remove all top-level elems of type et. If you instead want deep removal use remove. */
  def removeTop(et: ElemType): Model = Model(elems.filterNot(e2 => e2.t == et))

  /** Remove all top-level relations linked by l. If you instead want deep removal use remove. */
  def removeTop(l: Link): Model = 
    val es = elems.filterNot: 
      case Rel(e, t, sub) if l.e == e && l.t == t => true
      case _ => false
    Model(es)
  
  /** Remove all elems equal to e recursively. Same as m - e */
  def remove(e: Elem): Model = 
    val es = elems.flatMap: 
      case e2 if e2 == e => Seq()
      case Rel(e2, t, sub) => Seq(Rel(e2, t, sub.remove(e)))
      case e2 => Seq(e2) 
    Model(es)

  /** Remove all elems equal to e recursively. Same as m.remove(e) */
  def -(e: Elem): Model = remove(e)

  /** Remove all elems equal of type t recursively. Same as m - t */
  def remove(t: ElemType): Model = 
    val es = elems.flatMap: 
      case e2 if e2.t == t => Seq()
      case Rel(e2, t2, sub) => Seq(Rel(e2, t2, sub.remove(t)))
      case e2 => Seq(e2) 
    Model(es)

  /** Remove all elems equal to e recursively. Same as m.remove(t) */
  def -(t: ElemType): Model = remove(t) 
  
  /** Remove all relations linked with l. Same as m - t */
  def remove(l: Link): Model = 
    val es = elems.flatMap: 
      case Rel(e2, t2, sub) if e2 == l.e && t2 == l.t => Seq()
      case Rel(e2, t2, sub) => Seq(Rel(e2, t2, sub.remove(l)))
      case e2 => Seq(e2) 
    Model(es)

  /** Remove all relations linked with l. Same as m.remove(t) */
  def -(l: Link): Model = remove(l)

  /** Remove all elems in es using remove. Same as m -- (e1, e2, e3) */
  def removeAll(es: (Elem | ElemType | Link)*): Model = 
    var result = self 
    for e <- es do e match
      case e: Elem => result -= e
      case e: ElemType => result -= e
      case e: Link => result -= e
    result

  /** Remove all elems in es using remove. Same as m.removeAll(e1, e2, e3) */
  def --(es: Elem*): Model = removeAll(es*)
  
  /** All entities and attributes in this model. */
  def nodes: Vector[Node] = elems.flatMap:
    case n: Node => Vector(n) 
    case Rel(e, r, m) => Vector(e) ++ m.nodes

  /** All relation links in of this model. */
  def links: Vector[Link] = elems.flatMap:
    case _: Node => Vector() 
    case Rel(e, r, m) => Vector(Link(e,r)) ++ m.links

  /** All relation types in of this model. */
  def relTypes: Vector[RelType] = links.map(_.t)

  /** All relations of this model. */
  def rels: Vector[Rel] = elems.flatMap:
    case _: Node => Vector() 
    case r@Rel(_, _, m) => Vector(r) ++ m.rels

  /** All undefined attributes of this model. */
  def undefined: Vector[Undefined[?]] = nodes.collect{ case u: Undefined[?] => u }

  /** All entities of this model. */
  def ents: Vector[Ent]         = nodes.collect { case e: Ent => e }

  /** All entities types of this model. */
  def entTypes: Vector[EntType] = nodes.collect { case e: Ent => e.t }

  /** All attributes of this model. */
  def attrs: Vector[Attr[?]]    = nodes.collect { case a: Attr[?] => a }

  /** All string attributes of this model. */
  def strAttrs: Vector[StrAttr] = nodes.collect { case a: StrAttr => a }

  /** All string attribute values of this model. */
  def strValues: Vector[String] = nodes.collect { case a: StrAttr => a.value }

  /** All integer attributes of this model. */
  def intAttrs: Vector[IntAttr] = nodes.collect { case a: IntAttr => a }

  /** All integer attribute values of this model. */
  def intValues: Vector[Int]    = nodes.collect { case a: IntAttr => a.value }
  
  /** All entity ids of this model. */
  lazy val ids: Vector[String] = ents.map(_.id)

  /** A Map from entity id to a Set of entity types of all entity ids of this model. */
  lazy val idTypeMap: Map[String, Set[EntType]] = 
    ents.groupBy(_.id).map((id, xs) => id -> xs.map(_.t).toSet)
  
  /** A Map from entity id to a Vector of all entities with that id. */
  lazy val idMap: Map[String, Vector[Ent]] = 
    ents.groupBy(_.id).map((id, xs) => id -> xs.toVector)

  /** Returns true if all ids have exactly one entity type. */
  def isIdTypeUnique: Boolean = idTypeMap.forall((id, xs) => xs.size == 1)

  /** A Map from id to a set of non-unique entity types for that id. Each set has at least two members. */
  def nonTypeUniqueIds: Map[String, Set[EntType]] = idTypeMap.filter((id, xs) => xs.size > 1)

  /** All string attributes of type sat. */
  def attrsOfType(sat: StrAttrType): Vector[StrAttr] = 
    nodes.collect { case a: StrAttr if a.t == sat => a }

  /** All integer attributes of type iat. */
  def attrsOfType(iat: IntAttrType): Vector[IntAttr] = 
    nodes.collect { case a: IntAttr if a.t == iat => a }

  /** All entities of a certain id. */
  def entsOfId(id: String): Vector[Ent] = idMap.get(id).getOrElse(Vector())

  /** All entities of a certain type. */
  def entsOfType(et: EntType): Vector[Ent] = nodes.collect { case e: Ent if e.t == et => e }

  /** A Set with all entity types of a certain id. */
  def entTypesOfId(id: String): Set[EntType] = idTypeMap.getOrElse(id, Set())
  
  /** The first entity with id. */
  def firstEntOfId(id: String): Option[Ent] = entsOfId(id).headOption

  /** All entities with leaf relation of type at and rt sorted by attribute value. */
  def sortLeafRelsBy[T](at: AttrType[T], rt: RelType = Has): Vector[Ent] = 
    at match
      case iat: IntAttrType => 
        val lrs: Model = leafRelsOf(iat)
        lrs.ents.sortBy(e =>(lrs / Link(e, rt) / iat).headOption)
      case sat: StrAttrType => 
        val lrs: Model = leafRelsOf(sat)
        lrs.ents.sortBy(e =>(lrs / Link(e, rt) / sat).headOption)
      
  /** For each entity generate Has-relations to integer attributes of type iat with rank values from one */
  def withRank(iat: IntAttrType, rt: RelType = Has): Vector[Rel] =
    ents.zipWithIndex.map((e, i) => Rel(e, rt, Model(iat.apply(i + 1))))

  /** For all entities of type et generate relations of type rt to integer attributes of type iat with rank values from one */
  def withRankDistinct(et: EntType, iat: IntAttrType, rt: RelType = Has): Vector[Rel] =
    entsOfType(et).distinct.zipWithIndex.map((e, i) => Rel(e, rt, Model(iat.apply(i + 1))))

  /** A new Model with distinct top-level elems (non-recursive). */
  def distinctTopElems: Model = Model(elems.distinct) 

  /** A new Model that is distinct by top-level attribute type (non-recursive). If duplicate AttrType is found on top level then last attribute is kept. */
  def distinctTopAttrType: Model =
    val foundAttrTypes: collection.mutable.Set[AttrType[?]] = collection.mutable.Set()
    val es = elems.reverse.flatMap:
      case a: Attr[?] => 
        if foundAttrTypes(a.t) then Vector()
        else 
          foundAttrTypes += a.t
          Vector(a)
      case e => Vector(e)
    Model(es.reverse)

  /** A new Model that removes top-level entities that are themselves part of relations. */
  def removeTopEntIfLinked: Model =
    val es = elems.flatMap:
      case e: Ent => 
        if elems.exists: 
          case Rel(e2, t, sub) if e == e2 => true
          case _ => false 
        then Seq()
        else Seq(e)
      case e => Seq(e)
    Model(es)

  /** A new Model that removes all entities that are themselves part of relations att the same level. */
  def removeEntIfLinkedDeep: Model =
    val es = removeTopEntIfLinked.elems.map:
      case n: Node => n
      case Rel(e, r, m) => Rel(e, r, m.removeTopEntIfLinked)
    Model(es)

  /** A new Model with recursive de-duplication of its elems on all levels. */
  def distinctElemsDeep: Model =
    val es = elems.distinct.map:
      case n: Node => n 
      case Rel(e, r, m) => Rel(e, r, m.distinctElemsDeep)
    Model(es) 

  /** A new Model with recursive de-duplication of its attributes by type on all levels. If duplicate AttrType is found on same level then last attribute is kept.  */
  def distinctAttrTypeDeep: Model =
    val es = distinctTopAttrType.elems.map:
      case n: Node => n 
      case Rel(e, r, m) => Rel(e, r, m.distinctAttrTypeDeep)
    Model(es) 

  /** Recursive sorting of elems using Ordering[Elem]. The default ordering is Elem.elemOrd which gives lexicographic sorting. */  
  def sorted(using Ordering[Elem]): Model = 
    val es = elems.map:
      case n: Node => n 
      case Rel(e, r, m) => Rel(e, r, m.sorted)  // recur
    Model(es.sorted) // sorted is using Elem.elemOrd implicitly

  /** All empty relations at any depth are replaced by its entity. */
  def removeEmptyRelationsDeep: Model =
    Model(elems.map:
      case n: Node => n 
      case Rel(e, r, m) => if m.elems.nonEmpty then Rel(e, r, m.removeEmptyRelationsDeep) else e
    )
  
  /** A Map that groups equal links to gether. Keys of type Link point to Vector[Rel]. */
  def groupByLink: Map[Link | Elem, Vector[Elem]] = elems.groupBy: 
      case Rel(e, rt, sub) => Link(e, rt)
      case e => e

  /** Recursively append submodels of relations that have same link. */
  def appendEqualLinks: Model =  
    val ess: Iterable[Vector[Elem]] = groupByLink.map:
      case (Link(e, rt), xs) => 
        val merged: Rel = 
          xs.asInstanceOf[Vector[Rel]].reduceLeft: (r1, r2) => 
            Rel(r1.e, r1.t, (r1.sub :++ r2.sub).appendEqualLinks)
        Vector(merged)
      case (e, xs) => xs

    Model(ess.flatten.toVector)

  /** A model with merged relations, no empty relations and no duplicate entities. */
  def compact = Model()
    .addAll(this)
    .removeEmptyRelationsDeep.distinctElemsDeep.removeEntIfLinkedDeep.distinctAttrTypeDeep

  /** A Model in normal form is compact and sorted */
  def normal: Model = compact.sorted

  /** All distinct leaf nodes and single elem relations for each branch. */
  def atoms: Vector[Elem] = //paths.flatMap(_.toModel.elems)
    def loop(m: Model): Vector[Elem] = m.elems.flatMap:
      case e: Ent => Vector(e)
      case r@Rel(e,l,sub) => 
        if sub.elems.isEmpty then Vector(r) 
        else sub.tip.elems.map(n => Rel(e,l,Model(n))) ++ loop(sub)
      case a: Attr[_] => Vector(a)
    loop(self).distinct  // todo: ska atoms vara distinct?

  /** A model split into leaf nodes and single elem relations for each branch. */
  def atomic: Model = Model(atoms) 

  /** A model with each relation split for each sub-elem. */
  def split: Model = self.paths.toModel

  /** A model with distinct elems and each relation joined by addAll. */
  def join: Model = Model().addAll(this).distinctElemsDeep

  /** True if this model is in normal form. */
  def isNormal: Boolean = self == normal

  /** A Model with the nodes but not relations at the top of this Model. */
  def tip: Model = cut(0)
  
  /** A Model with the tip of this Model and the tip of its sub-models. */
  def top: Model = cut(1)

  /** A Model with all its top-level submodels. */ 
  def sub: Model =
    elems.collect { case Rel(e, rt, sub) => sub }.foldLeft(Model())(_ :++ _)
  
  /** A Map from each link to a model with all submodels of that link. */
  def linkMapOf(et: EntType): Map[Link, Model] = 
    val ls: Seq[(Link, Model)]  = elems.collect { case Rel(e, rt, sub) if e.t == et => Link(e, rt) -> sub }
    val grouped: Map[Link, Seq[(Link, Model)]] = ls.groupBy(_._1)
    val merged: Map[Link, Model] = grouped.map((l, xs) => l -> xs.map(_._2).foldLeft(Model())(_ :++ _))
    merged

  /** Cut all relations so that no relations is deeper than depth. cut(0) == tip, cut(1) == top */
  def cut(depth : Int): Model = 
    if depth <= 0 then  Model(elems.map { case n: Node => n case Rel(e, _, _) => e }) 
    else Model(elems.map { case n: Node => n case Rel(e, r, m) => Rel(e, r, m.cut(depth - 1)) })

  /** A Model with elems deeply filtered according to a selection expression. */
  infix def keep(s: Selection.Expr): Model = Selection.select(s, this)

  /** A sub-model of Link */
  def /(link: Link): Model = self / LinkPath(Vector(link))

  /** A deep sub-model recursing into a sequence of links in a LinkPath. */
  def /(p: LinkPath): Model = 
    p.links match
      case Vector() => self

      case Vector(link) => 
        val ms: Vector[Model] = elems.collect{ case r: Rel if r.e == link.e && r.t == link.t => r.sub}
        ms.foldLeft(Model())(_ :++ _)

      case Vector(link, rest*) => 
        val m2 = self / link
        m2 / LinkPath(rest.toVector)

  def /[T](a: Attr[T]): Boolean = elems.exists(_ == a)

  def /[T](at: AttrType[T]): Vector[T] = elems.collect{
    case a: Attr[?] if !a.isInstanceOf[Undefined[?]] && a.t == at => a.value.asInstanceOf[T]
  }

  def /(e: Ent): Boolean = elems.exists(_ == e)

  def /(et: EntType): Vector[String] = elems.collect{case e: Ent if e.t == et => e.id}

  def /[T](u: Undefined[T]): Vector[Undefined[T]] = elems.collect{case Undefined(at) if u.t == at => u} 

  def /(ut: Undefined.type): Vector[Undefined[?]] = elems.collect{case u: Undefined[?] => u} 

  def /[T](p: AttrTypePath[T]): Vector[T] =  self / LinkPath(p.links) / p.dest

  def /[T](p: AttrPath[T]): Boolean =  self / LinkPath(p.links) / p.dest

  def /[T](p: EntPath): Boolean =  self / LinkPath(p.links) / p.dest

  def /[T](p: EntTypePath): Vector[String] =  self / LinkPath(p.links) / p.dest

  /** All paths to leaf elems. */
  def paths: Vector[Path] = 
    val pb = collection.mutable.ArrayBuffer.empty[Path]

    def recur(links: Vector[Link], m: Model): Unit =
      for e <- m.elems do e match
        case a: Attr[?] => pb.append(AttrPath(links, a))
        case e: Ent => pb.append(EntPath(links, e))
        case Rel(e, rt, sub) if sub.elems.isEmpty => pb.append(LinkPath(links :+ Link(e, rt))) 
        case Rel(e, rt, sub) => recur(links :+ Link(e, rt), sub) 
    end recur
    recur(Vector(), self)
    pb.toVector

  /** All attribute paths to attributes of type at. */
  def pathsOf[T](at: AttrType[T]): Vector[AttrPath[T]] = 
    val pb = collection.mutable.ArrayBuffer.empty[AttrPath[T]]

    def recur(links: Vector[Link], m: Model): Unit =
      for e <- m.elems do 
        e match
          case a: StrAttr if a.t == at => pb.append(AttrPath(links, a.asInstanceOf[Attr[T]]))
          case a: IntAttr if a.t == at => pb.append(AttrPath(links, a.asInstanceOf[Attr[T]]))
          case Rel(e, rt, sub) => recur(links :+ Link(e, rt), sub) 
          case _ => 
    end recur
    recur(Vector(), self)
    pb.toVector

  /** A model with concatenated paths fragments of n last path links. */
  def bottom(n: Int): Model = paths.map(_.takeLinksRight(n)).toModel

  /** A model with all leaf elems */
  lazy val leafs: Model = bottom(0)

  /** A model with all leaf relations */
  lazy val leafRels: Model = Model(bottom(1).elems.collect{ case r: Rel => r})

  /** A new model with only leaf relations to attributes of type at. */
  def leafRelsOf[T](at: AttrType[T]): Model = 
    Model(leafRels.elems.collect{ case r: Rel if r.sub.elems.exists(e => e.t == at) => r})

  /** A new model with only leaf relations from entity et. */
  def leafRelsOf(et: EntType): Model = 
    Model(leafRels.elems.collect{ case r: Rel if r.e.t == et => r})

  /** A model with all adjacent Text string values joined with newline into one string. */
  def concatAdjacentText = Model(elems.concatAdjacent(Text, "\n"))

  /** A model with all leading and trailing empty Text attributes dropped */
  def trim: Model = 
    extension (es: Vector[Elem]) def dropWhileEmptyText: Vector[Elem] = 
      es.dropWhile:
        case StrAttr(Text, s) if s.trim.isEmpty => true
        case _ => false

    // optimize this as double reverse is inefficient ...
    Model(elems.reverse.dropWhileEmptyText.reverse.dropWhileEmptyText) 

  def toMarkdown: String =
    // TODO turn tweaks below into default args or context using MarkdownSettings
    val MaxLen = 72  // Limit for creating one-liners in pretty markdown; TODO: should this be hardcoded???
    val isDetectOneLiner = true  // perhaps not allow one liner un-parsing as it is non-regular???
    val isInsertColon = true

    val colonOpt = if isInsertColon then ":" else ""
    val sb = StringBuilder()

    def recur(level: Int, m: Model): Unit = 
      val indent = "  " * level 
      for e <- m.elems do e match
        case Undefined(at) => 
          sb.append(s"$indent* $at\n")

        case a: Attr[?] => 
          val trimmed = a.value.toString.trim
          val formattedValue = 
            if !trimmed.hasNewline then trimmed else "\n" + trimmed.trimIndent(level * 2 + 2)

          if a.t == Text then sb.append(s"$indent$formattedValue\n") 
          else if a.t == Title && formattedValue.startsWith("#") then 
            sb.append(s"$indent$formattedValue\n")  //never colon after title
          else sb.append(s"$indent* ${a.t}$colonOpt $formattedValue\n")
        
        case e: Ent => 
          sb.append(s"$indent* ${e.t}$colonOpt ${e.id}\n")
        
        case Rel(e, rt, Model(Vector(e2: Ent))) 
          if isDetectOneLiner && !e.id.contains('\n') && e.id.length + indent.length < MaxLen 
          => // a one-liner
            sb.append(s"$indent* ${e.t}$colonOpt ${e.id} ${rt.toString.deCapitalize} ${e2.t}$colonOpt ${e2.id}\n")
        
        case Rel(e, rt, Model(Vector(u: Undefined[?]))) 
          if isDetectOneLiner 
          => // a one-liner
          sb.append(s"$indent* ${e.t}$colonOpt ${e.id} ${rt.toString.deCapitalize} ${u.t}\n")
        
        case Rel(e, rt, Model(Vector(a: Attr[?]))) 
          if isDetectOneLiner && !a.value.toString.contains('\n') && a.value.toString.length + indent.length < MaxLen 
          => // a one-liner
            sb.append(s"$indent* ${e.t}$colonOpt ${e.id} ${rt.toString.deCapitalize} ${a.t}$colonOpt ${a.value}\n")
        
        case Rel(e, rt, sub) => // put sub on indented new line 
          sb.append(s"$indent* ${e.t}$colonOpt ${e.id} ${rt.toString.deCapitalize}\n")
          if sub.elems.length > 0 then recur(level + 1, sub)
    end recur

    recur(0, self)
    sb.toString
  end toMarkdown

  def md = toMarkdown

  def showDense: String = elems.map(_.show).mkString("Model(",",",")")
  def showLines: String = elems.map(_.show).mkString("Model(\n  ",",\n  ","\n)")

  override def toString: String = elems.mkString("Model(",",",")")
