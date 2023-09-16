package civil.controllers

import java.util.UUID
import civil.models.enums.TribunalCommentType
import civil.services.TribunalCommentsService
import zio.http._
import zio._
import civil.controllers.ParseUtils._
import civil.errors.AppError.{InternalServerError, JsonDecodingError}
import civil.models.IncomingComment
import io.circe.syntax.EncoderOps
import zio.http.model.Method
import io.circe._
import io.circe.parser._

final case class TribunalCommentsController(
                                             tribunalCommentsService: TribunalCommentsService
                                           ) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req@Method.POST -> !! / "api" / "v1" / "tribunal-comments" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          tribunalComment <- parseBody[IncomingComment](req)
          res <- tribunalCommentsService.insertComment(
            jwt,
            jwtType,
            tribunalComment
          )
        } yield Response.json(res.asJson.noSpaces)).catchAll(_.toResponse)

      case req@Method.GET -> !! / "api" / "v1" / "tribunal-comments" =>
        (for {

          contentId <- parseQueryFirst(req, "contentId")
          commentType <- parseQueryFirst(req, "commentType")

          tComments <- req.bearerToken match {
            case Some(jwt) =>
              for {
                jwtTypeHeader <- ZIO
                  .fromOption(req.header("X-JWT-TYPE"))
                  .orElseFail(JsonDecodingError(new Throwable("error")))
                jwtType = jwtTypeHeader.value.toString
                // Call the function for authenticated users
                comments <- tribunalCommentsService
                  .getComments(
                    jwt,
                    jwtType,
                    UUID.fromString(contentId),
                    TribunalCommentType.withName(commentType)
                  )
              } yield comments
            case None =>
              // Call the function for non-authenticated users
              tribunalCommentsService
                .getCommentsUnauthenticated(
                  UUID.fromString(contentId),
                  TribunalCommentType.withName(commentType)
                )
          }
        } yield Response.json(tComments.asJson.noSpaces))
          .orElseFail(JsonDecodingError(new Throwable("error decoding")))
          .catchAll(_.toResponse)

      case req@Method.GET -> !! / "api" / "v1" / "tribunal-comments-batch" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          contentIdParam <- parseQuery(req, "contentId")
          contentId <- ZIO
            .fromOption(contentIdParam.headOption)
          res <- tribunalCommentsService.getCommentsBatch(
            jwt,
            jwtType,
            UUID.fromString(contentId)
          )
        } yield Response.json(res.asJson.noSpaces))
          .orElseFail(JsonDecodingError(new Throwable("error decoding")))
          .catchAll(_.toResponse)
    }
}

object TribunalCommentsController {
  val layer: URLayer[TribunalCommentsService, TribunalCommentsController] =
    ZLayer.fromFunction(TribunalCommentsController.apply _)
}
