package co.datachef.aggregator

import java.util.Properties

import co.datachef.aggregator.topology.{ClickStreamTopology, ImpressionStreamTopology, RevenueStreamTopology}
import co.datachef.shared.model.config.{KafkaConfig, RedisConfig}
import co.datachef.shared.repository.DataRepository
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KafkaStreams.{State, StateListener}
import org.apache.kafka.streams.scala.StreamsBuilder
import org.redisson.Redisson
import org.redisson.config.Config
import zio._
import zio.console.{putStrLn, _}

import scala.jdk.CollectionConverters._
import scala.util.Try

object Main extends App {
  type AppEnvironment = Console

  def streamConfig(broker: String, appId: String): UIO[Properties] = ZIO.succeed {
    import org.apache.kafka.clients.consumer.ConsumerConfig
    import org.apache.kafka.streams.StreamsConfig

    val props: Map[String, AnyRef] = Map(
      StreamsConfig.APPLICATION_ID_CONFIG -> appId,
      StreamsConfig.BOOTSTRAP_SERVERS_CONFIG -> broker,
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest"
    )

    val properties = new Properties()
    properties.putAll(props.asJava)

    properties
  }

  def program: ZIO[ZEnv, Throwable, Unit] = {
    for {
      rConfig <- ZIO.fromTry(Try {
        val server = RedisConfig.getConfig.server
        val config = new Config()
        config.useSingleServer.setAddress(server)
        config
      })
      kafkaConfig <- ZIO.fromTry(Try(KafkaConfig.getConfig))
      revenueStreamConfig <- streamConfig(kafkaConfig.brokers, "stream-revenue")
      clickStreamConfig <- streamConfig(kafkaConfig.brokers, "stream-impression-click")
      revenueStreamBuilder = new StreamsBuilder()
      clickStreamBuilder = new StreamsBuilder()
      dataRepository = new DataRepository(Redisson.create(rConfig))
      _ <- RevenueStreamTopology(revenueStreamBuilder, dataRepository).build()
      _ <- ClickStreamTopology(clickStreamBuilder, dataRepository).build()
      _ <- ImpressionStreamTopology(clickStreamBuilder, dataRepository).build()
      p <- Promise.make[Nothing, String]
      managedStream = Managed.makeEffect {
        val terminateOnError = new StateListener {
          override def onChange(newState: State, oldState: State): Unit = {
            if (newState == State.ERROR) p.succeed(s"Failed ${newState.toString}")
            ()
          }
        }

        val revenueStream = new KafkaStreams(revenueStreamBuilder.build(), revenueStreamConfig)
        val clickStream = new KafkaStreams(clickStreamBuilder.build(), clickStreamConfig)

        val streams = List(revenueStream, clickStream)
        streams.foreach(_.setStateListener(terminateOnError))
        streams
      }(streams => ZIO.effect(streams.foreach(_.close())))
      _ <- managedStream
        .use(streams => ZIO.effect(streams.foreach(_.start())))
        .fork
      waitForIt <- p.await.flatMap(putStrLn(_)).fork
      _ <- waitForIt.join
    } yield ()
  }

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, Int] = {
    program
      .catchAll(e => putStrLn(s"Failed to start processing: ${e.getMessage}") *> ZIO.succeed(1))
      .as(0)
  }
}
