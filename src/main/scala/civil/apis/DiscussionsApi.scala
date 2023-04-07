package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.{baseEndpoint, baseEndpointAuthenticated}
import civil.models.{Discussions, AppError, GeneralDiscussionId, IncomingDiscussion, OutgoingDiscussion, OutgoingTopic}
import sttp.tapir._

import java.util.UUID



object DiscussionsApi {
  val newDiscussionEndpoint: Endpoint[(String, String, IncomingDiscussion), AppError, Discussions, Any] =
    baseEndpointAuthenticated
      .post
      .in("discussions")
      .in(jsonBody[IncomingDiscussion])
      .out(jsonBody[Discussions])

  val getAllDiscussionsEndpoint: Endpoint[(String, Int), AppError, List[OutgoingDiscussion], Any] =
    baseEndpoint
      .get
      .in("discussions")
      .in(query[String]("topicId"))
      .in(query[Int]("skip"))
      .out(jsonBody[List[OutgoingDiscussion]])
  

  val getDiscussionEndpoint: Endpoint[String, AppError, OutgoingDiscussion, Any] =
    baseEndpoint
      .get
      .in("discussions")
      .in(path[String]("subTopicId"))
      .out(jsonBody[OutgoingDiscussion])

  val getGeneralDiscussionIdEndpoint: Endpoint[String, AppError, GeneralDiscussionId, Any] =
    baseEndpoint
      .get
      .in("discussions" / "general")
      .in(path[String]("topicId"))
      .out(jsonBody[GeneralDiscussionId])

  val getUserDiscussions: Endpoint[(String, String, String), AppError, List[OutgoingDiscussion], Any] =
    baseEndpointAuthenticated.get
      .in("discussions")
      .in("user")
      .in(path[String]("userId"))
      .out(jsonBody[List[OutgoingDiscussion]])

}
