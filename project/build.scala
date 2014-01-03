import sbt._
import sbt.Keys._

object build extends Build {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.github.dci",
    version := "0.4.0",
    scalaVersion := "2.10.3",
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature"),
    resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.specs2" %% "specs2" % "2.3.7" % "test"
    ),
    addCompilerPlugin("org.scala-lang.plugins" % "macro-paradise" % "2.0.0-SNAPSHOT" cross CrossVersion.full)
  )

  lazy val scaladci = Project(
    "root",
    file("."),
    settings = buildSettings
  ) aggregate(scaladciCore, scaladciExamples)

  lazy val scaladciCore = Project(
    "scaladci-core",
    file("core"),
    settings = buildSettings
  )

  lazy val scaladciExamples = Project(
    "scaladci-examples",
    file("examples"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.jscala" %% "jscala-macros" % "0.3",
        "org.jscala" %% "jscala-annots" % "0.3"
      )
    )
  ) dependsOn scaladciCore
}