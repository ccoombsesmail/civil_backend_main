package civil.apis

import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.{baseEndpoint, baseEndpointAuthenticated}
import civil.models.{AppError, TopicLiked, UpdateTopicLikes}
import sttp.tapir.Endpoint
import sttp.tapir.json.circe.jsonBody

object TopicLikesApi {
  val updateTopicLikesEndpoint: Endpoint[(String, String, UpdateTopicLikes), AppError, TopicLiked, Any] =
    baseEndpointAuthenticated.put
      .in("topic-likes")
      .in(jsonBody[UpdateTopicLikes])
      .out(jsonBody[TopicLiked])
}
