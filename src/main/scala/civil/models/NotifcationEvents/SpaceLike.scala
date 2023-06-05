package civil.models.NotifcationEvents

import zio.ZIO
import zio.json.{
  DecoderOps,
  DeriveJsonDecoder,
  DeriveJsonEncoder,
  EncoderOps,
  JsonDecoder,
  JsonEncoder
}
import zio.kafka.serde.Serde

import java.util.UUID

case class SpaceLike(
    eventType: String,
    spaceId: UUID,
    receivingUserId: String,
    givingUserData: GivingUserNotificationData,
)

object SpaceLike {
  implicit val decoder: JsonDecoder[SpaceLike] =
    DeriveJsonDecoder.gen[SpaceLike]
  implicit val encoder: JsonEncoder[SpaceLike] =
    DeriveJsonEncoder.gen[SpaceLike]

  val spaceLikeSerde: Serde[Any, SpaceLike] = Serde.string.inmapM {
    spaceLikeAsString =>
      ZIO.fromEither(
        spaceLikeAsString
          .fromJson[SpaceLike]
          .left
          .map(new RuntimeException(_))
      )
  } { spaceLikeAsObj =>
    ZIO.attempt(spaceLikeAsObj.toJson)
  }
}
