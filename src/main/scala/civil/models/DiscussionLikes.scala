package civil.models

import civil.models.actions.LikeAction
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID


case class DiscussionLikes(discussionId: UUID, userId: String, likeState: LikeAction)

object DiscussionLikes {
  implicit val codec: JsonCodec[DiscussionLikes] = DeriveJsonCodec.gen[DiscussionLikes]
}


case class UpdateDiscussionLikes(id: UUID, likeAction: LikeAction, createdByUserId: Option[String] = None)

object UpdateDiscussionLikes {
  implicit val codec: JsonCodec[UpdateDiscussionLikes] = DeriveJsonCodec.gen[UpdateDiscussionLikes]
}

case class DiscussionLiked(id: UUID, likes: Int, likeState: LikeAction)

object DiscussionLiked {
  implicit val codec: JsonCodec[DiscussionLiked] = DeriveJsonCodec.gen[DiscussionLiked]
}


case class UpdateDiscussionFollows(id: UUID, createdByUserId: Option[String] = None)

object UpdateDiscussionFollows {
  implicit val codec: JsonCodec[UpdateDiscussionFollows] = DeriveJsonCodec.gen[UpdateDiscussionFollows]
}