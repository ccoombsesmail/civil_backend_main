package civil.controllers

import java.util.UUID
import civil.models.enums.TribunalCommentType
import civil.services.TribunalCommentsService
import zhttp.http.{Http, Request, Response}
import zhttp.http._
import zio._
import zio.json.EncoderOps
import civil.controllers.ParseUtils._
import civil.models.IncomingComment


final case class TribunalCommentsController(tribunalCommentsService: TribunalCommentsService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "tribunal-comments" =>
      for {
        authDataOpt <- extractJwtData(req)
        tribunalComment <- parseBody[IncomingComment](req)
        res <- tribunalCommentsService.insertComment(authDataOpt.get._1, authDataOpt.get._2, tribunalComment)
      } yield Response.json(res.toJson)

    case req @ Method.GET -> !! / "tribunal-comments" =>
      for {
        authDataOpt <- extractJwtData(req)
        contentId <- parseQuery[String](req, "contentId")
        commentType <- parseQuery[String](req, "commentType")
        res <- tribunalCommentsService.getComments(authDataOpt.get._1, authDataOpt.get._2, UUID.fromString(contentId), TribunalCommentType.withName(commentType))
      } yield Response.json(res.toJson)

    case req @ Method.GET -> !! / "tribunal-comments-batch" =>
      for {
        authDataOpt <- extractJwtData(req)
        contentId <- parseQuery[String](req, "contentId")
        res <- tribunalCommentsService.getCommentsBatch(authDataOpt.get._1, authDataOpt.get._2, UUID.fromString(contentId))
      } yield Response.json(res.toJson)
  }
}

object TribunalCommentsController {
  val layer: URLayer[TribunalCommentsService, TribunalCommentsController] = ZLayer.fromFunction(TribunalCommentsController.apply _)
}