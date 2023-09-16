package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class TribunalJuryMembers(
    userId: String,
    contentId: UUID,
    contentType: String,
    juryDutyCompletionTime: Option[Long]
)

object TribunalJuryMembers {
  implicit val codec: JsonCodec[TribunalJuryMembers] =
    DeriveJsonCodec.gen[TribunalJuryMembers]
}

case class JuryDuty(
    contentId: UUID,
    contentType: String,
    juryDutyCompletionTime: Option[Long],
    space: Option[OutgoingSpace],
    discussion: Option[OutgoingDiscussion],
    comment: Option[CommentWithUserData]
)

object JuryDuty {
  implicit val codec: JsonCodec[JuryDuty] =
    DeriveJsonCodec.gen[JuryDuty]
}
