package civil.models.NotifcationEvents

import zio.ZIO
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.serde.Serde

import java.util.UUID

case class CommentLike(
    eventType: String,
    commentId: UUID,
    receivingUserId: String,
    givingUserData: GivingUserNotificationData,
    topicId: UUID,
    subtopicId: UUID
)

object CommentLike {
  implicit val decoder: JsonDecoder[CommentLike] =
    DeriveJsonDecoder.gen[CommentLike]
  implicit val encoder: JsonEncoder[CommentLike] =
    DeriveJsonEncoder.gen[CommentLike]

  val commentLikeSerde: Serde[Any, CommentLike] = Serde.string.inmapM {
    commentLikeAsString =>
      ZIO.fromEither(
        commentLikeAsString
          .fromJson[CommentLike]
          .left
          .map(new RuntimeException(_))
      )
  } { commentLikeAsObj =>
    ZIO.attempt(commentLikeAsObj.toJson)
  }
}
