package civil.models.NotifcationEvents

import civil.models.ExternalLinks
import zio.ZIO
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.serde.Serde

import java.util.UUID

case class TopicMLEvent (
    eventType: String,
    topicId: UUID,
    editorTextContent: String,
    externalUrl: Option[ExternalLinks]
                        )


object TopicMLEvent {
  implicit val decoder: JsonDecoder[TopicMLEvent] =
    DeriveJsonDecoder.gen[TopicMLEvent]
  implicit val encoder: JsonEncoder[TopicMLEvent] =
    DeriveJsonEncoder.gen[TopicMLEvent]

  val topicMLEventSerde: Serde[Any, TopicMLEvent] = Serde.string.inmapM {
    topicLikeAsString =>
      ZIO.fromEither(
        topicLikeAsString
          .fromJson[TopicMLEvent]
          .left
          .map(new RuntimeException(_))
      )
  } { topicLikeAsObj =>
    ZIO.attempt(topicLikeAsObj.toJson)
  }
}
