//> using scala 3.9.0-RC4
//> using javaOpt -Dstdout.encoding=UTF-8
//> using javaOpt -Dstderr.encoding=UTF-8

// project.scala — the SINGLE SOURCE of the toolbox's Scala version. scala-cli's conventional name for
// project-wide using-directives, so the role is legible to any Scala reader. Every tool file carries
// `//> using file project.scala` instead of naming a version, so a bump is ONE edit here instead of one
// per tool (the 3.8.4 -> 3.9.0-RC4 bump touched 78 sites; this is the fix for that).
//
// WHY the tools include it EXPLICITLY, rather than relying on the conventional pickup: the `tt`
// launcher's scala-cli fallback runs ONE tool file (`scala-cli run tools/<tool>.scala`), and that build
// unit does NOT contain the rest of the directory. Whole-directory builds (`scala-cli test tools`, the
// native package) pick this file up on their own; the explicit include is what makes the single-file
// path agree with them. Verified live, not assumed — see ScalaVersionSuite and the commit that added it.
//
// Deliberately NO code: this file exists for its directives alone, so including it into any tool adds a
// version and an output encoding, and nothing else. It has no `@main`, so it is not a `tt` verb.
//
// WHY the encoding directives live here: on Windows System.out follows the console code page, so any
// tool printing a non-ASCII glyph produced mojibake (`ö` as a lone cp1252 byte, `→` flattened to `?`).
// Every tool includes this file, so one edit covers the whole scala-cli path — single-file launcher
// fallback and whole-directory builds alike. The NATIVE binary takes no -D at run time and is fixed
// separately, in dispatch.scala's driver. Reading needs nothing: JDK 18+ already defaults to UTF-8.
//
// ⚠ The MAINLESS HELPERS (lib, seqspec, boxstats, minijson, limitstore, secrets, mdparse) deliberately
// do NOT include this file. They are themselves included by tools, and scala-cli does not support
// CHAINING `using file`: the second hop is dropped with a warning on every build — which additionally
// breaks CliSuite's empty-stderr contract. They inherit the version from whichever tool includes them,
// so nothing is lost. ScalaVersionSuite asserts that rule in both directions.
//
// Policy lives in prose in README.md (track the latest LTS Scala, bleeding edge — an RC counts); the
// version itself lives HERE, in the one directive above.
