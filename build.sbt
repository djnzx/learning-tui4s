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

/** Lanterna needs the real terminal: `sbt run` forks the app and hands it stdin and stdout, so the app owns the terminal */
run / fork := true
run / connectInput := true
run / outputStrategy := Some(StdoutOutput)

enablePlugins(JavaAppPackaging)

libraryDependencies ++= Seq(
  /** Lanterna: terminal screen and key input */
  "com.googlecode.lanterna" % "lanterna" % "3.1.5",
  /** testing */
  "org.scalatest" %% "scalatest"      % "3.2.20"
)
