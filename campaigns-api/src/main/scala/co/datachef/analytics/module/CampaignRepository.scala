package co.datachef.analytics.module

import co.datachef.shared.model.{Banner, CampaignID, TimeSlot}
import co.datachef.shared.repository.DataRepository
import org.redisson.Redisson
import org.redisson.config.Config
import zio._

object CampaignRepository {
  type CampaignRepository = Has[CampaignRepository.Service]

  trait Service {
    def topBannersByRevenue(campaignID: CampaignID, timeSlot: TimeSlot, number: Int): Task[List[Banner]]
    def topBannersByClick(campaignID: CampaignID, timeSlot: TimeSlot, number: Int): Task[List[Banner]]
    def bannersByCampaign(campaignID: CampaignID, timeSlot: TimeSlot, number: Int): Task[List[Banner]]
  }

  final class Live(dataRepository: DataRepository) extends Service {

    override def topBannersByRevenue(campaignID: CampaignID, timeSlot: TimeSlot, number: TimeSlot): Task[List[Banner]] =
      ZIO.fromFuture(implicit ec => dataRepository.topBannersByRevenue(campaignID, timeSlot, number))

    override def topBannersByClick(campaignID: CampaignID, timeSlot: TimeSlot, number: TimeSlot): Task[List[Banner]] =
      ZIO.fromFuture(implicit ec => dataRepository.topBannersByClick(campaignID, timeSlot, number))

    override def bannersByCampaign(campaignID: CampaignID, timeSlot: TimeSlot, number: TimeSlot): Task[List[Banner]] =
      ZIO.fromFuture(implicit ec => dataRepository.bannersByCampaign(campaignID, timeSlot, number))
  }

  def live: ZLayer[Has[Config], Throwable, CampaignRepository] =
    ZLayer.fromFunction { env =>
      val config = env.get
      val dataRepository: DataRepository = new DataRepository(Redisson.create(config))
      new Live(dataRepository)
    }

  //Accessor Methods
  def topBannersByRevenue(
    campaignID: CampaignID,
    timeSlot: TimeSlot,
    number: Int): RIO[CampaignRepository, List[Banner]] =
    ZIO.accessM(_.get.topBannersByRevenue(campaignID, timeSlot, number))

  def topBannersByClick(
    campaignID: CampaignID,
    timeSlot: TimeSlot,
    number: Int): RIO[CampaignRepository, List[Banner]] =
    ZIO.accessM(_.get.topBannersByClick(campaignID, timeSlot, number))

  def randomBannersByCampaign(
    campaignID: CampaignID,
    timeSlot: TimeSlot,
    number: Int): RIO[CampaignRepository, List[Banner]] =
    ZIO.accessM(_.get.bannersByCampaign(campaignID, timeSlot, number))
}
