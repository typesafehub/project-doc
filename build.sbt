name := "project-doc"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  ws
)

routesGenerator := InjectedRoutesGenerator
routesImport += "controllers.Application.Project"
