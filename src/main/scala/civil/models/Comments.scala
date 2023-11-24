package civil.models

import civil.models.actions.{LikeAction, NeutralState}
import civil.models.enums.ReportStatus.CLEAN
import civil.models.enums.TribunalCommentType
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.UUID
import io.scalaland.chimney.dsl._
import zio.{Random, Task, UIO, ZIO}
import zio.json.{DeriveJsonCodec, JsonCodec}

case class CommentId(id: UUID)

object CommentId {

  def random: UIO[CommentId] = Random.nextUUID.map(CommentId(_))

  def fromString(id: String): Task[CommentId] =
    ZIO.attempt {
      CommentId(UUID.fromString(id))
    }

  implicit val codec: JsonCodec[CommentId] =
    JsonCodec[UUID].transform(CommentId(_), _.id)
}

case class CommentNode(data: CommentReply, children: List[CommentNode])

object CommentNode {
  //  implicit val codec: JsonCodec[CommentNode] =
  //    DeriveJsonCodec.gen[CommentNode]

  implicit val encoder: Encoder[CommentNode] = deriveEncoder[CommentNode]

}

case class EntryWithDepth(comment: CommentReply, depth: Int)

object Comments {
  def commentToCommentReplyWithDepth(
      commentWithDepth: CommentWithDepth,
      likeState: LikeAction,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String],
      createdByTag: Option[String],
      createdByUsername: String,
      createdByCivility: Long,
      numFollowers: Option[Int]
  ): EntryWithDepth =
    EntryWithDepth(
      commentWithDepth
        .into[CommentReply]
        .withFieldConst(_.likeState, likeState)
        .withFieldConst(_.civility, civility)
        .withFieldConst(_.createdByUserData, CreatedByUserData(
          createdByUsername = createdByUsername,
          createdByUserId = userId,
          createdByIconSrc = iconSrc,
          createdByTag = createdByTag,
          civilityPoints = createdByCivility,
          numFollowers = numFollowers,
          numFollowed = None,
          numPosts = None,
          createdByExperience = createdByExperience
        ))
        .withFieldConst(_.source, commentWithDepth.source)
        .transform,
      commentWithDepth.depth
    )

  def commentToCommentReply(
      comment: CommentWithDepth,
      likeState: LikeAction,
      civility: Float,
      iconSrc: String,
      userId: String,
      createdByExperience: Option[String],
      createdByTag: Option[String],
      createdByUsername: String,
      createdByCivility: Long,
      numFollowers: Option[Int]
  ): CommentReply =
    comment
      .into[CommentReply]
      .withFieldConst(_.likeState, likeState)
      .withFieldConst(_.civility, civility)
      .withFieldConst(_.createdByUserData, CreatedByUserData(
        createdByUsername = createdByUsername,
        createdByUserId = userId,
        createdByIconSrc = iconSrc,
        createdByTag = createdByTag,
        civilityPoints = createdByCivility,
        numFollowers = numFollowers,
        numFollowed = None,
        numPosts = None,
        createdByExperience = createdByExperience
      ))
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
    createdAt: ZonedDateTime,
    likes: Int,
    rootId: Option[UUID],
    depth: Int,
    source: Option[String],
    reportStatus: String = CLEAN.entryName,
    toxicityStatus: Option[String] = None,
    userIconSrc: Option[String],
    userExperience: Option[String],
    createdByTag: Option[String],
    createdByCivility: Long,
    numFollowers: Int,
    userId: String,
    likeState: Option[LikeAction] = Some(NeutralState),
    civility: Option[Float]
)

case class CommentWithDepthAndUserUnauthenticated(
    id: UUID,
    editorState: String,
    createdByUsername: String,
    sentiment: String,
    discussionId: UUID,
    parentId: Option[UUID],
    createdAt: ZonedDateTime,
    likes: Int,
    rootId: Option[UUID],
    depth: Int,
    source: Option[String],
    reportStatus: String = CLEAN.entryName,
    toxicityStatus: Option[String] = None,
    userIconSrc: Option[String],
    userExperience: Option[String],
    createdByTag: Option[String],
    createdByCivility: Long,
    numFollowers: Int,
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
    spaceId: UUID,
    parentId: Option[UUID],
    createdAt: ZonedDateTime,
    likes: Int,
    rootId: Option[UUID],
    source: Option[String],
    reportStatus: String = CLEAN.entryName,
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
    spaceId: UUID,
    commentType: TribunalCommentType = TribunalCommentType.General,
    createdByUserId: Option[String] = None
)

object IncomingComment {
  implicit val codec: JsonCodec[IncomingComment] =
    DeriveJsonCodec.gen[IncomingComment]
}

case class CommentReply(
    id: UUID,
    editorState: String,
    sentiment: String,
    discussionId: UUID,
    parentId: Option[UUID],
    createdAt: ZonedDateTime,
    likes: Int,
    rootId: Option[UUID],
    likeState: LikeAction,
    civility: Float,
    source: Option[String],
    reportStatus: String = CLEAN.entryName,
    toxicityStatus: Option[String] = None,
    createdByUserData: CreatedByUserData
)

case class CommentReplyWithParent(
    id: UUID,
    editorState: String,
    createdByUsername: String,
    createdByUserId: String,
    createdByTag: Option[String],
    sentiment: String,
    discussionId: UUID,
    parentId: Option[UUID],
    createdAt: ZonedDateTime,
    likes: Int,
    rootId: Option[UUID],
    likeState: LikeAction,
    civility: Float,
    source: Option[String],
    createdByIconSrc: String,
    createdByExperience: Option[String],
    reportStatus: String = CLEAN.entryName,
    toxicityStatus: Option[String] = None,
    parentComment: Option[Comments] = None
)

object CommentReply {
//    implicit val codec: JsonCodec[CommentReply] = DeriveJsonCodec.gen[CommentReply]

  implicit val encoder: Encoder[CommentReply] = deriveEncoder[CommentReply]
}

case class CommentWithDepth(
    id: UUID,
    editorState: String,
    createdByUsername: String,
    sentiment: String,
    discussionId: UUID,
    parentId: Option[UUID],
    createdAt: ZonedDateTime,
    likes: Int,
    rootId: Option[UUID],
    depth: Int,
    source: Option[String],
    reportStatus: String = CLEAN.entryName,
    toxicityStatus: Option[String] = None
)

case class CommentWithReplies(
    replies: List[CommentNode],
    comment: CommentReply
)

object CommentWithReplies {
  implicit val encoder: Encoder[CommentWithReplies] =
    deriveEncoder[CommentWithReplies]

}
