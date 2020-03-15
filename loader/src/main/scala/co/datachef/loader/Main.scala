package co.datachef.loader

import java.nio.file.{Files, Paths}

import co.datachef.shared.model.{FileNameVal, RowParser, TimeSlotVal}
import co.datachef.loader.module.Producer.{produce, Producer}
import co.datachef.loader.module.{Producer, ProducerSettings}
import co.datachef.shared.model.config.KafkaConfig
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

  def program(filePath: String): ZIO[Console, Throwable, Unit] = {
    import zio.stream._

    for {
      fileDetails <- ZIO.fromTry(Try {
        val fileNamePattern(timeSlot, fileName) = filePath
        (FileNameVal(fileName), TimeSlotVal(timeSlot))
      })
      (fileName, timeSlot) = fileDetails
      rowParser = RowParser.fromFileName(fileName)
      topicName = s"${rowParser.recordType}-${timeSlot.value}"
      kafkaConfig <- ZIO.fromTry(Try(KafkaConfig.getConfig))
      inputStream <- IO(Files.newInputStream(Paths.get(filePath)))
      _ <- putStrLn(s"Loading file: $filePath")
      loader = ZIO.runtime[AppEnvironment].flatMap { _ =>
        Stream
          .fromInputStream(inputStream)
          .chunks
          .transduce(Sink.utf8DecodeChunk)
          .transduce(Sink.splitLines)
          .flatMap(c => Stream.fromChunk(c))
          .drop(1)
          .map(rowParser.fromString)
          .collect {
            case Some(record) => new ProducerRecord(topicName, record.key, record)
          }
          .foreach(produce)
      }
      _ <- loader.provideLayer(
        Clock.live ++ Console.live ++ Blocking.live ++ (ZLayer
          .succeed(ProducerSettings(kafkaConfig.brokers)) >>> Producer.live()))
    } yield ()
  }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    if (args.isEmpty) putStrLn("Please provide a file to import") *> ZIO.succeed(1) else program(args.head).orDie.as(0)
}
