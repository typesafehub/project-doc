name := "project-doc"

version := "1.0-SNAPSHOT"

import JsEngineKeys._

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

StylusKeys.useNib in Assets := true

StylusKeys.compress in Assets := true

libraryDependencies ++= Seq(
  "org.webjars" % "foundation" % "5.5.1",
  ws
)

routesGenerator := InjectedRoutesGenerator
routesImport += "controllers.Application.Project"
