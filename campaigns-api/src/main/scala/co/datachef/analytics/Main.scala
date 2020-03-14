package co.datachef.analytics

import scala.util.Try
import cats.effect.ExitCode
import co.datachef.analytics.model.config.ApplicationConfig
import co.datachef.analytics.route.CampaignRoute
import co.datachef.shared.module.CampaignRepository
import co.datachef.shared.module.CampaignRepository._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.redisson.config.Config
import tapir.docs.openapi._
import tapir.openapi.circe.yaml._
import tapir.swagger.http4s.SwaggerHttp4s
import zio._
import zio.clock.Clock
import zio.console.{putStrLn, Console}
import zio.interop.catz._
import zio.logging.Logging

object Main extends App {
  type AppEnvironment = Clock with Console with CampaignRepository with Logging

  private val campaignRoute = new CampaignRoute[AppEnvironment]
  private val yaml = campaignRoute.getEndPoints.toOpenAPI("Campaign", "1.0").toYaml

  private val httpApp =
    Router("/" -> campaignRoute.getRoutes, "/docs" -> new SwaggerHttp4s(yaml).routes[RIO[AppEnvironment, *]]).orNotFound

  private val finalHttpApp =
    Logger.httpApp[ZIO[AppEnvironment, Throwable, *]](logHeaders = true, logBody = true)(httpApp)

  //TODO unify configs
  val redissonConfig: UIO[Config] = ZIO.succeed {
    val config = new Config()
    config.useSingleServer.setAddress("redis://127.0.0.1:6379")
    config
  }

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    (for {
      applicationConfig <- ZIO.fromTry(Try(ApplicationConfig.getConfig))
      server = ZIO.runtime[AppEnvironment].flatMap { implicit rts =>
        BlazeServerBuilder[ZIO[AppEnvironment, Throwable, *]]
          .bindHttp(applicationConfig.server.port, applicationConfig.server.host.getHostAddress)
          .withHttpApp(finalHttpApp)
          .serve
          .compile[ZIO[AppEnvironment, Throwable, *], ZIO[AppEnvironment, Throwable, *], ExitCode]
          .drain
      }
      rConfig <- redissonConfig
      campaignRepo = ZLayer.succeed(rConfig) >>> CampaignRepository.live
      _ <- server.provideLayer {
        Clock.live ++ Console.live ++ campaignRepo ++ Logging.console((_, logEntry) => logEntry)
      }
    } yield ())
      .foldM(failure = err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1), success = _ => ZIO.succeed(0))
  }
}
