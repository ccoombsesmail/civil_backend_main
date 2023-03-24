package civil.models

import civil.models.enums.ReportStatus.Clean
import civil.models.enums.UserVerificationType.NO_VERIFICATION
import civil.models.enums.{LinkType, ReportStatus, TopicCategories, UserVerificationType}

import java.time.{LocalDateTime, Instant}
import java.util.UUID
import io.getquill.Embedded

case class IncomingTopic(
    title: String,
    editorState: String,
    editorTextContent: String,
    externalContentData: Option[ExternalContentData],
    evidenceLinks: Option[List[String]],
    category: TopicCategories,
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
)

case class ExternalContentData(
    linkType: LinkType,
    externalContentUrl: String,
    embedId: Option[String],
    thumbImgUrl: Option[String]
)

case class Topics(
    id: UUID,
    title: String,
    createdByUserId: String,
    createdByUsername: String,
    editorState: String,
    editorTextContent: String,
    evidenceLinks: Option[List[String]],
    likes: Int,
    category: TopicCategories,
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
    topicWords: Seq[String] = Seq(),
    reportStatus: String = Clean.entryName,
    userVerificationType: UserVerificationType = NO_VERIFICATION,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    topicId: Option[UUID] = None,
    discussionId: Option[UUID] = None
 ) 

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
    likeState: Int,
    category: TopicCategories,
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
    reportStatus: String = ReportStatus.Clean.entryName,
    topicCreatorIsDidUser: Boolean,
    userVerificationType: UserVerificationType = NO_VERIFICATION,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
)

case class OutgoingTopicsPayload(
  offset: Int,
  items: List[OutgoingTopic]
)

case class Words(topicWords: Seq[String])
