import com.typesafe.sbt.pgp.PgpKeys

releaseSettings

organization := "com.jteigen"

name := "unfiltered-directives"

description := "monadic api for unfiltered"

scalaVersion := "2.10.0"

crossScalaVersions := Seq("2.9.1-1", "2.9.2", "2.10.0")

scalacOptions <++= (scalaBinaryVersion).map{
    case "2.10" => Seq("-language:implicitConversions")
    case _      => Nil
}

licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/MIT"))

libraryDependencies += "net.databinder" %% "unfiltered" % "0.6.7"

libraryDependencies ++= Seq(
    "net.databinder" %% "unfiltered-jetty" % "0.6.7" % "test",
    "net.databinder" %% "unfiltered-filter" % "0.6.7" % "test")

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publish <<= PgpKeys.publishSigned

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

homepage := Some(url("http://github.com/teigen/unfiltered-directives"))

pomExtra := (
  <scm>
    <url>git@github.com:teigen/unfiltered-directives.git</url>
    <connection>scm:git:git@github.com:teigen/unfiltered-directives.git</connection>
  </scm>
  <developers>
    <developer>
      <id>jteigen</id>
      <name>Jon-Anders Teigen</name>
      <url>http://jteigen.com</url>
    </developer>
  </developers>)
