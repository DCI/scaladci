import sbt._
import sbt.Keys._

object build extends Build with BuildSettings {
  lazy val root                = project in file(".") settings (_root: _*) aggregate(scaladci, `scaladci-examples`)
  lazy val scaladci            = project in file("core") settings (_core: _*)
  lazy val `scaladci-examples` = project in file("examples") settings (_examples: _*) dependsOn scaladci
}

trait BuildSettings extends Publishing {

  lazy val shared = Defaults.defaultSettings ++ publishSettings ++ Seq(
    organization := "org.scaladci",
    version := "0.5.1",
    scalaVersion := "2.11.0",
    scalacOptions := Seq("-unchecked", "-deprecation", "-feature"),
    resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
    libraryDependencies += "org.specs2" %% "specs2" % "2.3.11" % "test",
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full)
  )

  lazy val _root     = shared :+ (packagedArtifacts := Map.empty)
  lazy val _core     = shared :+ (libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)
  lazy val _examples = shared :+ (packagedArtifacts := Map.empty)
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
