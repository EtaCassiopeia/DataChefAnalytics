package co.datachef.aggregator.topology

import java.time.Duration

import co.datachef.aggregator.CombinedKey
import co.datachef.loader.model.Click
import co.datachef.shared.repository.DataRepository
import io.circe.generic.auto._
import org.apache.kafka.streams.kstream.TimeWindows
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.KStream
import zio.{UIO, ZIO}

class ClickStreamTopology(builder: StreamsBuilder, dataRepository: DataRepository) {

  def build(): UIO[Unit] = ZIO.effectTotal {
    val timeSlot = 1
    val clicks: KStream[String, Click] =
      builder.stream[String, Click](s"click-$timeSlot")

    clicks.groupBy {
      case (_, click) => CombinedKey(click.campaignId, click.bannerId)
    }.windowedBy(TimeWindows.of(Duration.ofSeconds(5)))
      .count()
      .toStream
      .map((windowedKey, count) => (windowedKey.key(), count))
      .foreach {
        case (CombinedKey(campaignId, bannerId), count) =>
          dataRepository.addClick(campaignId, bannerId, timeSlot, count)
          ()
      }
  }
}
