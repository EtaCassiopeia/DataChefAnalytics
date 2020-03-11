package co.datachef.loader.module

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import zio._
import zio.blocking.{Blocking, _}

case class ProducerSettings(private val broker: String) {

  def props: Properties = {
    val producerProperties = new Properties()
    producerProperties.put("bootstrap.servers", broker)
    producerProperties.put("acks", "all")
    producerProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    producerProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    producerProperties
  }
}

object Producer {
  type Producer = Has[Producer.Service]

  trait Service {
    def produce(record: ProducerRecord[String, String]): RIO[Blocking, Task[RecordMetadata]]

    def flush: RIO[Blocking, Unit]
  }

  final class Live(
    p: KafkaProducer[String, String]
  ) extends Service {

    override def produce(record: ProducerRecord[String, String]): RIO[Blocking, Task[RecordMetadata]] =
      for {
        done <- Promise.make[Throwable, RecordMetadata]
        runtime <- ZIO.runtime[Blocking]
        _ <- effectBlocking {
          p.send(
            record,
            (metadata: RecordMetadata, err: Exception) => {
              if (err != null) runtime.unsafeRun(done.fail(err))
              else runtime.unsafeRun(done.succeed(metadata))

              ()
            }
          )
        }
      } yield done.await

    override def flush: RIO[Blocking, Unit] = effectBlocking(p.flush())

    private[Producer] def close: UIO[Unit] = UIO(p.close())
  }

  def live(): ZLayer[Has[ProducerSettings], Throwable, Producer] =
    ZLayer.fromManaged {
      ZIO
        .accessM[Has[ProducerSettings]] { env =>
          val settings = env.get[ProducerSettings]
          ZIO {
            val props = settings.props
            val producer = new KafkaProducer[String, String](
              props,
              new StringSerializer(),
              new StringSerializer()
            )
            new Live(producer)
          }
        }
        .toManaged(_.close)
    }

  def produce(record: ProducerRecord[String, String]): RIO[Producer with Blocking, Task[RecordMetadata]] =
    ZIO.accessM(_.get.produce(record))
}
