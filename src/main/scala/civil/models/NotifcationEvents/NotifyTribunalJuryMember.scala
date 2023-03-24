package civil.models.NotifcationEvents

import zio.ZIO
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.serde.Serde
import java.util.UUID

case class NotifyTribunalJuryMember(
  eventType: String,
  reportedContentId: UUID,
  tribunalJuryMemberId: String,
  contentType: String,
  title: Option[String],
  contentCreatedBy: Option[String],
  topicThumbImg: Option[String],
  ytUrl: Option[String],
  externalRecommendedContentUrl: Option[String],
  reportedUserId: Option[String],
  reportedUserTag: Option[String]
)


object NotifyTribunalJuryMember {
  implicit val decoder: JsonDecoder[NotifyTribunalJuryMember] = DeriveJsonDecoder.gen[NotifyTribunalJuryMember]
  implicit val encoder: JsonEncoder[NotifyTribunalJuryMember] = DeriveJsonEncoder.gen[NotifyTribunalJuryMember]

  val notifyTribunalJuryMemberSerde: Serde[Any, NotifyTribunalJuryMember] = Serde.string.inmapM { notifyTribunalJuryMemberAsString =>
    ZIO.fromEither(notifyTribunalJuryMemberAsString.fromJson[NotifyTribunalJuryMember].left.map(new RuntimeException(_)))
  } { notifyTribunalJuryMemberAsObj =>
    ZIO.effect(notifyTribunalJuryMemberAsObj.toJson)
  }
}

