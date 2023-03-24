package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.baseEndpoint
import civil.models.{ErrorInfo, OpposingRecommendations, OutGoingOpposingRecommendations}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{Endpoint, query}

object OpposingRecommendationsApi {
  val newOpposingRecommendationEndpoint: Endpoint[OpposingRecommendations, ErrorInfo, Unit, Any] =
    baseEndpoint.post
      .in("opposing-recommendations")
      .in(jsonBody[OpposingRecommendations])
      .out(jsonBody[Unit])


  val getOpposingRecommendationEndpoint: Endpoint[String, ErrorInfo, List[OutGoingOpposingRecommendations], Any] =
    baseEndpoint.get
      .in("opposing-recommendations")
      .in(query[String]("targetContentId"))
      .out(jsonBody[List[OutGoingOpposingRecommendations]])
}
