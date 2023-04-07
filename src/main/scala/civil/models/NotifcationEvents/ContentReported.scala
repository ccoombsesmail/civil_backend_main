package civil.models.NotifcationEvents

import zio.ZIO
import zio.json.{
  DecoderOps,
  DeriveJsonDecoder,
  DeriveJsonEncoder,
  EncoderOps,
  JsonDecoder,
  JsonEncoder
}
import zio.kafka.serde.Serde

import java.util.UUID

case class ContentReported(
    eventType: String,
    reportedContentId: UUID,
    contentType: String
)

object ContentReported {
  implicit val decoder: JsonDecoder[ContentReported] =
    DeriveJsonDecoder.gen[ContentReported]
  implicit val encoder: JsonEncoder[ContentReported] =
    DeriveJsonEncoder.gen[ContentReported]

  val contentReportedSerde: Serde[Any, ContentReported] =
    Serde.string.inmapM { contentReportedAsString =>
      ZIO.fromEither(
        contentReportedAsString
          .fromJson[ContentReported]
          .left
          .map(new RuntimeException(_))
      )
    } { contentReportedAsObj =>
      ZIO.attempt(contentReportedAsObj.toJson)
    }
}
