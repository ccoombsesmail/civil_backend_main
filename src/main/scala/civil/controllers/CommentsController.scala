package civil.controllers

import civil.apis.CommentsApi._
import civil.controllers.ParseUtils.{extractJwtData, parseBody, parseCommentId, parseDiscussionId, parseSkip}
import civil.errors.AppError.JsonDecodingError
import civil.models.IncomingComment
import civil.repositories.UsersRepositoryLive
import civil.repositories.comments.CommentsRepositoryLive
import civil.repositories.topics.DiscussionsRepositoryLive
import civil.services.AuthenticationServiceLive
import civil.services.comments.{CommentLikesService, CommentsService, CommentsServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Method, Request, Response}
import zio._
import zio.json.EncoderOps
import zhttp.http._


final case class CommentsController(commentsService: CommentsService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "comments"  =>
      for {
        incomingComment <- parseBody[IncomingComment](req)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        insertedComment <- commentsService.insertComment(authDataOpt.get._1, authDataOpt.get._2, incomingComment)
      } yield Response.json(insertedComment.toJson)

    case req @ Method.GET -> !! / "comments" / discussionId / skip =>
      for {
        id <- parseDiscussionId(discussionId)
        skip <- parseSkip(skip)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        comments <- commentsService.getComments(authDataOpt.get._1, authDataOpt.get._2, id.id, skip.value )
      } yield Response.json(comments.toJson)

    case req@Method.GET -> !! / "comments" / "replies" / commentId =>
      for {
        id <- parseCommentId(commentId)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        comments <- commentsService.getAllCommentReplies(authDataOpt.get._1, authDataOpt.get._2, id.id)
      } yield Response.json(comments.toJson)

    case req@Method.GET -> !! / "comments" / commentId =>
      for {
        id <- parseCommentId(commentId)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        comments <- commentsService.getComment(authDataOpt.get._1, authDataOpt.get._2, id.id)
      } yield Response.json(comments.toJson)

    case req@Method.GET -> !! / "comments" / "user" / userId =>
      for {
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        comments <- commentsService.getUserComments(authDataOpt.get._1, authDataOpt.get._2, userId)
      } yield Response.json(comments.toJson)
  }

}

object CommentsController {

  val layer: URLayer[CommentsService, CommentsController] = ZLayer.fromFunction(CommentsController.apply _)

}
