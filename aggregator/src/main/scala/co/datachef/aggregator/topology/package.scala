package co.datachef.aggregator

import java.util

import co.datachef.loader.model.{Conversion, Record}
import co.datachef.loader.serde.JSONSerde
import co.datachef.shared.model.{BannerID, CampaignID}
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.kstream.Consumed

sealed trait ExpectedError extends Product with Serializable
case class ClickDetailsNotFound(conversion: Conversion) extends ExpectedError

case class CombinedKey(campaignID: CampaignID, bannerID: BannerID)

package object topology {
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

  implicit val recordConsumed: Consumed[String, Record] = Consumed.`with`[String, Record]
}
