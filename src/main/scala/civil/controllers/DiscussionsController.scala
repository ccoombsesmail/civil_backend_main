package civil.controllers

import civil.services.DiscussionService
import civil.errors.AppError.JsonDecodingError
import civil.models.IncomingDiscussion
import zhttp.http.{Http, Method, Request, Response}
import zio._
import zio.json.EncoderOps
import zhttp.http._
import civil.controllers.ParseUtils._


final case class DiscussionsController(discussionsService: DiscussionService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "discussions"  =>
      for {
        incomingDiscussion <- parseBody[IncomingDiscussion](req)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        insertedComment <- discussionsService.insertDiscussion(authDataOpt.get._1, authDataOpt.get._2, incomingDiscussion)
      } yield Response.json(incomingDiscussion.toJson)

    case req @ Method.GET -> !! / "discussions" / topicId / skip =>
      for {
        id <- parseTopicId(topicId)
        skip <- parseSkip(skip)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        discussions <- discussionsService.getDiscussions(authDataOpt.get._1, authDataOpt.get._2, id.id, skip.value )
      } yield Response.json(discussions.toJson)

    case req @ Method.GET -> !! / "discussions" / discussionId =>
      for {
        id <- parseDiscussionId(discussionId)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        discussion <- discussionsService.getDiscussion(id.id)
      } yield Response.json(discussion.toJson)

    case req @ Method.GET -> !! / "discussions" / "general" / topicId =>
      for {
        id <- parseTopicId(topicId)
        genDiscussionId <- discussionsService.getGeneralDiscussionId(id.id)
      } yield Response.json(genDiscussionId.toJson)

    case req@Method.GET -> !! / "discussions" / "user" / userId =>
      for {
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        discussions <- discussionsService.getUserDiscussions(authDataOpt.get._1, authDataOpt.get._2, userId)
      } yield Response.json(discussions.toJson)
  }

}

object DiscussionsController {

  val layer: URLayer[DiscussionService, DiscussionsController] = ZLayer.fromFunction(DiscussionsController.apply _)

}

