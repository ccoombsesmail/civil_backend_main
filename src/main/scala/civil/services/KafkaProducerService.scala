package civil.services

import civil.config.Config
import civil.errors.AppError.InternalServerError
import org.apache.kafka.clients.producer.RecordMetadata
import zio._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde

trait KafkaProducerService {
  def publish[A](
      message: A,
      userId: String,
      serde: Serde[Any, A],
      topic: String
  ): ZIO[Any, Throwable, RecordMetadata]
}

class KafkaProducerServiceLive extends KafkaProducerService {

  override def publish[A](
      message: A,
      key: String,
      serde: Serde[Any, A],
      topic: String = "notifications"
  ): ZIO[Any, Throwable, RecordMetadata] = {
    (for {
      _ <- ZIO.logInfo(s"Publishing to $topic ")
      p <- Producer.produce(
        topic,
        key,
        message,
        Serde.string,
        serde
      )
    } yield p).provide(KafkaProducerService.producerLayer)

  }
}

object KafkaProducerService {
  private val producerSettings: ProducerSettings =
    ProducerSettings(List(Config().getString("kafka.bootstrap.servers")))
      .withProperty(
        "security.protocol",
        Config().getString("kafka.security.protocol")
      )
      .withProperty(
        "sasl.jaas.config",
        Config().getString("kafka.sasl.jaas.config")
      )
      .withProperty(
        "sasl.mechanism",
        Config().getString("kafka.sasl.mechanism")
      )
      .withProperty(
        "client.dns.lookup",
        Config().getString("kafka.client.dns.lookup")
      )
      .withProperty("acks", Config().getString("kafka.acks"))
  def producerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = producerSettings
      )
    )
}
