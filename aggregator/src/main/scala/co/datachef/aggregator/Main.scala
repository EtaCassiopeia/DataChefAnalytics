package co.datachef.aggregator

import java.util.Properties

import co.datachef.aggregator.topology.RevenueStreamTopology
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KafkaStreams.State
import org.apache.kafka.streams.scala.StreamsBuilder
import zio._
import zio.console.{putStrLn, _}

import scala.jdk.CollectionConverters._

object Main extends App {
  type AppEnvironment = Console

  def streamConfig(): UIO[Properties] = ZIO.effectTotal {
    import org.apache.kafka.streams.StreamsConfig
    import org.apache.kafka.clients.consumer.ConsumerConfig

    val props: Map[String, AnyRef] = Map(
      StreamsConfig.APPLICATION_ID_CONFIG -> "streams-revenue-click-aggregator",
      StreamsConfig.BOOTSTRAP_SERVERS_CONFIG -> "localhost:9092",
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest"
    )

    val properties = new Properties()
    properties.putAll(props.asJava)

    properties
  }

  def program: ZIO[Console, Throwable, Unit] = {
    for {
      config <- streamConfig()
      builder = new StreamsBuilder()
      _ <- RevenueStreamTopology(builder).build()
      p <- Promise.make[Nothing, String]
      managedStream = Managed.makeEffect {
        val stream = new KafkaStreams(builder.build(), config)
        stream.setStateListener((newState: KafkaStreams.State, _: KafkaStreams.State) => {
          if (newState == State.ERROR) p.succeed(s"Failed ${newState.toString}")
          ()
        })
        stream
      }(stream => ZIO.effect(stream.close()))
      _ <- managedStream.use(stream => ZIO.effect(stream.start())).fork
      waitForIt <- p.await.flatMap(putStrLn(_)).fork
      _ <- waitForIt.join
    } yield ()
  }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] =
    program.catchAll(e => putStrLn(s"Failed to start processing: ${e.getMessage}") *> ZIO.succeed(1)).as(0)
}
