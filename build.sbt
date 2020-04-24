import Dependencies._

Global / cancelable := true // Allow cancelation of forked task without killing SBT

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

def baseProject(id: String): Project = {
  Project(id, file(id))
    .settings(commonSettings: _*)
}

val scalastyleCfgFile     = "project/scalastyle-config.xml"
val scalastyleTestCfgFile = "project/scalastyle-test-config.xml"

lazy val root: Project = Project("alephium-scala-common", file("."))
  .settings(commonSettings: _*)
  .settings(
    // This is just a project to aggregate modules, nothing to compile or to check scalastyle for.
    unmanagedSourceDirectories := Seq(),
    scalastyle := {},
    scalastyle in Test := {}
  )
  .aggregate(util, macros, serde, crypto, rpc)

def subProject(path: String): Project = {
  baseProject(path)
    .settings(
      Compile / scalastyleConfig := root.base / scalastyleCfgFile,
      Test / scalastyleConfig := root.base / scalastyleTestCfgFile
    )
}

lazy val crypto = subProject("crypto")
  .dependsOn(util % "test->test;compile->compile", serde)

lazy val rpc = subProject("rpc")
  .settings(
    libraryDependencies ++= Seq(
      `akka-http`,
      `akka-http-circe`,
      `akka-stream`,
      `circe-parser`,
      `circe-generic`,
      `scala-logging`,
      akkatest,
      akkahttptest
    )
  )
  .dependsOn(util % "test->test;compile->compile")

lazy val serde = subProject("serde")
  .settings(
    Compile / sourceGenerators += (sourceManaged in Compile).map(Boilerplate.genSrc).taskValue,
    Test / sourceGenerators += (sourceManaged in Test).map(Boilerplate.genTest).taskValue
  )
  .dependsOn(util % "test->test;compile->compile")

lazy val util = subProject("util")
  .dependsOn(macros)
  .settings(
    publishArtifact in Test := true,
    libraryDependencies ++= Seq(
      akka,
      `akka-slf4j`,
      bcprov,
      `scala-reflect`(scalaVersion.value)
    )
  )

lazy val macros = subProject("macros")
  .settings(
    libraryDependencies += `scala-reflect`(scalaVersion.value),
    wartremoverErrors in (Compile, compile) := Warts.allBut(
      wartsCompileExcludes :+ Wart.AsInstanceOf: _*)
  )

val commonSettings = Seq(
  organization := "org.alephium",
  version := "0.3.0-SNAPSHOT",
  scalaVersion := "2.12.10",
  parallelExecution in Test := false,
  scalacOptions ++= Seq(
//    "-Xdisable-assertions", // TODO: use this properly
    "-deprecation",
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-feature",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint:adapted-args",
    "-Xlint:by-name-right-associative",
    "-Xlint:constant",
    "-Xlint:delayedinit-select",
    "-Xlint:doc-detached",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:missing-interpolator",
    "-Xlint:nullary-override",
    "-Xlint:nullary-unit",
    "-Xlint:option-implicit",
    "-Xlint:package-object-classes",
    "-Xlint:poly-implicit-overload",
    "-Xlint:private-shadow",
    "-Xlint:stars-align",
    "-Xlint:type-parameter-shadow",
    "-Xlint:unsound-match",
    "-Yno-adapted-args",
    "-Ypartial-unification",
    "-Ywarn-dead-code",
    "-Ywarn-extra-implicit",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused:implicits",
    "-Ywarn-unused:imports",
    "-Ywarn-unused:locals",
    "-Ywarn-unused:params",
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:privates",
    "-Ywarn-value-discard"
  ),
  wartremoverErrors in (Compile, compile) := Warts.allBut(wartsCompileExcludes: _*),
  wartremoverErrors in (Test, test) := Warts.allBut(wartsTestExcludes: _*),
  fork := true,
  Test / scalacOptions += "-Xcheckinit",
  Test / javaOptions += "-Xss2m",
  Test / envVars += "ALEPHIUM_ENV" -> "test",
  run / javaOptions += "-Xmx4g",
  libraryDependencies ++= Seq(
    akkatest,
    scalacheck,
    scalatest,
    scalatestplus,
  )
)

val wartsCompileExcludes = Seq(
  Wart.MutableDataStructures,
  Wart.Var,
  Wart.Overloading,
  Wart.NonUnitStatements,
  Wart.Nothing,
  Wart.Return, // Covered by scalastyle
  Wart.Any,
  Wart.Equals
)

val wartsTestExcludes = wartsCompileExcludes ++ Seq(
  Wart.PublicInference,
  Wart.OptionPartial
)
