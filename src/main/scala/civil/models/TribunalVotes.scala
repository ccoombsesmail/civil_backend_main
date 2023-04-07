package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class TribunalVotes(
    userId: String,
    contentId: UUID,
    voteAgainst: Option[Boolean],
    voteFor: Option[Boolean]
)

case class TribunalVote(
    contentId: UUID,
    voteAgainst: Option[Boolean],
    voteFor: Option[Boolean]
)

object TribunalVote {
  implicit val codec: JsonCodec[TribunalVote] = DeriveJsonCodec.gen[TribunalVote]
}
