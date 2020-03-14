package co.datachef.aggregator.topology

import java.time.Duration
import java.util

import co.datachef.loader.model.{Click, Conversion, EnrichedConversion, Record}
import co.datachef.loader.serde.JSONSerde
import co.datachef.shared.model.{BannerID, CampaignID}
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import org.apache.kafka.streams.kstream.TimeWindows
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.kstream.{Consumed, Grouped, KStream, KTable}
import org.apache.kafka.streams.scala.StreamsBuilder
import zio.UIO

class RevenueStreamTopology(builder: StreamsBuilder) {
  import RevenueStreamTopology._

  def build(): UIO[Unit] = UIO.succeed {
    val conversions: KStream[String, Conversion] =
      builder.stream[String, Conversion]("conversion-1")

    val clicks: KTable[String, Click] =
      builder.table[String, Click]("click-1")

    val joinFailed: (String, Either[ClickDetailsNotFound, EnrichedConversion]) => Boolean =
      (_, result: Either[ClickDetailsNotFound, EnrichedConversion]) => result.isLeft

    val joinSucceed: (String, Either[ClickDetailsNotFound, EnrichedConversion]) => Boolean =
      (_, result: Either[ClickDetailsNotFound, EnrichedConversion]) => result.isRight

    val revenueAggregatorFork: Array[KStream[String, Either[ClickDetailsNotFound, EnrichedConversion]]] = conversions
      .leftJoin(clicks)((conversion, click) => {
        println("running")
        if (click == null) Left(ClickDetailsNotFound(conversion))
        else
          Right(
            EnrichedConversion(
              conversion.conversionId,
              click.clickId,
              click.campaignId,
              click.bannerId,
              conversion.revenue))
      })
      .branch(joinFailed, joinSucceed)

    revenueAggregatorFork(0).map { (key, value) =>
      val ClickDetailsNotFound(conversion) = value.swap.toOption.get
      println(s"Retry: $conversion")
      (key, conversion)
    }.to("conversion-1")

    revenueAggregatorFork(1).map { (key, value) =>
      (key, value.toOption.get)
    }.groupBy {
      case (_, eConversion) => CombinedKey(eConversion.campaignID, eConversion.bannerID)
    }.windowedBy(TimeWindows.of(Duration.ofSeconds(50)))
      .aggregate(0d) {
        case (_, enrichedConversion, revenue) =>
          revenue + enrichedConversion.revenue
      }
      .toStream
      .map((windowedKey, revenue) => (windowedKey.key(), revenue))
      .foreach {
        case (CombinedKey(campaignId, bannerId), revenue) =>
          println(s"revenue for campaign: $campaignId, banner: $bannerId = $revenue")
      }
  }
}

object RevenueStreamTopology {
  sealed trait ExpectedError extends Product with Serializable
  case class ClickDetailsNotFound(conversion: Conversion) extends ExpectedError
  case class CombinedKey(campaignID: CampaignID, bannerID: BannerID)

  implicit def jsonSerDeRecord[T <: Record: Encoder: Decoder]: JSONSerde[T] = new JSONSerde[T]()

  implicit val combinedKeySerde: Serde[CombinedKey] with Serializer[CombinedKey] with Deserializer[CombinedKey] =
    new Serde[CombinedKey] with Serializer[CombinedKey] with Deserializer[CombinedKey] {
      override def configure(configs: util.Map[BannerID, _], isKey: Boolean): Unit = super.configure(configs, isKey)

      override def serializer(): Serializer[CombinedKey] = this

      override def deserializer(): Deserializer[CombinedKey] = this

      override def serialize(topic: String, data: CombinedKey): Array[Byte] =
        s"${data.campaignID}-${data.bannerID}".getBytes

      override def deserialize(topic: String, data: Array[Byte]): CombinedKey = {
        val input = data.map(_.toChar).mkString
        input.split("-").toList match {
          case campaignId :: bannerId :: Nil => CombinedKey(campaignId, bannerId)
          case _ => throw new SerializationException(s"Failed to deserialize $input")
        }
      }
    }

  implicit val conversionConsumed: Consumed[String, Record] = Consumed.`with`[String, Record]

  implicit val grouped: Grouped[CombinedKey, EnrichedConversion] =
    Grouped.`with`[CombinedKey, EnrichedConversion]

  def apply(builder: StreamsBuilder): RevenueStreamTopology = new RevenueStreamTopology(builder)
}
