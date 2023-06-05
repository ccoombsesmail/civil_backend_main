package civil.models

import civil.models.actions.LikeAction
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class CommentLikes(commentId: UUID, userId: String, likeState: LikeAction)

object CommentLikes {
  implicit val codec: JsonCodec[CommentLikes] = DeriveJsonCodec.gen[CommentLikes]
}

case class UpdateCommentLikes(
    id: UUID,
    likeAction: LikeAction,
    createdByUserId: String
)

object UpdateCommentLikes {
  implicit val codec: JsonCodec[UpdateCommentLikes] = DeriveJsonCodec.gen[UpdateCommentLikes]
}


case class CommentLiked(
    commentId: UUID,
    likes: Int,
    likeState: LikeAction,
    rootId: Option[UUID],
)

object CommentLiked {
  implicit val codec: JsonCodec[CommentLiked] = DeriveJsonCodec.gen[CommentLiked]
}

case class SpaceLikes(spaceId: UUID, userId: String, likeState: LikeAction)

object SpaceLikes {
  implicit val codec: JsonCodec[SpaceLikes] = DeriveJsonCodec.gen[SpaceLikes]
}


case class UpdateSpaceLikes(id: UUID, likeAction: LikeAction, createdByUserId: Option[String] = None)

object UpdateSpaceLikes {
  implicit val codec: JsonCodec[UpdateSpaceLikes] = DeriveJsonCodec.gen[UpdateSpaceLikes]
}

case class SpaceLiked(id: UUID, likes: Int, likeState: LikeAction)

object SpaceLiked {
  implicit val codec: JsonCodec[SpaceLiked] = DeriveJsonCodec.gen[SpaceLiked]
}


case class UpdateSpaceFollows(id: UUID, createdByUserId: Option[String] = None)

object UpdateSpaceFollows {
  implicit val codec: JsonCodec[UpdateSpaceFollows] = DeriveJsonCodec.gen[UpdateSpaceFollows]
}