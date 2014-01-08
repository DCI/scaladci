import sbt._
import sbt.Keys._

object build extends Build with BuildSettings {

  lazy val root = Project(
    "root",
    file("."),
    settings = buildSettings ++ Seq(packagedArtifacts := Map.empty)
  ) aggregate(core, examples)

  lazy val core = Project(
    "scaladci",
    file("core"),
    settings = buildSettings
  )

  lazy val examples = Project(
    "scaladci-examples",
    file("examples"),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "org.jscala" %% "jscala-macros" % "0.3",
        "org.jscala" %% "jscala-annots" % "0.3"
      ),
      packagedArtifacts := Map.empty
    )
  ) dependsOn core
}

trait BuildSettings extends Publishing {

  lazy val majorVersion = "0.4.1"
//  lazy val versionFormat = "%s-SNAPSHOT"
  lazy val versionFormat = "%s"

  val buildSettings = Defaults.defaultSettings ++ publishSettings ++ Seq(
    organization := "com.marcgrue",
    version := versionFormat format majorVersion,
    scalaVersion := "2.10.3",
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature"),
    resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.specs2" %% "specs2" % "2.3.7" % "test"
      ),
      addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0-M2" cross CrossVersion.full)
  )
}

trait Publishing {
  val snapshots = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
  val releases  = "Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"

  lazy val publishSettings = Seq(
    publishMavenStyle := true,
    publishTo <<= version((v: String) => Some(if (v.trim endsWith "SNAPSHOT") snapshots else releases)),
    publishArtifact in Test := false,
    pomIncludeRepository := (_ => false),
    pomExtra := projectPomExtra
  )

  val projectPomExtra =
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
