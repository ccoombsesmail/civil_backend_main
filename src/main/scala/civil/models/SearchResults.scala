package civil.models

import civil.models.enums.ReportStatus
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class Space(id: UUID, editorTextContent: String)

object Space {
  implicit val codec: JsonCodec[Space] = DeriveJsonCodec.gen[Space]
}
case class Discussion(id: UUID, editorTextContent: String, spaceId: UUID)

object Discussion {
  implicit val codec: JsonCodec[Discussion] = DeriveJsonCodec.gen[Discussion]
}
case class Comment(
    id: UUID,
    editorTextContent: String,
    spaceId: UUID,
    discussionId: UUID
)

object Comment {
  implicit val codec: JsonCodec[Comment] = DeriveJsonCodec.gen[Comment]
}

case class CommentWithUserData(
    id: UUID,
    editorTextContent: String,
    spaceId: UUID,
    discussionId: UUID,
    createdByUserId: String,
    createdByUsername: String,
    createdByTag: Option[String],
    createdByIconSrc: String,
    reportStatus: String = ReportStatus.CLEAN.entryName
)

object CommentWithUserData {
  implicit val codec: JsonCodec[CommentWithUserData] =
    DeriveJsonCodec.gen[CommentWithUserData]
}
case class User(
    userId: String,
    iconSrc: Option[String],
    tag: Option[String],
    username: String,
    bio: Option[String] = None
)

object User {
  implicit val codec: JsonCodec[User] = DeriveJsonCodec.gen[User]
}

case class SearchResult(
    space: Option[Space] = None,
    discussion: Option[Discussion] = None,
    comment: Option[Comment] = None,
    user: User
)

object SearchResult {
  implicit val codec: JsonCodec[SearchResult] =
    DeriveJsonCodec.gen[SearchResult]
}
