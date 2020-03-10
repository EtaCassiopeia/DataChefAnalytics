package co.datachef.analytics.model.config

import java.net.InetAddress

import ciris.api.Id
import ciris.{env, loadConfig, ConfigResult}

final case class ApplicationConfig(
  server: ServerConfig
)

object ApplicationConfig {

  private val config: ConfigResult[Id, ApplicationConfig] = loadConfig(
    env[InetAddress]("server.host"),
    env[Int]("server.port")
  ) { (url, port) =>
    ApplicationConfig(ServerConfig(url, port))
  }

  val getConfig: ApplicationConfig = config.orThrow()
}
