import sbt._

object Version {
  lazy val akka  = "2.5.25"
  lazy val circe = "0.12.1"
}

object Dependencies {
  lazy val akka              = "com.typesafe.akka"          %% "akka-actor"         % Version.akka
  lazy val `akka-http`       = "com.typesafe.akka"          %% "akka-http"          % "10.1.9"
  lazy val `akka-http-circe` = "de.heikoseeberger"          %% "akka-http-circe"    % "1.29.1"
  lazy val `akka-slf4j`      = "com.typesafe.akka"          %% "akka-slf4j"         % Version.akka
  lazy val `akka-stream`     = "com.typesafe.akka"          %% "akka-stream"        % Version.akka
  lazy val akkatest          = "com.typesafe.akka"          %% "akka-testkit"       % Version.akka % Test
  lazy val akkahttptest      = "com.typesafe.akka"          %% "akka-http-testkit"  % "10.1.10" % Test
  lazy val bcprov            = "org.bouncycastle"           % "bcprov-jdk15on"      % "1.62"
  lazy val `circe-parser`    = "io.circe"                   %% "circe-parser"       % Version.circe
  lazy val `circe-generic`   = "io.circe"                   %% "circe-generic"      % Version.circe
  lazy val curve25519        = "org.whispersystems"         % "curve25519-java"     % "0.5.0"
  lazy val `scala-logging`   = "com.typesafe.scala-logging" %% "scala-logging"      % "3.9.2"
  lazy val scalacheck        = "org.scalacheck"             %% "scalacheck"         % "1.14.0" % Test
  lazy val scalatest         = "org.scalatest"              %% "scalatest"          % "3.0.8" % Test

  def `scala-reflect`(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion
}
