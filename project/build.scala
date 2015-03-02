import sbt._
import sbt.Keys._

object ScalaDciBuild extends Build with Publishing {

  lazy val scaladci = Project(
    id = "scaladci",
    base = file("."),
    aggregate = Seq(scaladciCore, scaladciCoretest, scaladciExamples),
    settings = commonSettings ++ Seq(
      moduleName := "scaladci-root",
      packagedArtifacts := Map.empty
    )
  )

  lazy val scaladciCore = Project(
    id = "scaladci-core",
    base = file("core"),
    settings = commonSettings ++ publishSettings ++ Seq(
      moduleName := "scaladci"
    )
  )

  lazy val scaladciCoretest = Project(
    id = "scaladci-coretest",
    base = file("coretest"),
    dependencies = Seq(scaladciCore),
    settings = commonSettings ++ Seq(
      packagedArtifacts := Map.empty
    )
  )

  lazy val scaladciExamples = Project(
    id = "scaladci-examples",
    base = file("examples"),
    dependencies = Seq(scaladciCore),
    settings = commonSettings ++ Seq(
      packagedArtifacts := Map.empty
    )
  )

  lazy val commonSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "org.scaladci",
    version := "0.5.2",
    scalaVersion := "2.11.6",
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature"),
    resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.specs2" %% "specs2" % "2.4.11" % "test"
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)
  )
}


trait Publishing {
  lazy val snapshots = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
  lazy val releases  = "Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"

  lazy val publishSettings = Seq(
    publishMavenStyle := true,
    publishTo <<= version((v: String) => Some(if (v.trim endsWith "SNAPSHOT") snapshots else releases)),
    publishArtifact in Test := false,
    pomIncludeRepository := (_ => false),
    pomExtra := projectPomExtra
  )

  lazy val projectPomExtra =
    <url>https://github.com/dci/scaladci</url>
      <licenses>
        <license>
          <name>Apache License</name>
          <url>http://www.apache.org/licenses/</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:dci/scaladci.git</url>
        <connection>scm:git:git@github.com:dci/scaladci.git</connection>
      </scm>
      <developers>
        <developer>
          <id>marcgrue</id>
          <name>Marc Grue</name>
          <url>http://marcgrue.com</url>
        </developer>
      </developers>
}
