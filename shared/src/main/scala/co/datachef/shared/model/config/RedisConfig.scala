package co.datachef.shared.model.config

import ciris.api.Id
import ciris.{env, ConfigResult}
import ciris._

final case class RedisConfig private (server: String)

object RedisConfig {

  private val config: ConfigResult[Id, RedisConfig] = loadConfig(
    env[String]("REDIS_SERVER")
  ) { server =>
    RedisConfig(server)
  }

  val getConfig: RedisConfig = config.orThrow()
}
