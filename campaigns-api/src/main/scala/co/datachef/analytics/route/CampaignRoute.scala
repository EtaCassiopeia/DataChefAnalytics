package co.datachef.analytics.route

import java.util.Calendar

import co.datachef.analytics.implicits.Throwable._
import co.datachef.analytics.model._
import co.datachef.shared.model.Banner
import co.datachef.shared.module.CampaignRepository._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import tapir.DecodeResult.Error
import tapir._
import tapir.json.circe._
import tapir.model.StatusCodes
import tapir.server.http4s._
import tapir.server.{DecodeFailureHandling, ServerDefaults}
import zio.interop.catz._
import zio.logging.{Logging, _}
import zio.{RIO, ZIO}

import scala.util.Random

class CampaignRoute[R <: CampaignRepository with Logging] extends Http4sDsl[RIO[R, *]] {

  private implicit val customServerOptions: Http4sServerOptions[RIO[R, *]] = Http4sServerOptions
    .default[RIO[R, *]]
    .copy(
      decodeFailureHandler = (request, input, failure) => {
        failure match {
          case Error(_, error) =>
            DecodeFailureHandling.response(jsonBody[BadRequestResponse])(BadRequestResponse(error.toString))
          case _ => ServerDefaults.decodeFailureHandler(request, input, failure)
        }
      }
    )

  private val getBannersEndPoint = endpoint.get
    .in("campaigns" / path[String]("campaign id"))
    .errorOut(
      oneOf(
        statusMapping(StatusCodes.InternalServerError, jsonBody[InternalServerErrorResponse]),
        statusMapping(StatusCodes.NotFound, jsonBody[NotFoundResponse])
      ))
    .out(jsonBody[List[Banner]])

  val getRoutes: HttpRoutes[RIO[R, *]] = {
    getBannersEndPoint.toRoutes { campaignId =>
      handleError(getBanners(campaignId))
    }
  }

  val getEndPoints = {
    List(getBannersEndPoint)
  }

  private def getBanners(campaignId: String): RIO[CampaignRepository with Logging, List[Banner]] = {
    val now = Calendar.getInstance()
    val currentMinute: Double = now.get(Calendar.MINUTE).doubleValue()
    val timeSlot: Int = Math.ceil(currentMinute / 15).intValue()
    getBannersByTimeSlot(campaignId, timeSlot)
  }

  private def getBannersByTimeSlot(
    campaignId: String,
    timeSlot: Int): RIO[CampaignRepository with Logging, List[Banner]] = {
    val emptyBannerList = ZIO.succeed(List.empty[Banner])
    // X is the number of banners with conversions within a campaign (topProfitableBanners.size)
    for {
      _ <- logDebug(s"id: $campaignId")
      // X>=10 : Show the Top 10 banners based on revenue within that campaign
      // X in range(5,10) : Show the Top x banners based on revenue within that campaign
      topProfitableBanners <- topBannersByRevenue(campaignId, timeSlot, 10)
      x = topProfitableBanners.size
      // X in range(1,5) : Your collection of banners should consists of 5 banners, containing:
      //      The top x banners based on revenue within that campaign
      //      Banners with the most clicks within that campaign to make up a collection of 5 unique banners.
      topClickedBanners <- if (x >= 1 && x <= 5)
        topBannersByClick(campaignId, timeSlot, 10 - x)
      else emptyBannerList
      // X == 0 : Show the top­5 banners based on clicks.If the amount of banners with clicks are less than 5 within that campaign,
      // then you should add random banners to make up a collection of 5 unique banners.
      topRandomBanners <- if (x == 0) for {
        tcb <- topBannersByClick(campaignId, timeSlot, 5)
        rnd <- if (tcb.size < 5) randomBannersByCampaign(campaignId, timeSlot, 5 - tcb.size)
        else emptyBannerList
      } yield tcb ++ rnd
      else emptyBannerList
    } yield Random.shuffle(topProfitableBanners ++ topClickedBanners ++ topRandomBanners)
  }

  private def handleError[A](result: ZIO[R, Throwable, A]): ZIO[R, Throwable, Either[ErrorResponse, A]] =
    result
      .fold(
        e => Left(InternalServerErrorResponse("Unexpected errors", "", e.getStacktrace)),
        Right(_)
      )
      .foldCause(
        c => Left(InternalServerErrorResponse("Unexpected errors", "", c.squash.getStacktrace)),
        identity
      )
}
