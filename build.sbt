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

/** the app runs in the real terminal: it gets its own JVM with the native access that crossterm needs */
Compile / run / fork        := true
Compile / run / connectInput := true
Compile / run / javaOptions ++= Seq(
  "--enable-native-access=ALL-UNNAMED"
)

libraryDependencies ++= Seq(
  /** basic category things */
  "org.typelevel"     %% "cats-core"               % "2.13.0",
  /** effects */
  "org.typelevel"     %% "cats-effect"             % "3.7.1",
  "co.fs2"            %% "fs2-io"                  % "3.14.0",
  /** json serialization */
  "io.circe"          %% "circe-parser"            % "0.14.17",
  "io.circe"          %% "circe-generic-extras"    % "0.14.5-RC1",
  /** http */
  "org.http4s"        %% "http4s-dsl"              % "0.23.38",
  "org.http4s"        %% "http4s-circe"            % "0.23.38",
  "org.http4s"        %% "http4s-blaze-server"     % "0.23.18",
  "org.http4s"        %% "http4s-blaze-client"     % "0.23.18",
  /** testing */
  "org.scalatest"     %% "scalatest"               % "3.2.20",
  "org.scalacheck"    %% "scalacheck"              % "1.20.0",
  "org.scalatestplus" %% "scalacheck-1-20"         % "3.2.20.0",
  "org.mockito"       %% "mockito-scala-scalatest" % "2.2.3",
  /** jatatui: Java port of ratatui (core, widgets and the crossterm backend) */
  "com.olvind.jatatui" % "jatatui-core"      % "0.31.0",
  "com.olvind.jatatui" % "jatatui-widgets"   % "0.31.0",
  "com.olvind.jatatui" % "jatatui-crossterm" % "0.31.0",
  /** colored & informative output */
  "com.lihaoyi"       %% "pprint"                  % "0.9.6"
)
