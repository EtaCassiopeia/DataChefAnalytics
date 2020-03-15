package co.datachef.analytics

import scala.util.Try
import cats.effect.ExitCode
import co.datachef.analytics.model.config.ServerConfig
import co.datachef.analytics.module.CampaignRepository
import co.datachef.analytics.module.CampaignRepository._
import co.datachef.analytics.route.CampaignRoute
import co.datachef.shared.model.config.RedisConfig
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

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    (for {
      serverConfig <- ZIO.fromTry(Try(ServerConfig.getConfig))
      rConfig <- ZIO.fromTry(Try {
        val server = RedisConfig.getConfig.server
        val config = new Config()
        config.useSingleServer.setAddress(server)
        config
      })
      server = ZIO.runtime[AppEnvironment].flatMap { implicit rts =>
        BlazeServerBuilder[ZIO[AppEnvironment, Throwable, *]]
          .bindHttp(serverConfig.port, serverConfig.host.getHostAddress)
          .withHttpApp(finalHttpApp)
          .serve
          .compile[ZIO[AppEnvironment, Throwable, *], ZIO[AppEnvironment, Throwable, *], ExitCode]
          .drain
      }
      campaignRepo = ZLayer.succeed(rConfig) >>> CampaignRepository.live
      _ <- server.provideLayer {
        Clock.live ++ Console.live ++ campaignRepo ++ Logging.console((_, logEntry) => logEntry)
      }
    } yield ())
      .foldM(failure = err => putStrLn(s"Execution failed with: $err") *> ZIO.succeed(1), success = _ => ZIO.succeed(0))
  }
}
