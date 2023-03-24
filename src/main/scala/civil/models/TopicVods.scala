package civil.models

import java.util.UUID

case class TopicVods(
  userId: String,
  vodUrl: String,
  topicId: UUID
)