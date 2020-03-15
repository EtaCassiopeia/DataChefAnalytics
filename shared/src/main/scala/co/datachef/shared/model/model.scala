package co.datachef.shared.model

import cats.implicits._
import scala.util.Try

sealed trait RowParser[+T] {
  def fromString(value: String): Option[T]

  def recordType: String

  protected final def split(value: String): List[String] = value.split(",").map(_.trim).toList
}

object RowParser {
  def apply[T <: Record: RowParser]: RowParser[T] = implicitly[RowParser[T]]

  def fromFileName(fileName: FileNameVal): RowParser[Record] = fileName match {
    case FileNameVal(value) if value.startsWith("clicks") => RowParser[Click]
    case FileNameVal(value) if value.startsWith("impressions") => RowParser[Impression]
    case FileNameVal(value) if value.startsWith("conversions") => RowParser[Conversion]
  }
}

sealed trait Record extends Product with Serializable {
  def key: String
}

case class Impression(bannerId: BannerID, campaignId: CampaignID) extends Record {
  val key: String = s"$bannerId-$campaignId"
}

object Impression {

  implicit val ImpressionRowParser: RowParser[Impression] = new RowParser[Impression] {

    override def fromString(value: String): Option[Impression] = {
      split(value) match {
        case bannerId :: campaignId :: Nil => Impression(bannerId, campaignId).some
        case _ => None
      }
    }

    val recordType: String = "impression"
  }
}

case class Click(clickId: ClickID, bannerId: BannerID, campaignId: CampaignID) extends Record {
  val key: String = clickId
}

object Click {

  implicit val ClickRowParser: RowParser[Click] = new RowParser[Click] {

    override def fromString(value: String): Option[Click] = {
      split(value) match {
        case clickId :: bannerId :: campaignId :: Nil => Click(clickId, bannerId, campaignId).some
        case _ => None
      }
    }

    val recordType: String = "click"
  }
}

case class Conversion(conversionId: ConversionID, clickId: ClickID, revenue: Revenue) extends Record {
  val key: String = clickId
}

object Conversion {

  implicit val ConversionRowParser: RowParser[Conversion] = new RowParser[Conversion] {

    override def fromString(value: String): Option[Conversion] = {
      split(value) match {
        case conversionId :: clickId :: revenue :: Nil =>
          Try(revenue.toDouble).toOption.map(Conversion(conversionId, clickId, _))
        case _ => None
      }
    }

    val recordType: String = "conversion"
  }
}

case class EnrichedConversion(
  conversionId: ConversionID,
  clickId: ClickID,
  campaignID: CampaignID,
  bannerID: BannerID,
  revenue: Revenue)
    extends Record {
  val key: String = clickId
}

final case class Banner(bannerId: BannerID, image: String)

final case class FileNameVal(value: String) extends AnyVal
final case class TimeSlotVal(value: String) extends AnyVal
