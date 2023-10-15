package civil.controllers

import civil.controllers.ParseUtils.{
  extractJwtData,
  parseBody,
  parseCommentId,
  parseDiscussionId,
  parseQuery,
  parseQueryFirst,
  parseSkip
}
import civil.errors.AppError.{InternalServerError, JsonDecodingError}
import civil.models.{CommentNode, IncomingComment}
import civil.services.comments.CommentsService
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps
import zio.Console.printLine
import zio.http._
import zio._

import zio.json.{DeriveJsonCodec, JsonCodec}
import io.circe.generic.semiauto._
import io.circe.generic.auto._

import java.util.UUID

final case class CommentsController(commentsService: CommentsService) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "api" / "v1" / "comments" =>
        (for {
          incomingComment <- parseBody[IncomingComment](req)
          authData <- extractJwtData(req).mapError(e => JsonDecodingError(e))
          (jwt, jwtType) = authData
          insertedComment <- commentsService.insertComment(
            jwt,
            jwtType,
            incomingComment
          )
        } yield Response.json(insertedComment.asJson.noSpaces))
          .catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "comments" =>
        (for {
          discussionId <- parseQueryFirst(req, "discussionId")
          skip <- parseQueryFirst(req, "skip")
          comments <- req.headers.get("authorization") match {
            case Some(jwt) =>
              for {
                jwtTypeHeader <- ZIO
                  .fromOption(req.headers.get("X-JWT-TYPE"))
                  .orElseFail(JsonDecodingError(new Throwable("error")))
                jwtType = jwtTypeHeader
                // Call the function for authenticated users
                comments <- commentsService.getComments(
                  jwt,
                  jwtType,
                  UUID.fromString(discussionId),
                  skip.toInt
                )
              } yield comments
            case None =>
              // Call the function for non-authenticated users
              commentsService.getCommentsUnauthenticated(
                UUID.fromString(discussionId),
                skip.toInt
              )
          }
        } yield Response.json(comments.asJson.noSpaces)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "comments" / "replies" / commentId =>
        (for {
          commentId <- parseCommentId(commentId)
          comments <- req.headers.get("authorization") match {
            case Some(jwt) =>
              for {
                jwtTypeHeader <- ZIO
                  .fromOption(req.headers.get("X-JWT-TYPE"))
                  .orElseFail(JsonDecodingError(new Throwable("error")))
                jwtType = jwtTypeHeader
                // Call the function for authenticated users
                comments <- commentsService.getAllCommentReplies(
                  jwt,
                  jwtType,
                  commentId.id
                )
              } yield comments
            case None =>
              // Call the function for non-authenticated users
              commentsService.getAllCommentRepliesUnauthenticated(commentId.id)
          }
        } yield Response.json(comments.asJson.noSpaces)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "comments" / commentId =>
        (for {
          id <- parseCommentId(commentId)
          authData <- extractJwtData(req).mapError(e => JsonDecodingError(e))
          (jwt, jwtType) = authData
          comments <- commentsService.getComment(jwt, jwtType, id.id)
        } yield Response.json(comments.asJson.noSpaces)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "comments" / "user" / userId =>
        (for {
          skip <- parseQueryFirst(req, "skip")
          comments <- req.headers.get("authorization") match {
            case Some(jwt) =>
              for {
                jwtTypeHeader <- ZIO
                  .fromOption(req.headers.get("X-JWT-TYPE"))
                  .orElseFail(JsonDecodingError(new Throwable("error")))
                jwtType = jwtTypeHeader
                // Call the function for authenticated users
                comments <- commentsService.getUserComments(
                  jwt,
                  jwtType,
                  userId,
                  skip.toInt
                )
              } yield comments
            case None =>
              // Call the function for non-authenticated users
              commentsService.getUserCommentsUnauthenticated(
                userId,
                skip.toInt
              )
          }
        } yield Response.json(comments.asJson.noSpaces)).catchAll(_.toResponse)
    }

}

object CommentsController {

  val layer: URLayer[CommentsService, CommentsController] =
    ZLayer.fromFunction(CommentsController.apply _)

}
