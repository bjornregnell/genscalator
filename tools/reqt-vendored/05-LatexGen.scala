package reqt

object LatexGen:
  def defineColors: String = 
    s"""|\\definecolor{entityColor}{RGB}{0,100,200}
        |\\definecolor{stringAttributeColor}{RGB}{180,100,40}
        |\\definecolor{intAttributeColor}{RGB}{0,120,50}
        |\\definecolor{relationColor}{RGB}{160,0,30}
        |""".stripMargin
  
  def lstDefineStyle: String = 
    import meta.{entityNames, relationNames, strAttrNames, intAttrNames}
    s"""|\\lstdefinestyle{reqT}{
        |  %belowcaptionskip=1\\baselineskip,
        |  breaklines=true,
        |  %showstringspaces=false,
        |  showspaces=false,
        |  %breakatwhitespace=true,
        |  basicstyle=\\ttfamily\\small,
        |  emph={${entityNames.mkString{","}}},
        |  emphstyle=\\bfseries\\color{entityColor},
        |  emph={[2]${(relationNames ++ relationNames.map(_.capitalize)).mkString{","}}},
        |  emphstyle={[2]\\bfseries\\color{relationColor}},
        |  emph={[3]${strAttrNames.mkString{","}}},
        |  emphstyle={[3]\\bfseries\\color{stringAttributeColor}},  
        |  emph={[4]${intAttrNames.mkString{","}}},
        |  emphstyle={[4]\\bfseries\\color{intAttributeColor}},  
        |}
        |""".stripMargin
