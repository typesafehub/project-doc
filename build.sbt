name := "project-doc"

version := "1.0-SNAPSHOT"

import JsEngineKeys._

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

StylusKeys.useNib in Assets := true

StylusKeys.compress in Assets := true

libraryDependencies ++= Seq(
  "org.webjars"          % "foundation"       % "5.5.1",
  "org.apache.commons"   % "commons-compress" % "1.8.1",
  "commons-io"           % "commons-io"       % "2.4",
  "com.googlecode.kiama" %% "kiama"           % "1.8.0",
  "org.scalatest"        %% "scalatest"       % "2.2.4" % "test",
  "org.scalatestplus"    %% "play"            % "1.2.0" % "test",
  ws
)

routesGenerator := InjectedRoutesGenerator
routesImport += "controllers.Application.Project"
