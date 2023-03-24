package civil.apis

import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import civil.apis.BaseApi.baseEndpointAuthenticated
import civil.models.{Civility, CivilityGiven, ErrorInfo}

object CommentCivilityApi {
  val updateCommentCivilityEndpoint: Endpoint[(String, String, Civility), ErrorInfo, CivilityGiven, Any] =
    baseEndpointAuthenticated.put
      .in("comments" / "civility")
      .in(jsonBody[Civility])
      .out(jsonBody[CivilityGiven])

  val updateTribunalCommentCivilityEndpoint: Endpoint[(String, String, Civility), ErrorInfo, CivilityGiven, Any] =
    baseEndpointAuthenticated.put
      .in("comments" / "civility-tribunal")
      .in(jsonBody[Civility])
      .out(jsonBody[CivilityGiven])
}
