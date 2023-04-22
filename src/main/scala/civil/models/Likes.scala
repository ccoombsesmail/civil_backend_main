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

case class TopicLikes(topicId: UUID, userId: String, likeState: LikeAction)

object TopicLikes {
  implicit val codec: JsonCodec[TopicLikes] = DeriveJsonCodec.gen[TopicLikes]
}


case class UpdateTopicLikes(id: UUID, likeAction: LikeAction)

object UpdateTopicLikes {
  implicit val codec: JsonCodec[UpdateTopicLikes] = DeriveJsonCodec.gen[UpdateTopicLikes]
}

case class TopicLiked(id: UUID, likes: Int, likeState: LikeAction)

object TopicLiked {
  implicit val codec: JsonCodec[TopicLiked] = DeriveJsonCodec.gen[TopicLiked]
}
