import sbt._
import sbt.Keys._

object ScalaDCIBuild extends Build {

  lazy val scaladci = Project("scaladci", file(".")) aggregate (scaladciCore)

  lazy val scaladciCore = Project(
    id = "scaladci-core",
    base = file("core"),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect" % "2.10.0-RC1",
        "org.scala-lang" % "scala-compiler" % "2.10.0-RC1"
      )
    )
  )

  lazy val scaladciExamples = Project(
    id = "scaladci-examples",
    base = file("examples"),
    settings = commonSettings
  ) dependsOn (scaladciCore)

  def commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.github.dci",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.10.0-RC1",
    scalacOptions := Seq(
      "-unchecked", "-deprecation", "-feature"
      , "-language:implicitConversions"
      , "-language:experimental.macros"
    ),
    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.10.0-RC1" % "2.0.M4",
      "org.scala-lang" % "scala-actors" % "2.10.0-RC1",
      "org.specs2" % "specs2_2.10.0-RC1" % "1.12.2",
      "org.scalaj" % "scalaj-time_2.9.2" % "0.6"
    ),
    resolvers += Classpaths.typesafeSnapshots
  )
}
