package reqt

// TODO: Refactor after paste from reqT 3.1.7
// Idea: Use underlying concurrent hashmap to be thread safe (e.g. for Swing) 
//       and Scala Selectable for nice interface 
object Settings {
  var indentSpacing = 2
  var lineLength = 72
  var columnSeparator = ";"
  var rowSeparator = "\n"
  // var defaultModelToString: exporter.StringExporter = exporter.toScalaCompact
  // var defaultModelToTable: exporter.StringExporter = exporter.toPathTable
  // var defaultModelToGraph: exporter.StringExporter = exporter.toGraphVizNested
  // var isGeneratingHtmlRawModel = false
  // var isMarkdownSymbolsInToText = false
  // var defaultTitle: String = "untitled"
  // var defaultModelName: String = defaultTitle+".reqt"
  object colors {
    val entRGB  = (0,100,200) //blueish
    val attrRGB = (0,100,50)  //greenish
    val relRGB  = (160,0,30)  //reddish
    val strRGB  = (200,90,40) //orange-like

    def JCol(t: (Int, Int, Int)) = new java.awt.Color(t._1, t._2, t._3)
    
    val entityColor    = JCol(entRGB)
    val attributeColor = JCol(attrRGB)
    val relationColor  = JCol(relRGB)
    val stringColor    = JCol(strRGB)

    val scalaReservedWordColor = JCol((0,0,125))
  }
  object fonts {
    var preferredFonts = List("DejaVu Sans Mono", "Consolas", "Liberation Mono", "Monospace")
    var preferredFontSize = 14
  }
}