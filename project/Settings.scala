import sbt._
import Keys._

object Settings {

  lazy val commonSettings =
    compilerSettings ++
      sbtSettings ++ Seq(
      organization := "com.github.etacassiopeia"
    )

  lazy val compilerSettings =
    Seq(
      scalaVersion := "2.13.1",
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
    )

  lazy val sbtSettings =
    Seq(fork := true, cancelable in Global := true)
}
