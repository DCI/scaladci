
lazy val commonSettings = Defaults.coreDefaultSettings ++ Seq(
  organization := "org.scaladci",
  version := "0.5.6",
  scalaVersion := "2.12.2",
  crossScalaVersions := Seq("2.11.8", "2.12.2"),
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature"),
  resolvers ++= Seq(Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots")),
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.specs2" %% "specs2" % "2.4.17" % "test"
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val scaladci = Project(
  id = "scaladci",
  base = file("."),
  aggregate = Seq(scaladciCore, scaladciCoretest, scaladciExamples),
  settings = commonSettings ++ noPublishSettings ++ Seq(
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
  settings = commonSettings ++ noPublishSettings ++ Seq(
    packagedArtifacts := Map.empty
  )
)

lazy val scaladciExamples = Project(
  id = "scaladci-examples",
  base = file("examples"),
  dependencies = Seq(scaladciCore),
  settings = commonSettings ++ noPublishSettings ++ Seq(
    packagedArtifacts := Map.empty
  )
)



lazy val snapshots = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
lazy val releases = "Sonatype OSS Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/"

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := (if (isSnapshot.value) Some(snapshots) else Some(releases)),
  publishArtifact in Test := false,
  pomIncludeRepository := (_ => false),
  pomExtra :=
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
)

lazy val noPublishSettings = Seq(
  publish :=(),
  publishLocal :=(),
  publishArtifact := false
)