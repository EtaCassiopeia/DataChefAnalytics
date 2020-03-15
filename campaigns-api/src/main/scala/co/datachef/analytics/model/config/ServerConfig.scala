package co.datachef.analytics.model.config

import java.net.InetAddress

import ciris.api.Id
import ciris.{env, loadConfig, ConfigResult}
final case class ServerConfig(host: InetAddress, port: Int)

object ServerConfig {

  private val config: ConfigResult[Id, ServerConfig] = loadConfig(
    env[InetAddress]("SERVER_HOST"),
    env[Int]("SERVER_PORT")
  ) { (url, port) =>
    ServerConfig(url, port)
  }

  val getConfig: ServerConfig = config.orThrow()
}
