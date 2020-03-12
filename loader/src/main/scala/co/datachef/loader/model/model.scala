package co.datachef.loader.model

import cats.implicits._
import co.datachef.shared.model.{BannerID, CampaignID, ClickID, ConversionID, Revenue}

import scala.util.Try

sealed trait RowParser[+T] {
  def fromString(value: String): Option[T]

  def recordType: String

  protected final def split(value: String): List[String] = value.split(",").map(_.trim).toList
}

object RowParser {
  def apply[T: RowParser]: RowParser[T] = implicitly[RowParser[T]]

  def fromFileName(fileName: FileName): RowParser[Record] = fileName match {
    case FileName(value) if value.startsWith("clicks") => RowParser[Click]
    case FileName(value) if value.startsWith("impressions") => RowParser[Impression]
    case FileName(value) if value.startsWith("conversions") => RowParser[Conversion]
  }
}

sealed trait Record

case class Impression(bannerId: BannerID, campaignId: CampaignID) extends Record

object Impression {

  implicit object ImpressionRowParser extends RowParser[Impression] {

    override def fromString(value: String): Option[Impression] = {
      split(value) match {
        case bannerId :: campaignId :: Nil => Impression(bannerId, campaignId).some
        case _ => None
      }
    }

    val recordType: String = "impression"
  }
}

case class Click(clickId: ClickID, bannerId: BannerID, campaignId: CampaignID) extends Record

object Click {

  implicit object ClickRowParser extends RowParser[Click] {

    override def fromString(value: String): Option[Click] = {
      split(value) match {
        case clickId :: bannerId :: campaignId :: Nil => Click(clickId, bannerId, campaignId).some
        case _ => None
      }
    }

    val recordType: String = "click"
  }
}

case class Conversion(conversionId: ConversionID, clickId: ClickID, revenue: Revenue) extends Record

object Conversion {

  implicit object ConversionRowParser extends RowParser[Conversion] {

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

final case class FileName(value: String) extends AnyVal

final case class TimeSlot(value: String) extends AnyVal
