package civil.models

import civil.models.enums.TribunalCommentType
import io.scalaland.chimney.dsl.TransformerOps
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.LocalDateTime
import java.util.UUID

case class TribunalCommentNode(
    data: TribunalCommentsReply,
    children: Seq[TribunalCommentNode]
)

object TribunalCommentNode {
  implicit val codec: JsonCodec[TribunalCommentNode] =
    DeriveJsonCodec.gen[TribunalCommentNode]
}
case class TribunalEntryWithDepth(comment: TribunalCommentsReply, depth: Int)

object TribunalComments {
  def withDepthToReplyWithDepth(
      commentWithDepth: TribunalCommentWithDepthAndUser,
      likeState: Int,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String]
  ) =
    TribunalEntryWithDepth(
      commentWithDepth
        .into[TribunalCommentsReply]
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
      comment: TribunalCommentWithDepthAndUser,
      likeState: Int,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String]
  ) =
    comment
      .into[TribunalCommentsReply]
      .withFieldConst(_.likeState, likeState)
      .withFieldConst(_.civility, civility)
      .withFieldConst(_.createdByIconSrc, iconSrc)
      .withFieldConst(_.createdByUserId, userId)
      .withFieldConst(_.createdByExperience, createdByExperience)
      .withFieldConst(_.source, comment.source)
      .transform
}

case class TribunalComments(
    id: UUID,
    editorState: String,
    editorTextContent: String,
    createdByUserId: String,
    createdByUsername: String,
    sentiment: String,
    reportedContentId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    source: Option[String],
    commentType: TribunalCommentType = TribunalCommentType.General
)

case class TribunalCommentsReply(
    id: UUID,
    editorState: String,
    editorTextContent: String,
    createdByUserId: String,
    createdByUsername: String,
    sentiment: String,
    reportedContentId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    likeState: Int,
    civility: Float,
    source: Option[String],
    createdByIconSrc: String,
    createdByExperience: Option[String],
    commentType: TribunalCommentType
)

object TribunalCommentsReply {
  implicit val codec: JsonCodec[TribunalCommentsReply] =
    DeriveJsonCodec.gen[TribunalCommentsReply]
}

case class TribunalCommentWithDepth(
    id: UUID,
    editorState: String,
    editorTextContent: String,
    createdByUsername: String,
    createdByUserId: String,
    sentiment: String,
    reportedContentId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    depth: Int,
    source: Option[String],
    commentType: TribunalCommentType
)

case class TribunalCommentWithDepthAndUser(
    id: UUID,
    editorState: String,
    editorTextContent: String,
    createdByUsername: String,
    createdByUserId: String,
    sentiment: String,
    reportedContentId: UUID,
    parentId: Option[UUID],
    createdAt: LocalDateTime,
    likes: Int,
    rootId: Option[UUID],
    depth: Int,
    source: Option[String],
    commentType: TribunalCommentType,
    userIconSrc: Option[String],
    userExperience: Option[String],
    userId: String
)

case class TribunalCommentsBatchResponse(
    Reporter: List[TribunalCommentNode],
    Defendant: List[TribunalCommentNode],
    Jury: List[TribunalCommentNode],
    General: List[TribunalCommentNode]
)
