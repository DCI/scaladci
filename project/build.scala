import sbt._
import sbt.Keys._

object ScalaDCIBuild extends Build {
  val buildScalaVersion = "2.10.0-RC1"

  val commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.github.dci",
    version := "0.1-SNAPSHOT",
    scalaVersion := buildScalaVersion,
    scalacOptions := Seq(
      "-unchecked", "-deprecation", "-feature"
      , "-language:implicitConversions"
      , "-language:experimental.macros"
    ),
    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.10.0-RC1" % "2.0.M4",
      "org.scala-lang" % "scala-actors" % buildScalaVersion,
      "org.specs2" % "specs2_2.10.0-RC1" % "1.12.2",
      "org.scalaj" % "scalaj-time_2.9.2" % "0.6"
    ),
    resolvers += Classpaths.typesafeSnapshots
  )

  lazy val scaladci = Project(
    id = "scaladci",
    base = file("."),
    settings = commonSettings
  ) aggregate (scaladciCore)

  lazy val scaladciCore = Project(
    id = "scaladci-core",
    base = file("core"),
    settings = commonSettings
      ++ Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % buildScalaVersion,
        "org.scala-lang" % "scala-compiler" % buildScalaVersion
      )
    )
  )

  lazy val scaladciExamples = Project(
    id = "scaladci-examples",
    base = file("examples"),
    settings = commonSettings
  ) dependsOn (scaladciCore)
}
