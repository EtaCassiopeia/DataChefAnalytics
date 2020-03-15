package co.datachef.loader.module

import java.util.Properties

import co.datachef.shared.model.Record
import co.datachef.shared.serde.JSONSerde
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import zio._
import zio.blocking.{Blocking, _}

case class ProducerSettings(private val broker: String) {

  def props: Properties = {
    val producerProperties = new Properties()
    producerProperties.put("bootstrap.servers", broker)
    producerProperties
  }
}

object Producer {
  type Producer = Has[Producer.Service]

  trait Service {
    def produce(record: ProducerRecord[String, Record]): RIO[Blocking, Task[RecordMetadata]]

    def flush: RIO[Blocking, Unit]
  }

  final class Live(
    kafkaProducer: KafkaProducer[String, Record]
  ) extends Service {

    override def produce(record: ProducerRecord[String, Record]): RIO[Blocking, Task[RecordMetadata]] =
      for {
        done <- Promise.make[Throwable, RecordMetadata]
        runtime <- ZIO.runtime[Blocking]
        _ <- effectBlocking {
          kafkaProducer.send(
            record,
            (metadata: RecordMetadata, err: Exception) => {
              if (err != null) runtime.unsafeRun(done.fail(err))
              else runtime.unsafeRun(done.succeed(metadata))

              ()
            }
          )
        }
      } yield done.await

    override def flush: RIO[Blocking, Unit] = effectBlocking(kafkaProducer.flush())

    private[Producer] def close: UIO[Unit] = UIO(kafkaProducer.close())
  }

  def live(): ZLayer[Has[ProducerSettings], Throwable, Producer] =
    ZLayer.fromManaged {
      ZIO
        .accessM[Has[ProducerSettings]] { env =>
          val settings = env.get[ProducerSettings]
          ZIO {
            val props = settings.props
            val producer = new KafkaProducer[String, Record](
              props,
              new StringSerializer(),
              JSONSerde()
            )
            new Live(producer)
          }
        }
        .toManaged(_.close)
    }

  def produce(record: ProducerRecord[String, Record]): RIO[Producer with Blocking, Task[RecordMetadata]] =
    ZIO.accessM(_.get.produce(record))
}
