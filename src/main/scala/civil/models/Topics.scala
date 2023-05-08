package civil.models

import civil.models.actions.LikeAction
import civil.models.enums.ReportStatus.Clean
import civil.models.enums.UserVerificationType.NO_VERIFICATION
import civil.models.enums.{LinkType, ReportStatus, TopicCategories, UserVerificationType}
import civil.repositories.QuillContext._

import java.time.ZonedDateTime
import java.util.UUID
import zio.json.{DeriveJsonCodec, JsonCodec}
import zio.{Random, Task, UIO, ZIO}

import scala.collection.mutable.ArrayBuffer

case class TopicId(
    id: UUID
)

object TopicId {

  def random: UIO[TopicId] = Random.nextUUID.map(TopicId(_))

  def fromString(id: String): Task[TopicId] =
    ZIO.attempt {
      TopicId(UUID.fromString(id))
    }

  implicit val codec: JsonCodec[TopicId] =
    DeriveJsonCodec.gen[TopicId]

}
case class IncomingTopic(
    title: String,
    editorState: String,
    editorTextContent: String,
    externalContentData: Option[ExternalContentData],
    evidenceLinks: Option[List[String]],
    category: String,
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
    createdByUserId: Option[String] = None,
    createdByUsername: Option[String] = None
)

object IncomingTopic {
  implicit val codec: JsonCodec[IncomingTopic] =
    DeriveJsonCodec.gen[IncomingTopic]
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

case class Topics(
    id: UUID,
    title: String,
    createdByUserId: String,
    createdByUsername: String,
    editorState: String,
    editorTextContent: String,
    evidenceLinks: Option[List[String]],
    likes: Int,
    category: String,
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
    topicWords: Seq[String] = Seq(),
    reportStatus: String = Clean.entryName,
    userVerificationType: UserVerificationType = NO_VERIFICATION,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    topicId: Option[UUID] = None,
    discussionId: Option[UUID] = None,
)

object Topics {
  implicit val codec: JsonCodec[Topics] = DeriveJsonCodec.gen[Topics]
}

case class OutgoingTopic(
    id: UUID,
    title: String,
    createdByUserId: String,
    createdByUsername: String,
    createdByTag: Option[String],
    editorState: String,
    externalContentData: Option[ExternalContentData],
    evidenceLinks: Option[List[String]],
    createdByIconSrc: String,
    likes: Int,
    likeState: LikeAction,
    category: TopicCategories,
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
    reportStatus: String = ReportStatus.Clean.entryName,
    topicCreatorIsDidUser: Boolean,
    userVerificationType: UserVerificationType = NO_VERIFICATION,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
    isFollowing: Boolean = false
)

object OutgoingTopic {
  implicit val codec: JsonCodec[OutgoingTopic] =
    DeriveJsonCodec.gen[OutgoingTopic]
}

case class OutgoingTopicsPayload(
    offset: Int,
    items: List[OutgoingTopic]
)

case class Words(topicWords: Seq[String])


case class TopicFollows(userId: String, followedTopicId: UUID)

object TopicFollows {
  implicit val codec: JsonCodec[TopicFollows] =
    DeriveJsonCodec.gen[TopicFollows]
}



case class ForYouTopics(id: Int, userId: String, topicIds: Seq[String])

object ForYouTopics {
//  implicit def arrayUUIDEncoder[Col <: Seq[UUID]]: Encoder[Col] = arrayRawEncoder[UUID, Col]("uuid")
//
//  implicit def arrayUUIDDecoder[Col <: Seq[UUID]](implicit bf: CBF[UUID, Col]): Decoder[Col] =
//    arrayRawDecoder[UUID, Col]
//
//
//
//  implicit val uuidListEncoder: Encoder[Seq[UUID]] = arrayUUIDEncoder[Seq[UUID]]
//
//  implicit val uuidListDecoder: Decoder[Seq[UUID]] = arrayUUIDDecoder[Seq[UUID]]
//
//  implicit val uuidListEncoder: Encoder[Seq[UUID]] =
//    encoder[Seq[UUID], ArrayBuffer[UUID]](_.to[ArrayBuffer], _.toSeq)
//
//  implicit val uuidListDecoder: Decoder[Seq[UUID]] =
//    decoder[Seq[UUID], List[UUID]](_.toList, _.toSeq)


}