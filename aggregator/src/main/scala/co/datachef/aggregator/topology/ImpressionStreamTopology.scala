package co.datachef.aggregator.topology

import co.datachef.loader.model.Impression
import co.datachef.shared.repository.DataRepository
import io.circe.generic.auto._
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.KStream
import zio.{UIO, ZIO}

class ImpressionStreamTopology(builder: StreamsBuilder, dataRepository: DataRepository) {

  def build(): UIO[Unit] = ZIO.effectTotal {
    val timeSlot = 1
    val impressions: KStream[String, Impression] =
      builder.stream[String, Impression](s"impression-$timeSlot")

    impressions.foreach {
      case (_, impression) =>
        dataRepository.addImpression(impression.campaignId, impression.bannerId, timeSlot)
        ()
    }
  }
}

object ImpressionStreamTopology {

  def apply(builder: StreamsBuilder, dataRepository: DataRepository): ImpressionStreamTopology =
    new ImpressionStreamTopology(builder, dataRepository)
}
