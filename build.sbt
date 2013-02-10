name := "unfiltered-directives"

organization := "com.jteigen"

version := "0.1.0-SNAPSHOT"

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