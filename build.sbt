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

/** tui4s runs in the real terminal: the app gets its own JVM with the native access and the scheduler values of tui4s */
Compile / run / fork        := true
Compile / run / connectInput := true
Compile / run / javaOptions ++= Seq(
  "--enable-native-access=ALL-UNNAMED",
  "-Dkyo.scheduler.cycleIntervalNs=10000000",
  "-Dkyo.scheduler.blockingMonitorIntervalNs=20000000",
  "-Dkyo.scheduler.blockingMonitorMinIntervalNs=20000000",
  "-Dkyo.scheduler.regulator.concurrencyCollectIntervalMs=100"
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
  /** tui4s: terminal UI runner, app and testkit */
  "io.github.yuri4n"  %% "tui4s-runner"            % "0.1.1-RC1",
  "io.github.yuri4n"  %% "tui4s-app"               % "0.1.1-RC1",
  "io.github.yuri4n"  %% "tui4s-widgets"           % "0.1.1-RC1",
  "io.github.yuri4n"  %% "tui4s-use"               % "0.1.1-RC1",
  "io.github.yuri4n"  %% "tui4s-testkit"           % "0.1.1-RC1" % Test,
  /** colored & informative output */
  "com.lihaoyi"       %% "pprint"                  % "0.9.6"
)
