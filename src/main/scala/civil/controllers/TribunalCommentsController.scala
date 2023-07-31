package civil.controllers

import java.util.UUID
import civil.models.enums.TribunalCommentType
import civil.services.TribunalCommentsService
import zio.http._
import zio._
import civil.controllers.ParseUtils._
import civil.errors.AppError.JsonDecodingError
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
      case req @ Method.POST -> !! / "api" / "v1" / "tribunal-comments" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          tribunalComment <- parseBody[IncomingComment](req).tapError(e =>
            ZIO.logInfo(e.toString)
          )
          res <- tribunalCommentsService.insertComment(
            jwt,
            jwtType,
            tribunalComment
          )
        } yield Response.json(res.asJson.noSpaces)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "tribunal-comments" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          contentIdParam <- parseQuery(req, "contentId")
          commentTypeParam <- parseQuery(req, "commentType")
          contentId <- ZIO
            .fromOption(contentIdParam.headOption)
            .mapError(e => JsonDecodingError(e.toString))
          commentType <- ZIO
            .fromOption(commentTypeParam.headOption)
            .mapError(e => JsonDecodingError(e.toString))
          res <- tribunalCommentsService
            .getComments(
              jwt,
              jwtType,
              UUID.fromString(contentId),
              TribunalCommentType.withName(commentType)
            )
            .mapError(e => {
              e
            })
        } yield Response.json(res.asJson.noSpaces)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "tribunal-comments-batch" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          contentIdParam <- parseQuery(req, "contentId")
          contentId <- ZIO
            .fromOption(contentIdParam.headOption)
            .mapError(e => JsonDecodingError(e.toString))
          res <- tribunalCommentsService.getCommentsBatch(
            jwt,
            jwtType,
            UUID.fromString(contentId)
          )
        } yield Response.json(res.asJson.noSpaces)).catchAll(_.toResponse)
    }
}

object TribunalCommentsController {
  val layer: URLayer[TribunalCommentsService, TribunalCommentsController] =
    ZLayer.fromFunction(TribunalCommentsController.apply _)
}
