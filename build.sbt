name := "play-dbx"
organization := "com.lingcreative"
version := "1.0.0"
scalaVersion := "2.11.8"

compileOrder := CompileOrder.JavaThenScala

//resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
lazy val root = (project in file(".")).enablePlugins(/*Playdoc,*/ PlayLibrary)
libraryDependencies += jdbc
libraryDependencies += evolutions
libraryDependencies += "com.adrianhurt" %% "play-bootstrap" % "1.0-P25-B3"
libraryDependencies += "com.typesafe.play" %% "anorm" % "2.5.0"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies ++= Seq(
  "aopalliance" % "aopalliance" % "1.0",
  "mysql" % "mysql-connector-java" % "6.0.5" % Test
)

pomExtra := (
<licenses>
  <license>
    <name>Apache 2</name>
    <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
    <distribution>repo</distribution>
  </license>
</licenses>)

playBuildRepoName := "play-dbx"
omnidocGithubRepo := s"lingcreative/${playBuildRepoName}"
homepage := Some(url(s"https://github.com/lingcreative/${playBuildRepoName}"))
