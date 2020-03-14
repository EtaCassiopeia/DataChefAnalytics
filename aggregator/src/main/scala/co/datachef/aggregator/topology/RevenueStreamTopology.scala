package co.datachef.aggregator.topology

import java.time.Duration

import co.datachef.aggregator.{ClickDetailsNotFound, CombinedKey}
import co.datachef.loader.model.{Click, Conversion, EnrichedConversion}
import co.datachef.shared.repository.DataRepository
import io.circe.generic.auto._
import org.apache.kafka.streams.kstream.TimeWindows
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{Grouped, KStream, KTable}
import zio.{UIO, ZIO}

class RevenueStreamTopology(builder: StreamsBuilder, dataRepository: DataRepository) {
  import RevenueStreamTopology._

  def build(): UIO[Unit] = ZIO.effectTotal {
    val timeSlot = 1
    val conversions: KStream[String, Conversion] =
      builder.stream[String, Conversion](s"conversion-$timeSlot")

    val clicks: KTable[String, Click] =
      builder.table[String, Click](s"click-$timeSlot")

    val joinFailed: (String, Either[ClickDetailsNotFound, EnrichedConversion]) => Boolean =
      (_, result: Either[ClickDetailsNotFound, EnrichedConversion]) => result.isLeft

    val joinSucceed: (String, Either[ClickDetailsNotFound, EnrichedConversion]) => Boolean =
      (_, result: Either[ClickDetailsNotFound, EnrichedConversion]) => result.isRight

    val revenueAggregatorFork: Array[KStream[String, Either[ClickDetailsNotFound, EnrichedConversion]]] = conversions
      .leftJoin(clicks)(
        (conversion, click) =>
          if (click == null) Left(ClickDetailsNotFound(conversion))
          else
            Right(
              EnrichedConversion(
                conversion.conversionId,
                click.clickId,
                click.campaignId,
                click.bannerId,
                conversion.revenue)))
      .branch(joinFailed, joinSucceed)

    revenueAggregatorFork(0).map { (key, value) =>
      val ClickDetailsNotFound(conversion) = value.swap.toOption.get
      (key, conversion)
    }.to(s"conversion-$timeSlot")

    revenueAggregatorFork(1).map { (key, value) =>
      (key, value.toOption.get)
    }.groupBy {
      case (_, eConversion) => CombinedKey(eConversion.campaignID, eConversion.bannerID)
    }.windowedBy(TimeWindows.of(Duration.ofSeconds(5)))
      .aggregate(0d) {
        case (_, enrichedConversion, revenue) =>
          revenue + enrichedConversion.revenue
      }
      .toStream
      .map((windowedKey, revenue) => (windowedKey.key(), revenue))
      .foreach {
        case (CombinedKey(campaignId, bannerId), revenue) =>
          dataRepository.addRevenue(campaignId, bannerId, timeSlot, revenue)
          ()
      }
  }
}

object RevenueStreamTopology {

  implicit val groupedConversion: Grouped[CombinedKey, EnrichedConversion] =
    Grouped.`with`[CombinedKey, EnrichedConversion]

  def apply(builder: StreamsBuilder, dataRepository: DataRepository): RevenueStreamTopology =
    new RevenueStreamTopology(builder, dataRepository)
}
