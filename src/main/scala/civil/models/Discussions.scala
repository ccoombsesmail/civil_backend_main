package civil.models

import java.time.{LocalDateTime, Instant};
import java.util.UUID


case class GeneralDiscussionId(
    id: UUID
 )

case class Discussions(
    id: UUID,
    topicId: UUID,
    createdAt: LocalDateTime,
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
    discussionId: Option[UUID] = None
   )

case class IncomingDiscussion(
    topicId: String,
    title: String,
    editorState: String,
    editorTextContent: String,
    externalContentData: Option[ExternalContentData],
    evidenceLinks: Option[List[String]],
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String]
)

case class OutgoingDiscussion(
    id: UUID,
    topicId: UUID,
    createdAt: LocalDateTime,
    createdByUsername: String,
    createdByUserId: String,
    createdByIconSrc: String,
    title: String,
    editorState: String,
    evidenceLinks: Option[List[String]],
    userUploadedImageUrl: Option[String],
    userUploadedVodUrl: Option[String],
    externalContentData: Option[ExternalContentData],
    likes: Int,
    liked: Boolean,
    allComments: Long,
    positiveComments: Long,
    neutralComments: Long,
    negativeComments: Long,
    totalCommentsAndReplies: Long
)
