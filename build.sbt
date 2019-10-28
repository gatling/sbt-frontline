enablePlugins(SbtPlugin)

homepage := Some(new URL("https://gatling.io"))
organization := "io.gatling.frontline"
organizationHomepage := Some(new URL("https://gatling.io"))
startYear := Some(2018)
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

name := "sbt-frontline"
scalafmtOnCompile := true
scalaVersion := "2.12.8"
publishMavenStyle := false
bintrayRepository := "sbt-plugins"
bintrayOrganization in bintray := None

libraryDependencies += "org.zeroturnaround" % "zt-zip" % "1.13"
