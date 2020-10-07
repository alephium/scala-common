// Copyright 2018 The Alephium Authors
// This file is part of the alephium project.
//
// The library is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// The library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with the library. If not, see <http://www.gnu.org/licenses/>.

import sbt._

object Version {
  lazy val akka        = "2.6.5"
  lazy val circe       = "0.13.0"
  lazy val `akka-http` = "10.1.11"
}

object Dependencies {
  lazy val akka              = "com.typesafe.akka"          %% "akka-actor"        % Version.akka
  lazy val `akka-http`       = "com.typesafe.akka"          %% "akka-http"         % Version.`akka-http`
  lazy val `akka-http-circe` = "de.heikoseeberger"          %% "akka-http-circe"   % "1.32.0"
  lazy val `akka-slf4j`      = "com.typesafe.akka"          %% "akka-slf4j"        % Version.akka
  lazy val `akka-stream`     = "com.typesafe.akka"          %% "akka-stream"       % Version.akka
  lazy val akkatest          = "com.typesafe.akka"          %% "akka-testkit"      % Version.akka % Test
  lazy val akkahttptest      = "com.typesafe.akka"          %% "akka-http-testkit" % Version.`akka-http` % Test
  lazy val bcprov            = "org.bouncycastle"           % "bcprov-jdk15on"     % "1.64"
  lazy val `circe-parser`    = "io.circe"                   %% "circe-parser"      % Version.circe
  lazy val `circe-generic`   = "io.circe"                   %% "circe-generic"     % Version.circe
  lazy val `scala-logging`   = "com.typesafe.scala-logging" %% "scala-logging"     % "3.9.2"
  lazy val scalacheck        = "org.scalacheck"             %% "scalacheck"        % "1.14.3" % Test
  lazy val scalatest         = "org.scalatest"              %% "scalatest"         % "3.1.1" % Test
  lazy val scalatestplus     = "org.scalatestplus"          %% "scalacheck-1-14"   % "3.1.1.1" % Test

  def `scala-reflect`(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion
}
