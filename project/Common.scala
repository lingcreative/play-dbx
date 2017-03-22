import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifacts

object Common extends AutoPlugin {
  import com.typesafe.tools.mima.core._
  
  override def trigger = allRequirements
  override def requires = JvmPlugin

  val previousVersion = "0.0.1"

  override def projectSettings = mimaDefaultSettings ++ Seq(
    resolvers += "Scalaz Bintray Repo" at {
      "http://dl.bintray.com/scalaz/releases" // specs2 depends on scalaz-stream
    },
    scalacOptions ~= (_.filterNot(_ == "-Xfatal-warnings")),
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint",
      "-Ywarn-unused-import", "-Ywarn-unused", "-Ywarn-dead-code",
      "-Ywarn-numeric-widen"),
    fork in Test := true,
    previousArtifacts := {
       if (scalaVersion.value startsWith "2.12.") Set.empty else {
         if (crossPaths.value) {
           Set(organization.value % s"${moduleName.value}_${scalaBinaryVersion.value}" % previousVersion)
         } else {
           Set(organization.value % moduleName.value % previousVersion)
         }
       }
    }
  )

  @inline def missMeth(n: String) =
    ProblemFilters.exclude[MissingMethodProblem](n)
}

