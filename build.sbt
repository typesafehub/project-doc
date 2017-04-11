// General

name := "project-doc"
version := "1.0-SNAPSHOT"
scalaVersion := "2.11.8"

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "com.typesafe.akka"      %% "akka-distributed-data-experimental" % "2.4.7",
  "org.apache.commons"     %  "commons-compress" 		               % "1.11",
  "commons-io"             %  "commons-io"       		               % "2.5",
  "org.webjars"            %  "foundation"       	     	           % "5.5.1",
  "com.googlecode.kiama"   %% "kiama"            		               % "1.8.0",
  "com.typesafe.conductr"  %% "play25-conductr-bundle-lib"	       % "1.4.7",
  "com.typesafe.play"      %% "play-doc"         		               % "1.6.0",
  "io.spray"               %% "spray-caching"    		               % "1.3.3",
  "com.typesafe.akka"      %% "akka-testkit"                       % "2.3.12",
  "org.scalatest"          %% "scalatest"        		               % "2.2.4" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play"                 % "1.5.1" % "test"
)

// Play
routesGenerator := InjectedRoutesGenerator

// sbt-web
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
sassOptions in Assets ++= Seq("--compass", "-r", "compass")
StylusKeys.useNib in Assets := true
StylusKeys.compress in Assets := false

// ConductR
import ByteConversions._

javaOptions in Universal := Seq(
  "-J-Xmx64m",
  "-J-Xms64m"
)

BundleKeys.nrOfCpus := 1.0
BundleKeys.memory := 128.MiB
BundleKeys.diskSpace := 50.MiB
BundleKeys.roles := Set("web-doc")

BundleKeys.system := "doc-renderer-cluster"
BundleKeys.systemVersion := "2"

BundleKeys.endpoints := Map(
  "akka-remote" -> Endpoint("tcp"),
  "web" -> Endpoint("http", services = Set(URI("http://conductr.lightbend.com")))
)

// Bundle publishing configuration

inConfig(Bundle)(Seq(
  bintrayVcsUrl := Some("https://github.com/typesafehub/project-doc"),
  bintrayOrganization := Some("typesafe")
))

// Project/module declarations
lazy val root = (project in file(".")).enablePlugins(PlayScala)
