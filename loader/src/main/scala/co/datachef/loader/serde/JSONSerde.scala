package co.datachef.loader.serde

import java.util

import co.datachef.loader.model.Record
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import io.circe.parser.decode
import org.apache.kafka.common.errors.SerializationException

class JSONSerde[T <: Record: Encoder: Decoder] extends Serializer[T] with Deserializer[T] with Serde[T] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = super.configure(configs, isKey)

  override def serialize(topic: String, data: T): Array[Byte] = data.asJson.noSpaces.getBytes

  override def deserialize(topic: String, data: Array[Byte]): T = decode[T](data.map(_.toChar).mkString) match {
    case Right(t) => t
    case Left(e) => throw new SerializationException(e)
  }

  override def serializer(): Serializer[T] = this

  override def deserializer(): Deserializer[T] = this
}
