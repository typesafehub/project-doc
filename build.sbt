// General

name := "project-doc"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io",
  "patriknw at bintray" at "http://dl.bintray.com/patriknw/maven"
)

libraryDependencies ++= Seq(
  "com.github.patriknw"   %% "akka-data-replication"    % "0.11",
  "com.typesafe.conductr" %% "akka-conductr-bundle-lib"	% "0.8.0",
  "org.apache.commons"    %  "commons-compress" 		    % "1.8.1",
  "commons-io"            %  "commons-io"       		    % "2.4",
  "org.webjars"           %  "foundation"       	     	% "5.5.1",
  "com.googlecode.kiama"  %% "kiama"            		    % "1.8.0",
  "com.typesafe.conductr" %% "play-conductr-bundle-lib"	% "0.9.0-4158e00ea986c1b619378d5ba8bc364d34acfc0d",
  "com.typesafe.play"     %% "play-doc"         		    % "1.1.0",
  "io.spray"              %% "spray-caching"    		    % "1.3.3",
  "org.scalatest"         %% "scalatest"        		    % "2.2.4" % "test",
  "org.scalatestplus"     %% "play"             		    % "1.2.0" % "test",
  ws
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

BundleKeys.system := "doc-renderer-cluster-1.0"

BundleKeys.endpoints := Map(
  "akka-remote" -> Endpoint("tcp"),
  "web" -> Endpoint("http", services = Set(URI("http://conductr.typesafe.com")))
)
BundleKeys.startCommand += "-Dhttp.port=$WEB_BIND_PORT -Dhttp.address=$WEB_BIND_IP"
