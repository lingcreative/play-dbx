resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

//addSbtPlugin("com.typesafe.play" % "interplay" % "1.3.4")
//addSbtPlugin("com.typesafe.play" % "play-docs-sbt-plugin" % sys.props.getOrElse("play.version", "2.5.10"))

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.8")

//addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

//libraryDependencies += "org.scalariform" %% "scalariform" % "0.1.5-20140822-69e2e30"
//addSbtPlugin("de.johoop" % "cpd4sbt" % "1.2.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
