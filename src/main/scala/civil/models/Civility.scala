package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class UpdateCommentCivility(
    receivingUserId: String,
    commentId: UUID,
    value: Float
)

object UpdateCommentCivility {
  implicit val codec: JsonCodec[UpdateCommentCivility] =
    DeriveJsonCodec.gen[UpdateCommentCivility]
}

case class CommentCivility(userId: String, commentId: UUID, value: Float)

object CommentCivility {
  implicit val codec: JsonCodec[CommentCivility] =
    DeriveJsonCodec.gen[CommentCivility]
}

case class CivilityGivenResponse(
    civility: Float,
    commentId: UUID,
    rootId: Option[UUID]
)

object CivilityGivenResponse {
  implicit val codec: JsonCodec[CivilityGivenResponse] =
    DeriveJsonCodec.gen[CivilityGivenResponse]
}
