enablePlugins(SbtPlugin)
enablePlugins(GatlingOssPlugin)

homepage := Some(new URL("https://gatling.io"))
organization := "io.gatling.frontline"
sonatypeProfileName := "io.gatling"
organizationHomepage := Some(new URL("https://gatling.io"))
githubPath := "gatling/sbt-frontline"
startYear := Some(2018)
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

name := "sbt-frontline"

scalaVersion := "2.13.7"

libraryDependencies += "org.zeroturnaround" % "zt-zip" % "1.14"

gatlingDevelopers := Seq(
  GatlingDeveloper(
    "slandelle@gatling.io",
    "Stéphane Landelle",
    true
  ),
  GatlingDeveloper(
    "pdalpra@gatling.io",
    "Pierre Dal-Pra",
    true
  )
)
