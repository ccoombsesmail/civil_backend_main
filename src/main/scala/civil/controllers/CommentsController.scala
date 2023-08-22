package civil.controllers

import civil.controllers.ParseUtils.{
  extractJwtData,
  parseBody,
  parseCommentId,
  parseDiscussionId,
  parseQuery,
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
import zio.http.model.Method
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
          authData <- extractJwtData(req).mapError(e => JsonDecodingError(e))
          (jwt, jwtType) = authData

          discussionIdParams <- parseQuery(req, "discussionId")
          discussionId <- ZIO
            .fromOption(discussionIdParams.headOption)
            .orElseFail(JsonDecodingError(new Throwable("error decoding")))
          skipParams <- parseQuery(req, "skip")
          skip <- ZIO
            .fromOption(skipParams.headOption)
            .orElseFail(JsonDecodingError(new Throwable("error decoding")))
          comments <- commentsService.getComments(
            jwt,
            jwtType,
            UUID.fromString(discussionId),
            skip.toInt
          )
        } yield Response.json(comments.asJson.noSpaces)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "comments" / "replies" / commentId =>
        (for {
          id <- parseCommentId(commentId)
          authData <- extractJwtData(req).mapError(e => JsonDecodingError(e))
          (jwt, jwtType) = authData
          comments <- commentsService.getAllCommentReplies(jwt, jwtType, id.id)
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
          authData <- extractJwtData(req).mapError(e => JsonDecodingError(e))
          skipParams <- parseQuery(req, "skip")
          skip <- ZIO
            .fromOption(skipParams.headOption)
            .orElseFail(JsonDecodingError(new Throwable("error decoding")))

          (jwt, jwtType) = authData
          comments <- commentsService.getUserComments(
            jwt,
            jwtType,
            userId,
            skip.toInt
          )
        } yield Response.json(comments.asJson.noSpaces)).catchAll(_.toResponse)
    }

}

object CommentsController {

  val layer: URLayer[CommentsService, CommentsController] =
    ZLayer.fromFunction(CommentsController.apply _)

}
