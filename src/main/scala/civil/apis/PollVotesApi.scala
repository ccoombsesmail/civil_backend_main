package civil.apis

import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import sttp.tapir.json.circe.jsonBody
import civil.apis.BaseApi.baseEndpointAuthenticated
import civil.models.{ErrorInfo, IncomingPollVote, OutgoingPollVote, PollVotes}
import sttp.model.QueryParams
import sttp.tapir.RenderPathTemplate.Defaults.query
import sttp.tapir.{Endpoint, path, queryParams}


object PollVotesApi {
  val createPollVoteEndpoint: Endpoint[(String, String, IncomingPollVote), ErrorInfo, OutgoingPollVote, Any] =
    baseEndpointAuthenticated.post
      .in("poll-votes")
      .in(jsonBody[IncomingPollVote])
      .out(jsonBody[OutgoingPollVote])

  val deletePollVoteEndpoint: Endpoint[(String, String, String), ErrorInfo, OutgoingPollVote, Any] =
    baseEndpointAuthenticated.delete
      .in("poll-votes")
      .in(path[String])
      .out(jsonBody[OutgoingPollVote])

  val getPollVoteDataEndpoint: Endpoint[(String, String, QueryParams), ErrorInfo, List[OutgoingPollVote], Any] =
    baseEndpointAuthenticated.get
      .in("poll-votes")
      .in(queryParams)
      .out(jsonBody[List[OutgoingPollVote]])
}
