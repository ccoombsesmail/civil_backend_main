package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class Space(id: UUID, editorTextContent: String)

object Space {
  implicit val codec: JsonCodec[Space] = DeriveJsonCodec.gen[Space]
}
case class Discussion(id: UUID, editorTextContent: String, topicId: UUID)

object Discussion {
  implicit val codec: JsonCodec[Discussion] = DeriveJsonCodec.gen[Discussion]
}
case class Comment(id: UUID, editorTextContent: String, topicId: UUID, discussionId: UUID)

object Comment {
  implicit val codec: JsonCodec[Comment] = DeriveJsonCodec.gen[Comment]
}
case class User(userId: String, iconSrc: Option[String], tag: Option[String], username: String, bio: Option[String] = None)

object User {
  implicit val codec: JsonCodec[User] = DeriveJsonCodec.gen[User]
}

case class SearchResult(
    space: Option[Space] = None,
    discussion: Option[Discussion] = None,
    comment: Option[Comment] = None,
    user: User,
)


object SearchResult {
  implicit val codec: JsonCodec[SearchResult] = DeriveJsonCodec.gen[SearchResult]
}