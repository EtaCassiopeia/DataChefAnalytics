import sbt._

object Dependencies {

  object Versions {
    val zio = "1.0.0-RC18-1"
    val zioInteropCats = "2.0.0.0-RC11"
    val http4s = "0.21.0-M5"
    val circe = "0.12.3"
    val scalaLogging = "3.9.2"
    val logback = "1.2.3"
    val ciris = "0.13.0-RC1"
    val tapir = "0.11.9"
    val scalaTest = "3.0.8"
    val kindProjector = "0.11.0"
  }

  object Libraries {
    val zio = "dev.zio" %% "zio"                         % Versions.zio
    val zioInteropCats = "dev.zio" %% "zio-interop-cats" % Versions.zioInteropCats

    val http4sModules: Seq[ModuleID] = Seq(
      "org.http4s" %% "http4s-core"         % Versions.http4s,
      "org.http4s" %% "http4s-dsl"          % Versions.http4s,
      "org.http4s" %% "http4s-blaze-server" % Versions.http4s,
      "org.http4s" %% "http4s-circe"        % Versions.http4s
    )

    val circe = "io.circe" %% "circe-generic" % Versions.circe

    val cirisModules: Seq[ModuleID] = Seq(
      "is.cir" %% "ciris-cats"        % Versions.ciris,
      "is.cir" %% "ciris-cats-effect" % Versions.ciris,
      "is.cir" %% "ciris-core"        % Versions.ciris,
      "is.cir" %% "ciris-enumeratum"  % Versions.ciris,
      "is.cir" %% "ciris-generic"     % Versions.ciris
    )

    val tapirModules: Seq[ModuleID] = Seq(
      "com.softwaremill.tapir" %% "tapir-core"               % Versions.tapir,
      "com.softwaremill.tapir" %% "tapir-http4s-server"      % Versions.tapir,
      "com.softwaremill.tapir" %% "tapir-swagger-ui-http4s"  % Versions.tapir,
      "com.softwaremill.tapir" %% "tapir-openapi-docs"       % Versions.tapir,
      "com.softwaremill.tapir" %% "tapir-openapi-circe-yaml" % Versions.tapir,
      "com.softwaremill.tapir" %% "tapir-json-circe"         % Versions.tapir
    )

    val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogging
    val logback = "ch.qos.logback"                                     % "logback-classic" % Versions.logback

    val scalatest = "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"

    val kindProjectorPlugin = "org.typelevel" %% "kind-projector" % Versions.kindProjector
  }
}
