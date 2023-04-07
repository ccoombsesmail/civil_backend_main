package civil.apis

import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import sttp.tapir.json.circe.jsonBody
import civil.apis.BaseApi.baseEndpointAuthenticated
import civil.models.{AppError, TribunalVote}
import sttp.tapir.Endpoint

object TribunalVotesApi {
  val newTribunalVoteEndpoint: Endpoint[(String, String, TribunalVote), AppError, TribunalVote, Any] =
    baseEndpointAuthenticated.post
      .in("tribunal-votes")
      .in(jsonBody[TribunalVote])
      .out(jsonBody[TribunalVote])
}
