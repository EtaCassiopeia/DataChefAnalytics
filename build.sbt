import Settings._
import Dependencies._
import Libraries._

lazy val shared = project.in(file("shared")).settings(commonSettings)
  .settings(
    name := "shared",
    libraryDependencies ++= Seq (
      zio,
      redisson,
      scalatest
    ) ++ logging
  )

lazy val `campaigns-api` = project
  .in(file("campaigns-api"))
  .settings(commonSettings)
  .settings(
    name := "campaigns-api",
    libraryDependencies ++= Seq(
      zioInteropCats,
      circe,
    ) ++ http4sModules ++ cirisModules ++ tapirModules,
    addCompilerPlugin(kindProjectorPlugin cross CrossVersion.full)
  ).dependsOn(shared)

lazy val loader = project
  .in(file("loader"))
  .settings(commonSettings)
  .settings(
    name := "loader",
    libraryDependencies ++= Seq (
      zioStreams,
      circe,
      kafkaClient,
    ) ++ cirisModules
  ).dependsOn(shared)

lazy val root = (project in file("."))
  .aggregate(shared,`campaigns-api`,loader)
  .dependsOn(shared,`campaigns-api`,loader)
  .settings(commonSettings)
  .settings(
    name := "data-chef-analytics"
  )
