package civil.models

import civil.models.actions.LikeAction
import civil.models.enums.ReportStatus.CLEAN
import civil.models.enums.UserVerificationType.NO_VERIFICATION
import civil.models.enums.{
  LinkType,
  ReportStatus,
  SpaceCategories,
  UserVerificationType
}

import java.time.ZonedDateTime
import java.util.UUID
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.{Random, Task, UIO, ZIO}

case class SpaceId(
    id: UUID
)

object SpaceId {

  def random: UIO[SpaceId] = Random.nextUUID.map(SpaceId(_))

  def fromString(id: String): Task[SpaceId] =
    ZIO.attempt {
      SpaceId(UUID.fromString(id))
    }

  implicit val codec: JsonCodec[SpaceId] =
    DeriveJsonCodec.gen[SpaceId]

}
case class IncomingSpace(
    title: String,
    editorState: String,
    editorTextContent: String,
    category: String,
    referenceLinks: Option[List[String]],
    //    userUploadedImageUrl: Option[String],
    //    userUploadedVodUrl: Option[String],
    createdByUserId: Option[String] = None,
    createdByUsername: Option[String] = None,
    contentHeight: Option[Float]
)

object IncomingSpace {
  implicit val codec: JsonCodec[IncomingSpace] =
    DeriveJsonCodec.gen[IncomingSpace]
}

case class ExternalContentData(
    linkType: LinkType,
    externalContentUrl: String,
    embedId: Option[String],
    thumbImgUrl: Option[String]
)

object ExternalContentData {
  implicit val codec: JsonCodec[ExternalContentData] =
    DeriveJsonCodec.gen[ExternalContentData]
}

case class Spaces(
    id: UUID,
    title: String,
    createdByUserId: String,
    createdByUsername: String,
    editorState: String,
    editorTextContent: String,
    likes: Int,
    category: String,
    reportStatus: String = CLEAN.entryName,
    userVerificationType: UserVerificationType = NO_VERIFICATION,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    spaceId: Option[UUID] = None,
    discussionId: Option[UUID] = None,
    referenceLinks: Option[List[String]] = None,
    contentHeight: Option[Float]
)

object Spaces {
  implicit val codec: JsonCodec[Spaces] = DeriveJsonCodec.gen[Spaces]
}

case class OutgoingSpace(
    id: UUID,
    title: String,
    createdByUserId: String,
    createdByUsername: String,
    createdByTag: Option[String],
    editorState: String,
    createdByIconSrc: String,
    likes: Int,
    likeState: LikeAction,
    category: SpaceCategories,
    reportStatus: String = ReportStatus.CLEAN.entryName,
    userVerificationType: UserVerificationType = NO_VERIFICATION,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    isFollowing: Boolean = false,
    referenceLinks: Option[List[String]] = None,
    contentHeight: Option[Float],
    editorTextContent: String
)

object OutgoingSpace {
  implicit val codec: JsonCodec[OutgoingSpace] =
    DeriveJsonCodec.gen[OutgoingSpace]
}

case class OutgoingSpacesPayload(
    offset: Int,
    items: List[OutgoingSpace]
)

case class Words(spaceWords: Seq[String])

case class SpaceFollows(userId: String, followedSpaceId: UUID)

object SpaceFollows {
  implicit val codec: JsonCodec[SpaceFollows] =
    DeriveJsonCodec.gen[SpaceFollows]
}

case class SpaceSimilarityScores(
    spaceId1: UUID,
    spaceId2: UUID,
    similarityScore: Float
)

case class ForYouSpaces(id: Int, userId: String, spaceIds: Seq[String])
