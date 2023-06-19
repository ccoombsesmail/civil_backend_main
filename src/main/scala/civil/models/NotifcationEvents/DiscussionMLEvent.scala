package civil.models.NotifcationEvents

import civil.models.ExternalLinksDiscussions
import zio.ZIO
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.serde.Serde

import java.util.UUID

case class DiscussionMLEvent (
                               eventType: String,
                               discussionId: UUID,
                               editorTextContent: String,
                                   externalUrl: Option[ExternalLinksDiscussions]
                             )


object DiscussionMLEvent {
  implicit val decoder: JsonDecoder[DiscussionMLEvent] =
    DeriveJsonDecoder.gen[DiscussionMLEvent]
  implicit val encoder: JsonEncoder[DiscussionMLEvent] =
    DeriveJsonEncoder.gen[DiscussionMLEvent]

  val discussionMLEventSerde: Serde[Any, DiscussionMLEvent] = Serde.string.inmapM {
    discussionMLEventAsString =>
      ZIO.fromEither(
        discussionMLEventAsString
          .fromJson[DiscussionMLEvent]
          .left
          .map(new RuntimeException(_))
      )
  } {  discussionMLEventAsObj =>
    ZIO.attempt(discussionMLEventAsObj.toJson)
  }
}
