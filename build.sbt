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

/** TermFlow: Elm-architecture terminal UI (the app layer with the terminal and the screen, without the widgets) */
lazy val termflow = (project in file("termflow"))
  .dependsOn(core)
  .settings(commonSettings, forkedRun)
  .settings(
    libraryDependencies += "org.llm4s" %% "termflow-app" % "0.4.0"
  )

/** jatatui: Java port of ratatui (core, widgets and the crossterm backend, JNI to Rust, needs native access); the app runs on cats-effect and fs2 */
lazy val jatatui = (project in file("jatatui"))
  .dependsOn(core)
  .settings(commonSettings, forkedRun)
  .settings(
    run / javaOptions += "--enable-native-access=ALL-UNNAMED",
    libraryDependencies ++= Seq(
      "com.olvind.jatatui" % "jatatui-core"      % "0.31.0",
      "com.olvind.jatatui" % "jatatui-widgets"   % "0.31.0",
      "com.olvind.jatatui" % "jatatui-crossterm" % "0.31.0",
      "org.typelevel"     %% "cats-effect"       % "3.7.1",
      "co.fs2"            %% "fs2-core"          % "3.14.0"
    )
  )

lazy val root = (project in file("."))
  .aggregate(core, lanterna, termflow, jatatui)
  .settings(
    name           := "learning-tui4s",
    publish / skip := true
  )
