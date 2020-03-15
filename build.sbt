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
      scalatest,
      kafkaClient
    ) ++ cirisModules ++ circeModules ++ logging
  )

lazy val `campaigns-api` = project
  .in(file("campaigns-api"))
  .settings(commonSettings)
  .settings(
    name := "campaigns-api",
    libraryDependencies ++= Seq(
      zioInteropCats
    ) ++ http4sModules ++ tapirModules,
    addCompilerPlugin(kindProjectorPlugin cross CrossVersion.full),
    mainClass in Compile := Some("co.datachef.analytics.Main"),
    dockerExposedPorts := Seq(8080)
  )
  .dependsOn(shared)
.enablePlugins(JavaAppPackaging)
.enablePlugins(DockerPlugin)

lazy val loader = project
  .in(file("loader"))
  .settings(commonSettings)
  .settings(
    name := "loader",
    libraryDependencies ++= Seq(
      zioStreams
    ) ++ circeModules
  )
  .dependsOn(shared)
  .enablePlugins(JavaAppPackaging)

lazy val aggregator = project
  .in(file("aggregator"))
  .settings(commonSettings)
  .settings(
    name := "aggregator",
    libraryDependencies ++= Seq(
      kafkaStreams
    ) ++ logging,
    mainClass in Compile := Some("co.datachef.aggregator.Main"),
  )
  .dependsOn(shared)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)

lazy val root = (project in file("."))
  .aggregate(shared, loader, aggregator, `campaigns-api`)
  .dependsOn(shared, loader, aggregator, `campaigns-api`)
  .settings(commonSettings)
  .settings(
    name := "data-chef-analytics"
  )
