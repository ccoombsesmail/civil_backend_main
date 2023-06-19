package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class Recommendations(
    targetContentId: UUID,
    recommendedContentId: UUID,
    similarityScore: Double
)

case class OutgoingRecommendations(
    discussion: Option[Discussions],
    space: Option[Spaces],
    similarityScore: Double
)

object OutgoingRecommendations {
  implicit val codec: JsonCodec[OutgoingRecommendations] = DeriveJsonCodec.gen[OutgoingRecommendations]
}

case class Recs(
    targetContentId: String,
    recommendedContentId: String,
    similarityScore: Double
)

case class IncomingRecommendations(
    recs: Array[Recs]
)
