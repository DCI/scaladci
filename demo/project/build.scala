import sbt._
import sbt.Keys._

object build extends Build {

  lazy val demo = Project("demo", file("."), settings = buildSettings)

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.yourcompany",
    version := "0.4.1",
    scalaVersion := "2.10.3",
    scalacOptions := Seq("-unchecked", "-deprecation"),
    resolvers ++= Seq(Resolver.sonatypeRepo("releases")), //, Resolver.sonatypeRepo("snapshots")),
    libraryDependencies ++= Seq(
      "com.marcgrue" %% "scaladci" % "0.4.1"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0-M2" cross CrossVersion.full)
  )
}