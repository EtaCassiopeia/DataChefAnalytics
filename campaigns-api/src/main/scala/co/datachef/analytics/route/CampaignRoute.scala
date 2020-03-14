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
import zio.logging.Logging
import zio.{RIO, ZIO}
//import zio.logging._

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
    .in("campaigns" / path[Long]("campaign id"))
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

  private def getBanners(campaignId: Long): ZIO[R, ExpectedFailure, List[Banner]] = {
    ZIO.fail(NotFoundFailure(s"Can not find a campaign by $campaignId"))
//    for {
//      _ <- logDebug(s"id: $campaignId")
//      banners <- topBannersByRevenue(campaignId,1,10)
//      u <- banners match {
//        case None => ZIO.fail(NotFoundFailure(s"Can not find a campaign by $campaignId"))
//        case Some(s) => ZIO.succeed(s)
//      }
//    } yield {
//      u
//    }
  }

  private def handleError[A](result: ZIO[R, ExpectedFailure, A]): ZIO[R, Throwable, Either[ErrorResponse, A]] = {
    result
      .fold(
        {
          case DSFailure(t) => Left(InternalServerErrorResponse("Data store failure", t.getMessage, t.getStacktrace))
          case NotFoundFailure(message) => Left(NotFoundResponse(message))
        },
        Right(_)
      )
      .foldCause(
        c => Left(InternalServerErrorResponse("Unexpected errors", "", c.squash.getStacktrace)),
        identity
      )
  }
}
