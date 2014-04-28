import sbt._
import sbt.Keys._

object build extends Build {

  lazy val demo = Project("demo", file("."), settings = buildSettings)

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.yourcompany",
    version := "0.5.0",
    scalaVersion := "2.11.0",
    scalacOptions := Seq("-unchecked", "-deprecation"),
    resolvers ++= Seq(Resolver.sonatypeRepo("releases")),
    libraryDependencies ++= Seq(
      "org.scaladci" %% "scaladci" % "0.5.1"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full)
  )
}