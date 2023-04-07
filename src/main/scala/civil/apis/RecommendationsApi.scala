package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.baseEndpoint
import civil.models.{AppError, OutgoingRecommendations}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{Endpoint, query}

object RecommendationsApi {

  val getAllRecommendationsEndpoint: Endpoint[String, AppError, List[OutgoingRecommendations], Any] =
    baseEndpoint.get
      .in("recommendations")
      .in(query[String]("targetContentId"))
      .out(jsonBody[List[OutgoingRecommendations]])
}
