package civil.services

import civil.config.Config
import civil.models.InternalServerError
import zio._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde


trait KafkaProducerService {
  def publish[A](message: A, userId: String, serde: Serde[Any, A], topic: String): Unit
}


class KafkaProducerServiceLive extends KafkaProducerService {
  val producerSettings: ProducerSettings =
    ProducerSettings(List(Config().getString("kafka.bootstrap.servers")))
      .withProperty("security.protocol", Config().getString("kafka.security.protocol"))
      .withProperty("sasl.jaas.config", Config().getString("kafka.sasl.jaas.config"))
      .withProperty("sasl.mechanism", Config().getString("kafka.sasl.mechanism"))
      .withProperty("client.dns.lookup", Config().getString("kafka.client.dns.lookup"))
      .withProperty("acks", Config().getString("kafka.acks"))

  private val producer = Producer.make(producerSettings)
  val runtime = zio.Runtime.default


  override def publish[A](message: A, userId: String, serde: Serde[Any, A], topic: String = "notifications"): Unit = {
    println(s"Publishing to $topic ")
    val producerEffect = for {
      p <- producer
    } yield p.produce(
      topic,
      userId,
      message,
      Serde.string,
      serde
    )
    val producerFlushEffect = for {
      p <- producer
    } yield p.flush

//    val producerEffect = {
//      producer.produce(
//        "notifications",
//        userId,
//        message,
//        Serde.string,
//        serde
//      )
//    }
    runtime.unsafeRunAsync(for {
      record <- producerEffect.use(identity)
    } yield record)(k => k.mapError(e => InternalServerError(e.toString)))

    runtime.unsafeRunAsync(for {
      _ <- producerFlushEffect.use(identity)
    } yield ())(k => k.mapError(e => InternalServerError(e.toString)))
  }
}

object KafkaProducerService {


}

