import sbt._

object Dependencies {

  object Versions {
    val zio = "1.0.0-RC18-2"
    val zioInteropCats = "2.0.0.0-RC12"
    val zioLogging = "0.2.4"
    val http4s = "0.21.0-M5"
    val circe = "0.13.0"
    val scalaLogging = "3.9.2"
    val logback = "1.2.3"
    val ciris = "0.13.0-RC1"
    val tapir = "0.11.9"
    val scalaTest = "3.0.8"
    val kindProjector = "0.11.0"
    val kafka = "2.4.1"
    val zioConfig = "1.0.0-RC12"
    val redisson = "3.12.3"
  }

  object Libraries {
    val zio = "dev.zio" %% "zio"                         % Versions.zio
    val zioStreams = "dev.zio" %% "zio-streams"          % Versions.zio
    val zioInteropCats = "dev.zio" %% "zio-interop-cats" % Versions.zioInteropCats
    val zioLogging = "dev.zio" %% "zio-logging"          % Versions.zioLogging

    val http4sModules: Seq[ModuleID] = Seq(
      "org.http4s" %% "http4s-core",
      "org.http4s" %% "http4s-dsl",
      "org.http4s" %% "http4s-blaze-server",
      "org.http4s" %% "http4s-circe"
    ).map(_ % Versions.http4s)

    val circeModules: Seq[ModuleID] = Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-parser",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-generic-extras"
    ).map(_ % Versions.circe)

    val cirisModules: Seq[ModuleID] = Seq(
      "is.cir" %% "ciris-cats",
      "is.cir" %% "ciris-cats-effect",
      "is.cir" %% "ciris-core",
      "is.cir" %% "ciris-enumeratum",
      "is.cir" %% "ciris-generic"
    ).map(_ % Versions.ciris)

    val tapirModules: Seq[ModuleID] = Seq(
      "com.softwaremill.tapir" %% "tapir-core",
      "com.softwaremill.tapir" %% "tapir-http4s-server",
      "com.softwaremill.tapir" %% "tapir-swagger-ui-http4s",
      "com.softwaremill.tapir" %% "tapir-openapi-docs",
      "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml",
      "com.softwaremill.tapir" %% "tapir-json-circe"
    ).map(_ % Versions.tapir)

    val kafkaClient = "org.apache.kafka"                           % "kafka-clients" % Versions.kafka
    val kafkaStreams = "org.apache.kafka" %% "kafka-streams-scala" % Versions.kafka

    val redisson = "org.redisson" % "redisson" % Versions.redisson

    val zioLoggingSlf4j = "dev.zio" %% "zio-logging-slf4j"             % Versions.zioLogging
    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogging
    val logback = "ch.qos.logback"                                     % "logback-classic" % Versions.logback

    val logging = Seq(
      zioLogging,
      zioLoggingSlf4j,
      logback
    )

    val scalatest = "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"

    val kindProjectorPlugin = "org.typelevel" %% "kind-projector" % Versions.kindProjector
  }
}
