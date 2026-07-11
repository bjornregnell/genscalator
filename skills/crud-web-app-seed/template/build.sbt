// Multi-project build: `common` (a pure cross-project shared by both sides) · `server` (JVM, JDK-only) ·
// `client` (Scala.js + Laminar). sbt 1.x (sbt-scalajs is not on sbt 2.x yet). Scala 3.9.0-RC1 (the coming LTS).
ThisBuild / scalaVersion := "3.9.0-RC1"
ThisBuild / organization := "todo"

lazy val common =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("common"))
    .settings(name := "todo-common")

lazy val server =
  project
    .in(file("server"))
    .dependsOn(common.jvm)
    .settings(
      name := "todo-server",
      libraryDependencies += "org.scalameta" %% "munit" % "1.3.4" % Test,
    )

lazy val client =
  project
    .in(file("client"))
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(common.js)
    .settings(
      name := "todo-client",
      scalaJSUseMainModuleInitializer := true,
      libraryDependencies += "com.raquo" %%% "laminar" % "17.2.1",
    )
