package co.datachef.loader.model.config

import ciris.api.Id
import ciris.{env, loadConfig, ConfigResult}

final case class ApplicationConfig(
  kafkaConfig: KafkaConfig
)

object ApplicationConfig {

  private val config: ConfigResult[Id, ApplicationConfig] = loadConfig(
    env[String]("bootstrap.servers")
  ) { brokers =>
    ApplicationConfig(KafkaConfig(brokers))
  }

  val getConfig: ApplicationConfig = config.orThrow()
}
