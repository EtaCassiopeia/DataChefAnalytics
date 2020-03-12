package co.datachef.shared.module

import co.datachef.shared.model._
import org.redisson.Redisson
import org.redisson.api.{RBloomFilter, RScoredSortedSet, RSet, RedissonClient}
import org.redisson.config.Config
import zio._
import zio.blocking._

import scala.jdk.CollectionConverters._

object DataRepository {
  type DataRepository = Has[DataRepository.Service]

  trait Service {
    def addImpression(campaignID: CampaignID, bannerID: BannerID, timeSlot: TimeSlot): RIO[Blocking, Unit]

    def addRevenue(
      campaignID: CampaignID,
      bannerID: BannerID,
      timeSlot: TimeSlot,
      revenue: Revenue): RIO[Blocking, Unit]
    def addClick(campaignID: CampaignID, bannerID: BannerID, timeSlot: TimeSlot, count: Double): RIO[Blocking, Unit]

    def topBannersByRevenue(campaignID: CampaignID, timeSlot: TimeSlot, number: Int): RIO[Blocking, List[Banner]]
    def topBannersByClick(campaignID: CampaignID, timeSlot: TimeSlot, number: Int): RIO[Blocking, List[Banner]]
    def bannersByCampaign(campaignID: CampaignID, timeSlot: TimeSlot, number: Int): RIO[Blocking, List[Banner]]

    def addConversion(conversionID: ConversionID, timeSlot: TimeSlot): RIO[Blocking, Unit]
    def isConversionExists(conversionID: ConversionID, timeSlot: TimeSlot): RIO[Blocking, Boolean]
  }

  final class Live(redissonClient: RedissonClient) extends Service {

    override def addImpression(campaignID: CampaignID, bannerID: BannerID, timeSlot: TimeSlot): RIO[Blocking, Unit] =
      effectBlocking {
        val key = s"I-C$campaignID-TS$timeSlot"
        val set: RSet[String] = redissonClient.getSet(key)
        set.add(bannerID)
        ()
      }

    override def addRevenue(
      campaignID: CampaignID,
      bannerID: BannerID,
      timeSlot: TimeSlot,
      revenue: Revenue): RIO[Blocking, Unit] = effectBlocking {
      val key = s"R-C$campaignID-TS$timeSlot"
      val set: RScoredSortedSet[String] = redissonClient.getScoredSortedSet(key)
      set.addScore(bannerID, revenue)
      ()
    }

    override def addClick(
      campaignID: CampaignID,
      bannerID: BannerID,
      timeSlot: TimeSlot,
      count: Double): RIO[Blocking, Unit] =
      effectBlocking {
        val key = s"C-C$campaignID-TS$timeSlot"
        val set: RScoredSortedSet[String] = redissonClient.getScoredSortedSet(key)
        set.addScore(bannerID, count)
        ()
      }

    override def topBannersByRevenue(
      campaignID: CampaignID,
      timeSlot: TimeSlot,
      number: TimeSlot): RIO[Blocking, List[Banner]] =
      effectBlocking {
        val key = s"R-C$campaignID-TS$timeSlot"
        val set: RScoredSortedSet[String] = redissonClient.getScoredSortedSet(key)
        set.valueRangeReversed(0, number - 1).asScala.toList.map(bannerId => Banner(campaignID, bannerId))
      }

    override def topBannersByClick(
      campaignID: CampaignID,
      timeSlot: TimeSlot,
      number: TimeSlot): RIO[Blocking, List[Banner]] =
      effectBlocking {
        val key = s"C-C$campaignID-TS$timeSlot"
        val set: RScoredSortedSet[String] = redissonClient.getScoredSortedSet(key)
        set.valueRangeReversed(0, number - 1).asScala.toList.map(bannerId => Banner(campaignID, bannerId))
      }

    override def bannersByCampaign(
      campaignID: CampaignID,
      timeSlot: TimeSlot,
      number: TimeSlot): RIO[Blocking, List[Banner]] =
      effectBlocking {
        val key = s"I-C$campaignID-TS$timeSlot"
        val set: RSet[String] = redissonClient.getSet(key)
        set.random(number - 1).asScala.toList.map(bannerId => Banner(campaignID, bannerId))
      }

    override def addConversion(conversionID: ConversionID, timeSlot: TimeSlot): RIO[Blocking, Unit] = effectBlocking {
      val key = s"CBF-TS$timeSlot"
      val conversionsBloomFilter: RBloomFilter[String] = redissonClient.getBloomFilter(key)
      //TODO: Initialize Bloom filter params
      conversionsBloomFilter.tryInit(10000, 0.01)
      conversionsBloomFilter.add(conversionID)
      ()
    }

    override def isConversionExists(conversionID: ConversionID, timeSlot: TimeSlot): RIO[Blocking, Boolean] =
      effectBlocking {
        val key = s"CBF-TS$timeSlot"
        val conversionsBloomFilter: RBloomFilter[String] = redissonClient.getBloomFilter(key)
        //TODO: Initialize Bloom filter params
        conversionsBloomFilter.tryInit(10000, 0.01)
        conversionsBloomFilter.contains(conversionID)
      }
  }

  def live(): ZLayer[Has[Config], Throwable, DataRepository] =
    ZLayer.fromFunction { env =>
      val config = env.get
      val redissonClient: RedissonClient = Redisson.create(config)
      new Live(redissonClient)
    }

//Accessor Methods
  def addImpression(
    campaignID: CampaignID,
    bannerID: BannerID,
    timeSlot: TimeSlot): RIO[DataRepository with Blocking, Unit] =
    ZIO.accessM(_.get.addImpression(campaignID, bannerID, timeSlot))

  def addRevenue(
    campaignID: CampaignID,
    bannerID: BannerID,
    timeSlot: TimeSlot,
    revenue: Revenue): RIO[DataRepository with Blocking, Unit] =
    ZIO.accessM(_.get.addRevenue(campaignID, bannerID, timeSlot, revenue))

  def addClick(
    campaignID: CampaignID,
    bannerID: BannerID,
    timeSlot: TimeSlot,
    count: Double): RIO[DataRepository with Blocking, Unit] =
    ZIO.accessM(_.get.addClick(campaignID, bannerID, timeSlot, count))

  def topBannersByRevenue(
    campaignID: CampaignID,
    timeSlot: TimeSlot,
    number: Int): RIO[DataRepository with Blocking, List[Banner]] =
    ZIO.accessM(_.get.topBannersByRevenue(campaignID, timeSlot, number))

  def topBannersByClick(
    campaignID: CampaignID,
    timeSlot: TimeSlot,
    number: Int): RIO[DataRepository with Blocking, List[Banner]] =
    ZIO.accessM(_.get.topBannersByClick(campaignID, timeSlot, number))

  def bannersByCampaign(
    campaignID: CampaignID,
    timeSlot: TimeSlot,
    number: Int): RIO[DataRepository with Blocking, List[Banner]] =
    ZIO.accessM(_.get.bannersByCampaign(campaignID, timeSlot, number))

  def addConversion(conversionID: ConversionID, timeSlot: TimeSlot): RIO[DataRepository with Blocking, Unit] =
    ZIO.accessM(_.get.addConversion(conversionID, timeSlot))

  def isConversionExists(conversionID: ConversionID, timeSlot: TimeSlot): RIO[DataRepository with Blocking, Boolean] =
    ZIO.accessM(_.get.isConversionExists(conversionID, timeSlot))
}
