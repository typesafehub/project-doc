resolvers ++= Seq(Resolver.bintrayRepo("akka-contrib-extra", "maven"))

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0-M3")

// web plugins

addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.7")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.0.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.0.1")

// conductR
addSbtPlugin("com.typesafe.conductr" % "sbt-typesafe-conductr" % "0.27.0")
