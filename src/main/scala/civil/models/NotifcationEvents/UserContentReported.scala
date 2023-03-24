package civil.models.NotifcationEvents

import zio.ZIO
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}
import zio.kafka.serde.Serde

import java.util.UUID

case class UserContentReported(
    eventType: String,
    reportedContentId: UUID,
    contentType: String,
    reportedUserId: String,
)

object UserContentReported {
  implicit val decoder: JsonDecoder[UserContentReported] =
    DeriveJsonDecoder.gen[UserContentReported]
  implicit val encoder: JsonEncoder[UserContentReported] =
    DeriveJsonEncoder.gen[UserContentReported]

  val userContentReportedSerde: Serde[Any, UserContentReported] =
    Serde.string.inmapM { userContentReportedAsString =>
      ZIO.fromEither(
        userContentReportedAsString
          .fromJson[UserContentReported]
          .left
          .map(new RuntimeException(_))
      )
    } { userContentReportedAsObj =>
      ZIO.effect(userContentReportedAsObj.toJson)
    }
}
