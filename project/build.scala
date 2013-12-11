import sbt._
import sbt.Keys._

object ScalaDCIBuild extends Build {
  val buildVersion = "0.3.1"
  val buildScalaVersion = "2.10.3"
  val buildScalaOrganization = "org.scala-lang"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.github.dci",
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    scalaOrganization := buildScalaOrganization,
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions"), //, "-language:experimental.macros"),
    libraryDependencies ++= Seq("org.scalatest" % "scalatest_2.10" % "2.0.M5b"),
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)
  )

  lazy val scaladci = Project(
    "scaladci",
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in scaladciExamples
    )
  ) aggregate(scaladciCore, scaladciExamples)

  lazy val scaladciCore = Project(
    "scaladci-core",
    file("core"),
    settings = buildSettings ++ Seq(
      name := "ScalaDCI Core",
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)
    )
  )

  lazy val scaladciExamples = Project(
    "scaladci-examples",
    file("examples"),
    settings = buildSettings ++ Seq(
      name := "ScalaDCI Examples",
      libraryDependencies ++= Seq(
        "org.jscala" %% "jscala-macros" % "0.3",
        "org.jscala" %% "jscala-annots" % "0.3"
      )
    )
  ) dependsOn scaladciCore
}
