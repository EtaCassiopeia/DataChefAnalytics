package co.datachef.analytics.route

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
      handleError(getBanners(campaignId, 1))
    }
  }

  val getEndPoints = {
    List(getBannersEndPoint)
  }

  private def getBanners(campaignId: String, timeSlot: Int): RIO[CampaignRepository with Logging, List[Banner]] = {
    for {
      _ <- logDebug(s"id: $campaignId")
      banners <- topBannersByRevenue(campaignId, timeSlot, 10)
    } yield banners
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
