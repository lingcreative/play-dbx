name := "play-dbx"
organization := "com.lingcreative"
version := "1.0.2-SNAPSHOT"
scalaVersion := "2.11.8"

compileOrder := CompileOrder.JavaThenScala

lazy val root = (project in file(".")).enablePlugins(/*Playdoc,*/ PlayLibrary)
libraryDependencies ++= Seq(
  jdbc % Optional,
  evolutions % Test,
  "org.codehaus.btm" % "btm" % "2.1.3" % Optional,
  "javax.transaction" % "jta" % "1.1" % Optional,
  "org.apache.geronimo.specs" % "geronimo-jms_1.1_spec" % "1.1.1" % Optional,
  "com.typesafe.play" %% "anorm" % "2.5.0" % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B3" % Test,
  "mysql" % "mysql-connector-java" % "6.0.5" % Test
)

pomExtra := (
<scm>
  <connection>scm:git:github.com/{organization.value.substring(4)}/{name.value}.git</connection>
  <developerConnection>scm:git:git@github.com:/{organization.value.substring(4)}/{name.value}.git</developerConnection>
  <url>https://github.com/{organization.value.substring(4)}/{name.value}</url>
</scm>
<developers>
  <developer>
    <id>sauntor</id>
    <name>适然(Sauntor)</name>
    <email>sauntor@yeah.net</email>
    <url>https://github.com/sauntor</url>
    <organization>LingCreative</organization>
    <organizationUrl>https://github.com/lingcreative</organizationUrl>
  </developer>
</developers>
)

playBuildRepoName := name.value
omnidocGithubRepo := s"${organization.value.substring(4)}/${name.value}"
homepage := Some(url(s"https://github.com/${organization.value.substring(4)}/${name.value}"))

sonatypeProfileName := organization.value
useGpg := true
