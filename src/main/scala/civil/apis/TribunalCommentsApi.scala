package civil.apis

import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.baseEndpointAuthenticated
import civil.models.{ErrorInfo, IncomingComment, TribunalCommentNode, TribunalCommentsReply}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{Endpoint, query}

object TribunalCommentsApi {

  val newTribunalCommentEndpoint: Endpoint[(String, String, IncomingComment), ErrorInfo, TribunalCommentsReply, Any] =
    baseEndpointAuthenticated.post
      .in("tribunal-comments")
      .in(jsonBody[IncomingComment])
      .out(jsonBody[TribunalCommentsReply])

  val getTribunalCommentsEndpoint: Endpoint[(String, String, String, String), ErrorInfo, List[TribunalCommentNode], Any] =
    baseEndpointAuthenticated.get
      .in("tribunal-comments")
      .in(query[String]("contentId"))
      .in(query[String]("commentType"))
      .out(jsonBody[List[TribunalCommentNode]])

  val getTribunalCommentsBatchEndpoint: Endpoint[(String, String, String), ErrorInfo, List[TribunalCommentNode], Any] =
    baseEndpointAuthenticated.get
      .in("tribunal-comments-batch")
      .in(query[String]("contentId"))
      .out(jsonBody[List[TribunalCommentNode]])

}
