// General

name := "project-doc"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "org.apache.commons"    %  "commons-compress" 		% "1.8.1",
  "commons-io"            %  "commons-io"       		% "2.4",
  "org.webjars"           %  "foundation"       		% "5.5.1",
  "com.googlecode.kiama"  %% "kiama"            		% "1.8.0",
  "com.typesafe.conductr" %% "play-conductr-bundle-lib"	% "0.6.1",
  "com.typesafe.play"     %% "play-doc"         		% "1.1.0",
  "io.spray"              %% "spray-caching"    		% "1.3.3",
  "org.scalatest"         %% "scalatest"        		% "2.2.4" % "test",
  "org.scalatestplus"     %% "play"             		% "1.2.0" % "test",
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

lazy val root = (project in file(".")).enablePlugins(JavaAppPackaging, PlayScala, SbtTypesafeConductR)

// ConductR

import ByteConversions._

BundleKeys.nrOfCpus := 1.0
BundleKeys.memory := 64.MiB
BundleKeys.diskSpace := 50.MiB
BundleKeys.roles := Set("web-doc")

BundleKeys.endpoints := Map("webdoc" -> Endpoint("http", 0, Set(URI("http://conductr.typesafe.com"))))
BundleKeys.startCommand += "-Dhttp.port=$WEBDOC_BIND_PORT -Dhttp.address=$WEBDOC_BIND_IP"
