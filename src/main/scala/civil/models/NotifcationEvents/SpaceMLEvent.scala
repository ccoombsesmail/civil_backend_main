package civil.models.NotifcationEvents

import civil.models.ExternalLinks
import zio.ZIO
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.serde.Serde

import java.util.UUID

case class SpaceMLEvent (
    eventType: String,
    spaceId: UUID,
    editorTextContent: String,
//    externalUrl: Option[ExternalLinks]
                        )


object SpaceMLEvent {
  implicit val decoder: JsonDecoder[SpaceMLEvent] =
    DeriveJsonDecoder.gen[SpaceMLEvent]
  implicit val encoder: JsonEncoder[SpaceMLEvent] =
    DeriveJsonEncoder.gen[SpaceMLEvent]

  val spaceMLEventSerde: Serde[Any, SpaceMLEvent] = Serde.string.inmapM {
    spaceLikeAsString =>
      ZIO.fromEither(
        spaceLikeAsString
          .fromJson[SpaceMLEvent]
          .left
          .map(new RuntimeException(_))
      )
  } { spaceLikeAsObj =>
    ZIO.attempt(spaceLikeAsObj.toJson)
  }
}
