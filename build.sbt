import Settings._
import Dependencies._
import Libraries._

lazy val shared = project
  .in(file("shared"))
  .settings(commonSettings)
  .settings(
    name := "shared",
    libraryDependencies ++= Seq(
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
      zioInteropCats
    ) ++ http4sModules ++ circeModules ++ cirisModules ++ tapirModules,
    addCompilerPlugin(kindProjectorPlugin cross CrossVersion.full)
  )
  .dependsOn(shared)

lazy val loader = project
  .in(file("loader"))
  .settings(commonSettings)
  .settings(
    name := "loader",
    libraryDependencies ++= Seq(
      zioStreams,
      kafkaClient
    ) ++ circeModules ++ cirisModules
  )
  .dependsOn(shared)

lazy val aggregator = project
  .in(file("aggregator"))
  .settings(commonSettings)
  .settings(
    name := "aggregator",
    libraryDependencies ++= Seq(
      kafkaClient
    ) ++ logging
  )
  .dependsOn(shared,loader)

lazy val root = (project in file("."))
  .aggregate(shared, loader, aggregator, `campaigns-api`)
  .dependsOn(shared, loader, aggregator, `campaigns-api`)
  .settings(commonSettings)
  .settings(
    name := "data-chef-analytics"
  )
