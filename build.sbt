import sbt.Keys._

lazy val GatlingTest = config("gatling") extend Test

// This must be set to 2.11.11 because Gatling does not run on 2.12.2
scalaVersion in ThisBuild := "2.11.11"

libraryDependencies += guice
libraryDependencies += "org.joda" % "joda-convert" % "1.8"
libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "4.9"

libraryDependencies += "com.netaporter" %% "scala-uri" % "0.4.16"
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.0"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0-M3" % Test
libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2" % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.2.2" % Test

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.0.0" // used to retrieve and parse HTML

// The Play project itself
lazy val root = (project in file("."))
  .enablePlugins(Common, PlayScala, GatlingPlugin)
  .configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    name := """vie-de-merde-scraper-scala""",
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )
