package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class CommentLikes(commentId: UUID, userId: String, value: Int)

object CommentLikes {
  implicit val codec: JsonCodec[CommentLikes] = DeriveJsonCodec.gen[CommentLikes]
}

case class UpdateCommentLikes(
    id: UUID,
    value: Int,
    createdByUserId: String
)

object UpdateCommentLikes {
  implicit val codec: JsonCodec[UpdateCommentLikes] = DeriveJsonCodec.gen[UpdateCommentLikes]
}


case class CommentLiked(
    commentId: UUID,
    likes: Int,
    likeState: Int,
    rootId: Option[UUID],
)

object CommentLiked {
  implicit val codec: JsonCodec[CommentLiked] = DeriveJsonCodec.gen[CommentLiked]
}

case class TopicLikes(topicId: UUID, userId: String, value: Int)

object TopicLikes {
  implicit val codec: JsonCodec[TopicLikes] = DeriveJsonCodec.gen[TopicLikes]
}


case class UpdateTopicLikes(id: UUID, value: Int, createdByUserId: String)

object UpdateTopicLikes {
  implicit val codec: JsonCodec[UpdateTopicLikes] = DeriveJsonCodec.gen[UpdateTopicLikes]
}

case class TopicLiked(id: UUID, likes: Int, likeState: Int)

object TopicLiked {
  implicit val codec: JsonCodec[TopicLiked] = DeriveJsonCodec.gen[TopicLiked]
}
