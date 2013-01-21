import sbt._
import sbt.Keys._

object ScalaDCIBuild extends Build {
  val buildVersion           = "0.2-SNAPSHOT"
  val buildScalaVersion      = "2.11.0-SNAPSHOT"
  val buildScalaOrganization = "org.scala-lang.macro-paradise"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.github.dci",
    version := buildVersion,
    scalaVersion := buildScalaVersion,
    scalaOrganization := buildScalaOrganization,
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions", "-language:experimental.macros"),
    libraryDependencies ++= Seq("org.scalatest" % "scalatest_2.10" % "2.0.M5b"),
    resolvers += Resolver.sonatypeRepo("releases"),
    resolvers += Resolver.sonatypeRepo("snapshots")
  )

  lazy val scaladci = Project(
    "scaladci",
    file("."),
    settings = buildSettings
  ) aggregate(scaladciCore, scaladciExamples)

  lazy val scaladciCore = Project(
    "scaladci-core",
    file("core"),
    settings = buildSettings ++ Seq(
      name := "ScalaDCI Core",
      libraryDependencies += buildScalaOrganization % "scala-reflect" % buildScalaVersion
    )
  )

  lazy val scaladciExamples = Project(
    "scaladci-examples",
    file("examples"),
    settings = buildSettings ++ Seq(
      name := "ScalaDCI Examples")
  ) dependsOn (scaladciCore)
}