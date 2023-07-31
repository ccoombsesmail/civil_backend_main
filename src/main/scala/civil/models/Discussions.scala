package civil.models

import civil.models.actions.LikeAction
import civil.models.enums.{ReportStatus, SpaceCategories}
import zio.{Random, Task, UIO, ZIO}
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.{Instant, LocalDateTime, ZonedDateTime}
import java.util.UUID

case class GeneralDiscussionId(
    id: UUID
)

object GeneralDiscussionId {
  implicit val codec: JsonCodec[GeneralDiscussionId] =
    DeriveJsonCodec.gen[GeneralDiscussionId]
}

case class DiscussionId(
    id: UUID
)

object DiscussionId {

  def random: UIO[DiscussionId] = Random.nextUUID.map(DiscussionId(_))

  def fromString(id: String): Task[DiscussionId] =
    ZIO.attempt {
      DiscussionId(UUID.fromString(id))
    }

  implicit val codec: JsonCodec[DiscussionId] =
    JsonCodec[UUID].transform(DiscussionId(_), _.id)
}

case class Discussions(
    id: UUID,
    spaceId: UUID,
    createdAt: ZonedDateTime,
    createdByUsername: String,
    createdByUserId: String,
    title: String,
    editorState: String,
    editorTextContent: String,
    evidenceLinks: Option[List[String]],
    likes: Int,
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
    discussionKeyWords: Seq[String] = Seq(),
    discussionId: Option[UUID] = None,
    contentHeight: Option[Float],
    popularityScore: Double,
    reportStatus: String = ReportStatus.CLEAN.entryName
)

object Discussions {
  implicit val codec: JsonCodec[Discussions] = DeriveJsonCodec.gen[Discussions]
}
case class IncomingDiscussion(
    spaceId: String,
    title: String,
    editorState: String,
    editorTextContent: String,
    externalContentData: Option[ExternalContentData],
    evidenceLinks: Option[List[String]],
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
    discussionKeyWords: Seq[String] = Seq(),
    discussionId: Option[UUID] = None,
    contentHeight: Option[Float]
)

object IncomingDiscussion {
  implicit val codec: JsonCodec[IncomingDiscussion] =
    DeriveJsonCodec.gen[IncomingDiscussion]
}

case class OutgoingDiscussion(
    id: UUID,
    spaceId: UUID,
    createdAt: ZonedDateTime,
    createdByUsername: String,
    createdByUserId: String,
    createdByIconSrc: String,
    createdByTag: Option[String],
    title: String,
    editorState: Option[String],
    evidenceLinks: Option[List[String]],
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
    externalContentData: Option[ExternalContentData],
    likes: Int,
    likeState: LikeAction,
    allComments: Long,
    positiveComments: Long,
    neutralComments: Long,
    negativeComments: Long,
    totalCommentsAndReplies: Long,
    contentHeight: Option[Float] = None,
    spaceTitle: Option[String] = None,
    spaceCategory: Option[SpaceCategories] = Some(SpaceCategories.General),
    reportStatus: String = ReportStatus.CLEAN.entryName,
    isFollowing: Boolean = false
)

object OutgoingDiscussion {
  implicit val codec: JsonCodec[OutgoingDiscussion] =
    DeriveJsonCodec.gen[OutgoingDiscussion]
}

case class DiscussionSimilarityScores(
    discussionId1: UUID,
    discussionId2: UUID,
    similarityScore: Float
)
