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

case class DiscussionLike(
    eventType: String,
    spaceId: UUID,
    discussionId: UUID,
    receivingUserId: String,
    givingUserData: GivingUserNotificationData
)

object DiscussionLike {
  implicit val decoder: JsonDecoder[DiscussionLike] =
    DeriveJsonDecoder.gen[DiscussionLike]
  implicit val encoder: JsonEncoder[DiscussionLike] =
    DeriveJsonEncoder.gen[DiscussionLike]

  val discussionLikeSerde: Serde[Any, DiscussionLike] = Serde.string.inmapM {
    discussionLikeAsString =>
      ZIO.fromEither(
        discussionLikeAsString
          .fromJson[DiscussionLike]
          .left
          .map(new RuntimeException(_))
      )
  } { discussionLikeAsObj =>
    ZIO.attempt(discussionLikeAsObj.toJson)
  }
}
