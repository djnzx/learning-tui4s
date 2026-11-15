import scala.collection.Seq

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version      := "0.1.0"
ThisBuild / scalaVersion := "3.9.0"

lazy val commonSettings = Seq(
  javacOptions := Seq("-source", "25", "-target", "25"),
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:postfixOps",
    "-language:higherKinds",
    "-language:existentials",
    "-Wconf:msg=exhaustive:error",
    "-Wunused:all",
    //  "-Xfatal-warnings",
    "-Wvalue-discard"
  ),
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.20" % Test
)

/** The apps run in the real terminal: sbt starts them in their own JVM (`runMain` of a module). */
lazy val forkedRun = Seq(
  run / fork := true
)

/** The pure part shared by the four implementations: the model of the folder and the text of the screen. No library dependency. */
lazy val core = (project in file("core"))
  .settings(commonSettings)

/** Lanterna: terminal screen and key input */
lazy val lanterna = (project in file("lanterna"))
  .dependsOn(core)
  .settings(commonSettings, forkedRun)
  .settings(
    libraryDependencies += "com.googlecode.lanterna" % "lanterna" % "3.1.5"
  )

lazy val root = (project in file("."))
  .aggregate(core, lanterna)
  .settings(
    name           := "learning-tui4s",
    publish / skip := true
  )
