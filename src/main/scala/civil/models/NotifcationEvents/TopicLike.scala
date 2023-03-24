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

case class TopicLike(
    eventType: String,
    topicId: UUID,
    receivingUserId: String,
    givingUserData: GivingUserNotificationData,
)

object TopicLike {
  implicit val decoder: JsonDecoder[TopicLike] =
    DeriveJsonDecoder.gen[TopicLike]
  implicit val encoder: JsonEncoder[TopicLike] =
    DeriveJsonEncoder.gen[TopicLike]

  val topicLikeSerde: Serde[Any, TopicLike] = Serde.string.inmapM {
    topicLikeAsString =>
      ZIO.fromEither(
        topicLikeAsString
          .fromJson[TopicLike]
          .left
          .map(new RuntimeException(_))
      )
  } { topicLikeAsObj =>
    ZIO.effect(topicLikeAsObj.toJson)
  }
}
