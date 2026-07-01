package reqt

/** https://graphviz.org/ */ 
object GraphvizGen:
  /** https://graphviz.org/docs/attrs/rankdir/ */
  enum RankDir { case LR, RL, TB, BT}

  /** https://graphviz.org/docs/attrs/ordering/ */
  enum EdgeOrdering: 
    case In, Out, Empty
    override def toString = this match
      case In => "in"
      case Out => "out"
      case Empty => ""

  case class GraphvizSettings(
    isFlat: Boolean = false, 
    fontName: String = "Sans",
    fontSize: Int = 10,
    rankDir: RankDir = RankDir.LR,
    edgeOrdering: EdgeOrdering = EdgeOrdering.Out,
    noJustify: Boolean = true,
    // edgeArrowhead: String = "empty", //https://graphviz.org/docs/attr-types/arrowType/
    // nodeShape: String = "record",  //https://graphviz.org/docs/attr-types/shape/
    compound: Boolean = true,
  )
  object GraphvizSettings:
    given default: GraphvizSettings = GraphvizSettings()

  type GraphvizCtx = GraphvizSettings ?=> String

  extension (m: Model)
    def toGraph: GraphvizCtx = 
      if summon[GraphvizSettings].isFlat then modelToGraphFlat(m) else modelToGraphNested(m)

    def toContextDiagram: GraphvizCtx = contextDiagram(m)

  def modelToGraphNested(m: Model): GraphvizCtx = preamble + nestedBody(m) + ending
  def modelToGraphFlat(m: Model): GraphvizCtx = preamble + flatBody(m) + ending

  // --- BEGIN copied from reqT 3.1.7; to be refactored
  def formats: GraphvizCtx = 
    val s = summon[GraphvizSettings]
    import s.*

    val clusterRank = //https://graphviz.org/docs/attrs/clusterrank/
      if isFlat then "" else "clusterrank=local;" 
    
    s"""|compound=true;overlap=false;rankdir=LR;$clusterRank
        |node [fontname="Sans", fontsize=9];
        |edge [fontname="Sans", fontsize=9];
        |""".stripMargin

  def preamble: GraphvizCtx = s"""digraph ${q}reqT.Model${q} { $nl$formats$nl"""
  def ending: String = "\n}"

  def style(elem: Elem): String = elem match
    case Ent(t, id) => s" [label=$q$t$nlLiteral$id$q, shape=box]"
    case Undefined(t) => s" [label=$q$t$nlLiteral ? $q, shape=box, style=rounded]"
    case StrAttr(t, value) =>  s" [label=$q$t$nlLiteral$value$q, shape=box, style=rounded]"
    case IntAttr(t, value) =>  s" [label=$q$t$nlLiteral$value$q, shape=box, style=rounded]"
    case Rel(e, t, sub) => ""
    
  def node(e: Elem, path: Path): String = q + s"${path}/${e}".noQuotes + q

  def singleSubnodeLink(from: Ent, rt: RelType, to: Elem, path: LinkPath): String =
    s"\n//singleSubnodeLink(${from.toString.noQuotes},$rt,${to.toString.noQuotes},${path.toString.noQuotes})\n\n"+
    indent(path.depth) + node(from, path) + style(from) + ";\n" +
    indent(path.depth) + node(to, path / Link(from,rt)) + style(to) + ";\n" +
    indent(path.depth) + node(from, path) + " -> " 
      + node(to, path/Link(from,rt)) + s" [label=$q${rt.show}  $q]" + ";\n"

  def subGraphPre(from: Ent, link: RelType, to: Elem, path: LinkPath): String =
    s"\n//subGraphPre($from,$link,$to,$path)\n\n"+ // add comment to help debug
    indent(path.depth) + node(from, path) + style(from) + ";\n" +
    indent(path.depth) + node(from, path) + " -> " + node(to, path/Link(from,link)) +
    s" [label=$q${link.show}  $q, labeljust=l, lhead=${q}cluster_${from.toString.noQuotes}$q]" + ";\n" +
    indent(path.depth) + s"  subgraph ${q}cluster_${from.toString.noQuotes}$q { \n"


  def nestedBody(m: Model, path: LinkPath = Path.Root): String =
    val xs: Seq[String] = m.elems.collect:
      case n: Node => indent(path.depth) + node(n, path) + style(n) +";\n"
      case Rel(e1,l1,sub) => sub.elems match
        case Vector() => indent(path.depth) + node(e1, path) + style(e1) +";\n"
        case Vector(e2) if e2.isNode => singleSubnodeLink(e1, l1, e2, path)
        case Vector(Rel(e2, _ , Model(Vector()))) => singleSubnodeLink(e1, l1, e2, path)
        case Vector(Rel(e2, l2, sub2)) if sub2.tip.size == 1 =>
          singleSubnodeLink(e1, l1, e2, path) +
          singleSubnodeLink(e2, l2, sub2.tip.elems.head, path/Link(e1,l1)) +
          nestedBody(sub2, path/Link(e1,l1)/Link(e2,l2))
        case _ =>
          subGraphPre(e1, l1, sub.tip.elems.head, path) +
          nestedBody(sub, path/Link(e1,l1))  + indent(path.depth + 1) + "}\n"
    xs.mkString

  def flatNode(elem: Elem): String = elem match {
      case e@Ent(t, id) => s"$q${e.toString.noQuotes}$q [label=$q$t$nlLiteral$id$q, shape=box];\n"
      case Undefined(t) => s" [label=$q$t$nlLiteral ??? $q, shape=box, style=rounded]"
      case a: Attr[_] =>
        val (row1, row2) = (a.t, a.value.toString.noQuotes)
        s"$q${a.toString.noQuotes}$q [label=$q$row1$nlLiteral$row2$q, shape=box, style=rounded];\n"
      case _ => ""
    }

  def flatBody(m: Model): String = m.atoms.map {
    case n: Node => flatNode(n)
    case Rel(from,link, Model(Vector(to))) =>
      flatNode(from) + flatNode(to) +
      s"$q${from.toString.noQuotes}$q" + " -> " + s"$q${to.toString.noQuotes}$q" + s" [label=$link]" + ";\n"
    case _ => ""
  } .mkString

  // --- END old copied on-going refactoring

  // --- below methods are used in reqt.meta.graph 

  def recordNode(
    nodeName: String,
    fontSize: Int = 10, 
    isHtml: Boolean = false,
  )(labelParts: String*): String =
    if isHtml then 
      s"""  $nodeName [ label = <{${labelParts.mkString("|")}}> fontsize = $fontSize]"""
    else
      s"""  $nodeName [ label = "{${labelParts.mkString("|")}}" fontsize = $fontSize]"""

  def edge(fromNode: String, toNode: String): String = s"  $fromNode -> $toNode"

  def classDiagram
    (title: String)
    (rankSame: Seq[Seq[String]])
    (edges: Seq[(String, String)])
    (nodeFormats: Seq[String]) 
    (using settings: GraphvizSettings): String =
      val s = settings.copy(rankDir = RankDir.BT)
      import s.*
      s"""|digraph $title {
          |  fontname = "$fontName"
          |  fontsize = $fontSize
          |  rankdir =  "$rankDir"
          |  ordering = "$edgeOrdering"
          |  nojustify = $noJustify
          |
          |  node [
          |    fontname = "$fontName"
          |    fontsize = $fontSize
          |    shape = "record"
          |  ]
          |
          |  edge [
          |    arrowhead = "empty"
          |  ]
          |
          |${rankSame.map(_.mkString("  { rank = same; ", "; ", "; }")).mkString("  ", "\n  ", "\n")}
          |
          |${nodeFormats.mkString("  ", "\n  ", "\n")}
          |
          |${edges.map((f,t) => edge(f,t)).mkString("  ", "\n  ", "\n")}
          |}
          |""".stripMargin

  def contextDiagram(M: Model) =  //TODO
    """
    /* TODO 
      here is an example to illustrate how to use the a png for nodes in graphviz
      in order to put the label below the actor it needs to be clustered
      inspired by the buggy thing here: https://martin.elwin.com/blog/2008/05/uml-use-case-diagrams-graphviz/
    */
    digraph G {
      rankdir=LR;

      subgraph clusterUser {label="User"; labelloc="b"; peripheries=0; user; };
      user [shapefile="actor.png", peripheries=0, label=""];

      login [label="Log In", shape=ellipse];

      user->login [arrowhead=none];
    }
    """