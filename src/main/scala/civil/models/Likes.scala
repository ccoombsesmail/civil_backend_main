package civil.models

import java.util.UUID

case class CommentLikes(commentId: UUID, userId: String, value: Int)

case class UpdateCommentLikes(
    id: UUID,
    value: Int,
    createdByUserId: String
)

case class CommentLiked(
    commentId: UUID,
    likes: Int,
    likeState: Int,
    rootId: Option[UUID],
)

case class TopicLikes(topicId: UUID, userId: String, value: Int)

case class UpdateTopicLikes(id: UUID, value: Int, createdByUserId: String)

case class TopicLiked(id: UUID, likes: Int, likeState: Int)
