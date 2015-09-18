// General

name := "project-doc"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "spray repo"          at "http://repo.spray.io",
  "patriknw at bintray" at "http://dl.bintray.com/patriknw/maven",
  "typesafe-releases"   at "http://repo.typesafe.com/typesafe/maven-releases",
  Resolver.bintrayRepo("typesafe", "maven-releases")
)

libraryDependencies ++= Seq(
  "com.github.patriknw"   %% "akka-data-replication"      % "0.11",
  "org.apache.commons"    %  "commons-compress" 		      % "1.8.1",
  "commons-io"            %  "commons-io"       		      % "2.4",
  "org.webjars"           %  "foundation"       	     	  % "5.5.1",
  "org.webjars"           %  "prettify"                   % "4-Mar-2013",
  "com.googlecode.kiama"  %% "kiama"            		      % "1.8.0",
  "com.typesafe.conductr" %% "play24-conductr-bundle-lib"	% "1.0.1",
  "com.typesafe.play"     %% "play-doc"         		      % "1.2.3",
  "io.spray"              %% "spray-caching"    		      % "1.3.3",
  "com.typesafe.akka"     %% "akka-testkit"               % "2.3.12",
  "org.scalatest"         %% "scalatest"        		      % "2.2.4" % "test",
  "org.scalatestplus"     %% "play"             		      % "1.4.0-M3" % "test"
)

// Play
routesGenerator := InjectedRoutesGenerator

// sbt-web
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
sassOptions in Assets ++= Seq("--compass", "-r", "compass")
StylusKeys.useNib in Assets := true
StylusKeys.compress in Assets := false

// Project/module declarations
lazy val root = (project in file(".")).enablePlugins(JavaAppPackaging, PlayScala, ConductRPlugin)

// ConductR
import ByteConversions._

BundleKeys.nrOfCpus := 1.0
BundleKeys.memory := 64.MiB
BundleKeys.diskSpace := 50.MiB
BundleKeys.roles := Set("web-doc")

BundleKeys.system := "doc-renderer-cluster-1"

BundleKeys.endpoints := Map(
  "akka-remote" -> Endpoint("tcp"),
  "web" -> Endpoint("http", services = Set(URI("http://conductr.typesafe.com")))
)
BundleKeys.startCommand += "-Dhttp.port=$WEB_BIND_PORT -Dhttp.address=$WEB_BIND_IP"
