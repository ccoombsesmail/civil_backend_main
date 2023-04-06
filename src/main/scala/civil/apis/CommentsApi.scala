package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import io.circe.syntax._
import civil.apis.BaseApi.{baseEndpoint, baseEndpointAuthenticated}
import civil.models.{CommentNode, CommentReply, CommentWithReplies, ErrorInfo, IncomingComment}
import sttp.tapir._

object CommentsApi {
  lazy val newCommentEndpoint: Endpoint[(String, String, IncomingComment), ErrorInfo, CommentReply, Any] =
    baseEndpointAuthenticated.post
      .in("comments")
      .in(jsonBody[IncomingComment])
      .out(jsonBody[CommentReply])

  lazy val getAllCommentsEndpoint: Endpoint[(String, String, String, Int), ErrorInfo, List[CommentNode], Any] =
    baseEndpointAuthenticated.get
      .in("comments")
      .in(query[String]("discussionId"))
      .in(query[Int]("skip"))
      .out(jsonBody[List[CommentNode]])

  lazy val getAllCommentRepliesEndpoint: Endpoint[(String, String, String), ErrorInfo, CommentWithReplies, Any] =
    baseEndpointAuthenticated.get
      .in("comments")
      .in("replies")
      .in(path[String]("commentId"))
      .out(jsonBody[CommentWithReplies])

  val getCommentEndpoint: Endpoint[(String, String, String), ErrorInfo, CommentReply, Any] =
    baseEndpointAuthenticated.get
      .in("comments")
      .in(path[String]("commentId"))
      .out(jsonBody[CommentReply])

  val getUserComments: Endpoint[(String, String, String), ErrorInfo, List[CommentNode], Any] =
    baseEndpointAuthenticated.get
      .in("comments")
      .in("user")
      .in(path[String]("userId"))
      .out(jsonBody[List[CommentNode]])


}
