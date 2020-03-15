package co.datachef.shared.model.config

import ciris.api.Id
import ciris.{env, loadConfig, ConfigResult}

case class KafkaConfig(brokers: String)

object KafkaConfig {

  private val config: ConfigResult[Id, KafkaConfig] = loadConfig(
    env[String]("KAFKA_BROKERS")
  ) { brokers =>
    KafkaConfig(brokers)
  }

  val getConfig: KafkaConfig = config.orThrow()
}
