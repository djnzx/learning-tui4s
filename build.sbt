import scala.collection.Seq

Global / onChangedBuildSource := ReloadOnSourceChanges

name := "learning-tui4s"
version := "0.1.0"

scalaVersion := "3.9.0"

javacOptions := Seq("-source", "25", "-target", "25")

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
)

/** TermFlow needs the real terminal: `sbt run` has no tty, so the app is started from the staged script (`sbt stage`) */
enablePlugins(JavaAppPackaging)

Universal / javaOptions ++= Seq("-J--enable-native-access=ALL-UNNAMED")

libraryDependencies ++= Seq(
  /** TermFlow: Elm-architecture terminal UI */
  "org.llm4s"     %% "termflow"       % "0.4.0",
  /** testing */
  "org.scalatest" %% "scalatest"      % "3.2.20"
)
