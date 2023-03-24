package civil.models

import java.util.UUID

case class Topic(id: UUID, editorTextContent: String)
case class Discussion(id: UUID, editorTextContent: String, topicId: UUID)
case class Comment(id: UUID, editorTextContent: String, topicId: UUID, discussionId: UUID)
case class User(userId: String, iconSrc: Option[String], tag: Option[String], username: String, bio: Option[String] = None)

case class SearchResult(
    topic: Option[Topic] = None,
    discussion: Option[Discussion] = None,
    comment: Option[Comment] = None,
    user: User,
)
