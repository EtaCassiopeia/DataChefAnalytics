package co.datachef.analytics.module.logger

import co.datachef.analytics.implicits.Throwable._
import com.typesafe.scalalogging.LazyLogging
import zio.ZLayer.NoDeps
import zio.console.Console
import zio.{Has, IO, ZIO, ZLayer}

object LoggerService {

  trait Service {
    def error(message: => String): ZIO[Any, Nothing, Unit]

    def warn(message: => String): ZIO[Any, Nothing, Unit]

    def info(message: => String): ZIO[Any, Nothing, Unit]

    def debug(message: => String): ZIO[Any, Nothing, Unit]

    def trace(message: => String): ZIO[Any, Nothing, Unit]

    def error(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit]

    def warn(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit]

    def info(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit]

    def debug(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit]

    def trace(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit]
  }

  type LoggerService = Has[Service]

  def console: ZLayer[Console, Nothing, LoggerService] =
    ZLayer.fromFunction(console =>
      new Service {
        def error(message: => String): ZIO[Any, Nothing, Unit] = console.get.putStr(message)

        def warn(message: => String): ZIO[Any, Nothing, Unit] = console.get.putStr(message)

        def info(message: => String): ZIO[Any, Nothing, Unit] = console.get.putStr(message)

        def debug(message: => String): ZIO[Any, Nothing, Unit] = console.get.putStr(message)

        def trace(message: => String): ZIO[Any, Nothing, Unit] = console.get.putStr(message)

        def error(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
          console.get.putStr(s"message: $message, exception: ${t.getStacktrace}")

        def warn(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
          console.get.putStr(s"message: $message, exception: ${t.getStacktrace}")

        def info(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
          console.get.putStr(s"message: $message, exception: ${t.getStacktrace}")

        def debug(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
          console.get.putStr(s"message: $message, exception: ${t.getStacktrace}")

        def trace(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
          console.get.putStr(s"message: $message, exception: ${t.getStacktrace}")
      })

  def live: NoDeps[Nothing, LoggerService] =
    ZLayer.succeed(new Service with LazyLogging {
      def error(message: => String): ZIO[Any, Nothing, Unit] = IO.effectTotal(logger.error(message))

      def warn(message: => String): ZIO[Any, Nothing, Unit] = IO.effectTotal(logger.warn(message))

      def info(message: => String): ZIO[Any, Nothing, Unit] = IO.effectTotal(logger.info(message))

      def debug(message: => String): ZIO[Any, Nothing, Unit] = IO.effectTotal(logger.debug(message))

      def trace(message: => String): ZIO[Any, Nothing, Unit] = IO.effectTotal(logger.trace(message))

      def error(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
        IO.effectTotal(logger.error(s"message: $message, exception: ${t.getStacktrace}"))

      def warn(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
        IO.effectTotal(logger.warn(s"message: $message, exception: ${t.getStacktrace}"))

      def info(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
        IO.effectTotal(logger.info(s"message: $message, exception: ${t.getStacktrace}"))

      def debug(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
        IO.effectTotal(logger.debug(s"message: $message, exception: ${t.getStacktrace}"))

      def trace(t: Throwable)(message: => String): ZIO[Any, Nothing, Unit] =
        IO.effectTotal(logger.trace(s"message: $message, exception: ${t.getStacktrace}"))
    })

  //accessor methods
  def error(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.error(message))

  def warn(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.warn(message))

  def info(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.info(message))

  def debug(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.debug(message))

  def trace(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.trace(message))

  def error(t: Throwable)(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.error(t)(message))

  def warn(t: Throwable)(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.warn(t)(message))

  def info(t: Throwable)(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.info(t)(message))

  def debug(t: Throwable)(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.debug(t)(message))

  def trace(t: Throwable)(message: => String): ZIO[LoggerService, Nothing, Unit] =
    ZIO.accessM(_.get.trace(t)(message))
}
