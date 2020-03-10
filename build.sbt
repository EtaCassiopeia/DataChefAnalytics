import Settings._
import Dependencies._
import Libraries._

lazy val `campaigns-api` = project
  .in(file("campaigns-api"))
  .settings(commonSettings)
  .settings(
    name := "campaigns-api",
    libraryDependencies ++= Seq(
      zio,
      zioInteropCats,
      circe,
      scalaLogging,
      logback,
      scalatest
    ) ++ http4sModules ++ cirisModules ++ tapirModules,
    addCompilerPlugin(kindProjectorPlugin cross CrossVersion.full)
  )

lazy val root = (project in file("."))
  .aggregate(`campaigns-api`)
  .dependsOn(`campaigns-api`)
  .settings(commonSettings)
  .settings(
    name := "data-chef-analytics"
  )
