name := "play-dbx"
version := "1.0.4"
scalaVersion := "2.11.8"
organization := "com.lingcreative"
organizationName := "LingCreative Studio"
description := "A transaction management library for PlayFramework's Jdbc DBApi."
homepage := Some(url(s"https://github.com/lingcreative/play-dbx"))
licenses := Seq(("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")))
scmInfo := Some(ScmInfo(
  url("https://github.com/lingcreative/play-dbx"),
  "https://github.com/lingcreative/play-dbx.git",
  Some("https://github.com/lingcreative/play-dbx.git")
))
developers := List(
  Developer("sauntor", "适然(Sauntor)", "sauntor@yeah.net", url("http://github.com/sauntor"))
)

sonatypeProfileName := organization.value
useGpg := true


lazy val root = (project in file("."))
compileOrder := CompileOrder.JavaThenScala
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.5.9" % Optional,
  "com.typesafe.play" %% "play-jdbc" % "2.5.9" % Optional,
  "com.typesafe.play" %% "play-jdbc-api" % "2.5.9" % Optional,
  "org.codehaus.btm" % "btm" % "2.1.3" % Optional,
  "javax.transaction" % "jta" % "1.1" % Optional,
  "org.apache.geronimo.specs" % "geronimo-jms_1.1_spec" % "1.1.1" % Optional,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.typesafe.play" %% "anorm" % "2.5.3" % Test
)

//filterScalaLibrary := false
