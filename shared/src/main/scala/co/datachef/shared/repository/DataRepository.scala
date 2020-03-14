package co.datachef.shared.repository

import java.lang

import co.datachef.shared.model.{Banner, BannerID, CampaignID, ClickID, ConversionID, Revenue, TimeSlot}
import org.redisson.api.{RBloomFilter, RScoredSortedSet, RSet, RedissonClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters._
import scala.jdk.CollectionConverters._

class DataRepository(redissonClient: RedissonClient) {

  def addImpression(campaignID: CampaignID, bannerID: BannerID, timeSlot: TimeSlot): Future[lang.Boolean] = {
    val key = s"I-C$campaignID-TS$timeSlot"
    val set: RSet[String] = redissonClient.getSet(key)
    set.addAsync(bannerID).asScala
  }

  def incRevenue(
    campaignID: CampaignID,
    bannerID: BannerID,
    timeSlot: TimeSlot,
    revenue: Revenue): Future[lang.Double] = {
    val key = s"R-C$campaignID-TS$timeSlot"
    val set: RScoredSortedSet[String] = redissonClient.getScoredSortedSet(key)
    set.addScoreAsync(bannerID, revenue).asScala
  }

  def incClickCount(
    campaignID: CampaignID,
    bannerID: BannerID,
    timeSlot: TimeSlot,
    count: Long): Future[lang.Double] = {
    val key = s"C-C$campaignID-TS$timeSlot"
    val set: RScoredSortedSet[String] = redissonClient.getScoredSortedSet(key)
    set.addScoreAsync(bannerID, count).asScala
  }

  def topBannersByRevenue(campaignID: CampaignID, timeSlot: TimeSlot, number: TimeSlot)(
    implicit ec: ExecutionContext): Future[List[Banner]] = {
    val key = s"R-C$campaignID-TS$timeSlot"
    val set: RScoredSortedSet[String] = redissonClient.getScoredSortedSet(key)
    set
      .valueRangeReversedAsync(0, number - 1)
      .asScala
      .map(_.asScala.toList.map(bannerId => Banner(campaignID, bannerId)))
  }

  def topBannersByClick(campaignID: CampaignID, timeSlot: TimeSlot, number: TimeSlot)(
    implicit ec: ExecutionContext): Future[List[Banner]] = {
    val key = s"C-C$campaignID-TS$timeSlot"
    val set: RScoredSortedSet[String] = redissonClient.getScoredSortedSet(key)
    set
      .valueRangeReversedAsync(0, number - 1)
      .asScala
      .map(_.asScala.toList.map(bannerId => Banner(campaignID, bannerId)))
  }

  def bannersByCampaign(campaignID: CampaignID, timeSlot: TimeSlot, number: TimeSlot)(
    implicit ec: ExecutionContext): Future[List[Banner]] = {
    val key = s"I-C$campaignID-TS$timeSlot"
    val set: RSet[String] = redissonClient.getSet(key)
    set.randomAsync(number - 1).asScala.map(_.asScala.toList.map(bannerId => Banner(campaignID, bannerId)))
  }

  def addConversion(conversionID: ConversionID, timeSlot: TimeSlot): Boolean = {
    val key = s"CBF-TS$timeSlot"
    val conversionsBloomFilter: RBloomFilter[String] = redissonClient.getBloomFilter(key)
    //TODO: Initialize Bloom filter params
    conversionsBloomFilter.tryInit(10000, 0.01)
    conversionsBloomFilter.add(conversionID)
  }

  def isConversionExists(conversionID: ConversionID, timeSlot: TimeSlot): Boolean = {
    val key = s"CBF-TS$timeSlot"
    val conversionsBloomFilter: RBloomFilter[String] = redissonClient.getBloomFilter(key)
    conversionsBloomFilter.tryInit(10000, 0.01)
    conversionsBloomFilter.contains(conversionID)
  }

  def addClick(clickID: ClickID, timeSlot: TimeSlot): Boolean = {
    val key = s"ClBF-TS$timeSlot"
    val conversionsBloomFilter: RBloomFilter[String] = redissonClient.getBloomFilter(key)
    conversionsBloomFilter.tryInit(10000, 0.01)
    conversionsBloomFilter.add(clickID)
  }

  def isClickExists(clickID: ClickID, timeSlot: TimeSlot): Boolean = {
    val key = s"ClBF-TS$timeSlot"
    val conversionsBloomFilter: RBloomFilter[String] = redissonClient.getBloomFilter(key)
    conversionsBloomFilter.tryInit(10000, 0.01)
    conversionsBloomFilter.contains(clickID)
  }
}
