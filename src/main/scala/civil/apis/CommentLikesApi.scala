package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.baseEndpointAuthenticated
import civil.models.{CommentLiked, AppError, UpdateCommentLikes}
import sttp.tapir.Endpoint

object CommentLikesApi {
  val updateCommentLikesEndpoint: Endpoint[(String, String, UpdateCommentLikes), AppError, CommentLiked, Any] =
    baseEndpointAuthenticated.put
      .in("comments")
      .in("likes")
      .in(jsonBody[UpdateCommentLikes])
      .out(jsonBody[CommentLiked])

  val updateTribunalCommentLikesEndpoint: Endpoint[(String, String, UpdateCommentLikes), AppError, CommentLiked, Any] =
    baseEndpointAuthenticated.put
      .in("comments")
      .in("likes-tribunal")
      .in(jsonBody[UpdateCommentLikes])
      .out(jsonBody[CommentLiked])
}
