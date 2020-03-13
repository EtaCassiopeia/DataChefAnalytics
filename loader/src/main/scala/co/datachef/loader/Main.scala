package co.datachef.loader

import java.nio.file.{Files, Paths}

import co.datachef.loader.model.config.ApplicationConfig
import co.datachef.loader.model.{FileName, RowParser, TimeSlot}
import co.datachef.loader.module.Producer.{produce, Producer}
import co.datachef.loader.module.{Producer, ProducerSettings}
//import io.circe.generic.extras.auto._
//import io.circe.generic.extras.Configuration
//import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.{putStrLn, Console}
import zio.{App, IO, ZIO, ZLayer}

import scala.util.Try
import scala.util.matching.Regex

object Main extends App {
  type AppEnvironment = Clock with Console with Blocking with Producer

  val fileNamePattern: Regex = """.*/(\d)/(.*.csv)""".r

  def program(filePath: String): ZIO[Any, Throwable, Unit] = {
    import zio.stream._

    for {
      fileDetails <- ZIO.fromTry(Try {
        val fileNamePattern(timeSlot, fileName) = filePath
        (FileName(fileName), TimeSlot(timeSlot))
      })
      (fileName, timeSlot) = fileDetails
      rowParser = RowParser.fromFileName(fileName)
      topicName = s"${rowParser.recordType}-${timeSlot.value}"
      applicationConfig <- ZIO.fromTry(Try(ApplicationConfig.getConfig))
      inputStream <- IO(Files.newInputStream(Paths.get(filePath)))
      loader = ZIO.runtime[AppEnvironment].flatMap { _ =>
        Stream
          .fromInputStream(inputStream)
          .chunks
          .transduce(Sink.utf8DecodeChunk)
          .transduce(Sink.splitLines)
          .flatMap(c => Stream.fromChunk(c))
          .map(rowParser.fromString)
          .collect {
            case Some(record) => record
          }
          .foreach(record => produce(new ProducerRecord(topicName, record)))
      }
      _ <- loader.provideLayer(
        Clock.live ++ Console.live ++ Blocking.live ++ (ZLayer.succeed(
          ProducerSettings(applicationConfig.kafkaConfig.brokers)) >>> Producer.live()))
    } yield ()
  }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    if (args.isEmpty) putStrLn("Please provide a file to import") *> ZIO.succeed(1) else program(args.head).orDie.as(0)
}
