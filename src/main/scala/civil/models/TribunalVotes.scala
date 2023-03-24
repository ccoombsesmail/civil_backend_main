package civil.models

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
