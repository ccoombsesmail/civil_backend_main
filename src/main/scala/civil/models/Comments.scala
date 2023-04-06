package civil.models

import civil.models.enums.ReportStatus.Clean
import civil.models.enums.{ReportStatus, Sentiment}

import java.time.LocalDateTime
import java.util.UUID
import io.scalaland.chimney.dsl._

case class CommentNode(data: CommentReply, children: Seq[CommentNode])
case class EntryWithDepth(comment: CommentReply, depth: Int)

object Comments {
  def commentToCommentReplyWithDepth(
      commentWithDepth: CommentWithDepth,
      likeState: Int,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String]
  ) =
    EntryWithDepth(
      commentWithDepth
        .into[CommentReply]
        .withFieldConst(_.likeState, likeState)
        .withFieldConst(_.civility, civility)
        .withFieldConst(_.createdByIconSrc, iconSrc)
        .withFieldConst(_.createdByUserId, userId)
        .withFieldConst(_.createdByExperience, createdByExperience)
        .withFieldConst(_.source, commentWithDepth.source)
        .transform,
      commentWithDepth.depth
    )

  def commentToCommentReply(
      comment: CommentWithDepth,
      likeState: Int,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String]
  ) =
    comment
      .into[CommentReply]
      .withFieldConst(_.likeState, 0)
      .withFieldConst(_.civility, civility)
      .withFieldConst(_.createdByIconSrc, iconSrc)
      .withFieldConst(_.createdByUserId, userId)
      .withFieldConst(_.createdByExperience, createdByExperience)
      .withFieldConst(_.source, comment.source)
      .transform
}

case class CommentWithDepthAndUser(
    id: UUID,
    editorState: String,
    createdByUsername: String,
    sentiment: String,
    discussionId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    depth: Int,
    source: Option[String],
    reportStatus: String = Clean.entryName,
    toxicityStatus: Option[String] = None,
    userIconSrc: Option[String],
    userExperience: Option[String],
    userId: String
)

case class Comments(
    id: UUID,
    editorState: String,
    editorTextContent: String,
    createdByUserId: String,
    createdByUsername: String,
    sentiment: String,
    discussionId: UUID,
    topicId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    source: Option[String],
    reportStatus: String = Clean.entryName,
    toxicityStatus: Option[String] =
      None // TODO: turn this into enum with varius toxicity values (profanity, racism etc.)
)

case class IncomingComment(
    editorState: String,
    editorTextContent: String,
    createdByUsername: String,
    contentId: UUID,
    parentId: Option[UUID],
    rootId: Option[UUID],
    source: Option[String],
    toxicityStatus: Option[String] = None,
    topicId: UUID
)

case class OutgoingComment(
    id: UUID,
    editorState: String,
    createdByUsername: String,
    createdByUserId: String,
    sentiment: String,
    discussionId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    likeState: Int,
    civility: Float,
    source: Option[String],
    createdByIconSrc: String,
    rootId: Option[UUID],
    reportStatus: String,
    toxicityStatus: Option[String] = None
)

case class CommentReply(
    id: UUID,
    editorState: String,
    createdByUsername: String,
    createdByUserId: String,
    sentiment: String,
    discussionId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    likeState: Int,
    civility: Float,
    source: Option[String],
    createdByIconSrc: String,
    createdByExperience: Option[String],
    reportStatus: String = Clean.entryName,
    toxicityStatus: Option[String] = None
)

case class CommentWithDepth(
    id: UUID,
    editorState: String,
    createdByUsername: String,
    sentiment: String,
    discussionId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    depth: Int,
    source: Option[String],
    reportStatus: String = Clean.entryName,
    toxicityStatus: Option[String] = None
)

case class CommentWithReplies(
    replies: List[CommentNode],
    comment: CommentReply
)

case class UpdateLikes(id: UUID, userId: String, increment: Boolean)

case class Liked(
    commentId: UUID,
    likes: Int,
    liked: Boolean,
    rootId: Option[UUID]
)

case class CivilityGiven(
    civility: Float,
    commentId: UUID,
    rootId: Option[UUID]
)
