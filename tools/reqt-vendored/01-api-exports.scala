package reqt

// exports to shape the surface api

export Model.{toModel, concatAdjacent} // extension methods on Seq[Elem]

export Show.show   // extension for pretty Model using enum types and apply 
export Selection.* // and/or-expressions for selecting Model parts 
export Path.`/` // path factories for slash notation on Model

export MarkdownParser.md       // string interpolator to parse markdown Model
export MarkdownParser.toModel // string extension to parse markdown Model

export HtmlGen.{toHtml, toHtmlBody}

export GraphvizGen.{toGraph, toContextDiagram}

export StringUtils.* // general extensions only depending on Scala stdlib

export meta.{help, ?, RelGroup, EntGroup}  // to explore concepts

export csp.* // types and extensions for expressing constraint satisfaction problems
export parseConstraints.toConstr // extension to parse String and StrAttr(value)


