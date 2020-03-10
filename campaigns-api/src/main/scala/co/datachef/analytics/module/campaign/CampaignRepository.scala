package co.datachef.analytics.module.campaign

import co.datachef.analytics.model.{Banner, ExpectedFailure}
import zio.{Has, Ref, ZIO, ZLayer}

object CampaignRepository {
  type CampaignRepository = Has[CampaignRepository.Service]

  trait Service {
    def banners(campaignId: Long): ZIO[Any, ExpectedFailure, Option[List[Banner]]] = ???
  }

  def ref: ZLayer[Has[Ref[Map[Long, List[Banner]]]], ExpectedFailure, CampaignRepository] = ???

  def live: ZLayer[Any, ExpectedFailure, CampaignRepository] = ???

  def banners(campaignId: Long): ZIO[CampaignRepository, ExpectedFailure, Option[List[Banner]]] =
    ZIO.accessM(_.get.banners(campaignId))
}
