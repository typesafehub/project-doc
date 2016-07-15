resolvers ++= Seq(
  Resolver.bintrayRepo("akka-contrib-extra", "maven"),
  Resolver.url("GitHub repository", url("http://shaggyyeti.github.io/releases"))(Resolver.ivyStylePatterns),
  "Typesafe Repository" at "https://repo.typesafe.com/typesafe/maven-releases"
)

// Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.3")

// Web plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-stylus" % "1.0.1")
addSbtPlugin("default"          % "sbt-sass"   % "0.1.9")

// ConductR
addSbtPlugin("com.lightbend.conductr" % "sbt-conductr"        % "2.1.6")
addSbtPlugin("com.typesafe.sbt"       % "sbt-bintray-bundle"  % "1.0.2")
