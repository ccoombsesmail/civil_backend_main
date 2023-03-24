package civil.models

import java.util.UUID

case class Civility(receivingUserId: String, commentId: UUID, value: Float)



case class CommentCivility(userId: String, commentId: UUID, value: Float)