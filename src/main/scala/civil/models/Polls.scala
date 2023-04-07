package civil.models
import civil.repositories.QuillContextHelper.insertMeta
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

trait PrimaryKey {
  var id: Long = 0
//
//  implicit val pollOptionsInsertMeta = insertMeta[PollOptions](_.id)
  implicit val pollVotesInsertMeta = insertMeta[PollVotes](_.id)

}

object Polls {
  implicit val pollsInsertMeta = insertMeta[Polls](_.id)

}

case class Polls(
    id: Long = 0,
    contentId: UUID,
    question: String,
    version: Int
)

case class PollOptions(
    pollId: Long,
    uid: String,
    text: String
) extends AnyRef
    with PrimaryKey

case class IncomingPollVote(
    pollOptionId: UUID
)
object IncomingPollVote {
  implicit val codec: JsonCodec[IncomingPollVote] = DeriveJsonCodec.gen[IncomingPollVote]
}
case class PollVotes(
    pollOptionId: UUID,
    userId: String
) extends AnyRef with PrimaryKey

case class OutgoingPollVote(
    pollOptionId: UUID,
    voteCast: Boolean,
    totalVotes: Int
) extends AnyRef with PrimaryKey

object OutgoingPollVote {
  implicit val codec: JsonCodec[OutgoingPollVote] = DeriveJsonCodec.gen[OutgoingPollVote]
}

case class JsonPoll(
    question: String,
    version: Int,
    options: Seq[JsonOption]
)

case class JsonOption(
    uid: String,
    text: String
)
