package civil.models

import java.util.UUID

case class OpposingRecommendations(
    targetContentId: UUID,
    recommendedContentId: Option[UUID],
    externalRecommendedContent: Option[String],
    isDiscussion: Boolean = false,
    similarityScore: Float
)

case class OutGoingOpposingRecommendations(
    id: UUID,
    recommendedContentId: Option[UUID],
    externalRecommendedContent: Option[String],
    discussion: Option[Discussions],
    topic: Option[Topics],
    similarityScore: Float
)

case class UrlsForTFIDFConversion(
    targetUrl: String,
    compareUrl: String
)

case class Score(
    score: Float
)
