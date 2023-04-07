package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.{baseEndpoint, baseEndpointAuthenticated}
import civil.models.{AppError, IncomingTopic, OutgoingTopic, OutgoingTopicsPayload}
import sttp.tapir._
import zio._


object TopicsApi {
  
  val newTopicEndpoint: Endpoint[(String, String, IncomingTopic), AppError, OutgoingTopic, Any] =
    baseEndpointAuthenticated.post
      .in("topics")
      .in(jsonBody[IncomingTopic])
      .out(jsonBody[OutgoingTopic])

  val getAllTopicsEndpointAuthenticated: Endpoint[(String, String, Int), AppError, List[OutgoingTopic], Any] =
    baseEndpointAuthenticated.get
      .in("topics")
      .in(query[Int]("skip"))
      .out(jsonBody[List[OutgoingTopic]])

  val getAllTopicsEndpoint: Endpoint[Unit, AppError, List[OutgoingTopic], Any] =
    baseEndpoint.get
      .in("topics")
      .out(jsonBody[List[OutgoingTopic]])


  val getTopicEndpoint: Endpoint[(String, String, String), AppError, OutgoingTopic, Any] =
    baseEndpointAuthenticated.get
      .in("topics")
      .in(path[String]("topicId"))
      .out(jsonBody[OutgoingTopic])

  val getUserTopics: Endpoint[(String, String, String), AppError, List[OutgoingTopic], Any] =
    baseEndpointAuthenticated.get
      .in("topics")
      .in("user")
      .in(path[String]("userId"))
      .out(jsonBody[List[OutgoingTopic]])


}
