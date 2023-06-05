package civil.models

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID

case class OpposingRecommendations(
    targetContentId: UUID,
    recommendedContentId: Option[UUID],
    externalRecommendedContent: Option[String],
    isDiscussion: Boolean = false,
    similarityScore: Float
)

object OpposingRecommendations {
  implicit val codec: JsonCodec[OpposingRecommendations] = DeriveJsonCodec.gen[OpposingRecommendations]
}

case class OutGoingOpposingRecommendations(
    id: UUID,
    recommendedContentId: Option[UUID],
    externalRecommendedContent: Option[String],
    discussion: Option[Discussions],
    topic: Option[Spaces],
    similarityScore: Float
)

object OutGoingOpposingRecommendations {
  implicit val codec: JsonCodec[OutGoingOpposingRecommendations] = DeriveJsonCodec.gen[OutGoingOpposingRecommendations]
}

case class UrlsForTFIDFConversion(
    targetUrl: String,
    compareUrl: String
)

case class Score(
    score: Float
)
