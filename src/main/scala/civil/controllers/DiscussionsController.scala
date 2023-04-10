package civil.controllers

import civil.services.DiscussionService
import civil.errors.AppError.JsonDecodingError
import civil.models.IncomingDiscussion
import zio.http._
import zio._
import zio.json.EncoderOps
import civil.controllers.ParseUtils._
import zio.http.model.Method

import java.util.UUID


final case class DiscussionsController(discussionsService: DiscussionService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !!  / "api" / "v1"/ "discussions"  =>
      (for {
        incomingDiscussion <- parseBody[IncomingDiscussion](req)
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        insertedDiscussion <- discussionsService.insertDiscussion(jwt, jwtType, incomingDiscussion)
      } yield Response.json(insertedDiscussion.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !!  / "api" / "v1" / "discussions" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        topicIdParams <- parseQuery(req, "topicId")
        topicId <- ZIO.fromOption(topicIdParams.headOption).mapError(e => JsonDecodingError(e.toString))
        skipParams <- parseQuery(req, "skip")
        skip <- ZIO.fromOption(skipParams.headOption).mapError(e => JsonDecodingError(e.toString))
        discussions <- discussionsService.getDiscussions(jwt, jwtType, UUID.fromString(topicId), skip.toInt)
      } yield Response.json(discussions.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "discussions" / discussionId =>
      (for {
        id <- parseDiscussionId(discussionId)
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        discussion <- discussionsService.getDiscussion(jwt, jwtType, id.id)
      } yield Response.json(discussion.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "discussions" / "general" / topicId =>
      (for {
        id <- parseTopicId(topicId)
        _ = println(id)
        genDiscussionId <- discussionsService.getGeneralDiscussionId(id.id)
      } yield Response.json(genDiscussionId.toJson)).catchAll(_.toResponse)

    case req@Method.GET -> !! / "api" / "v1" / "discussions" / "user" / userId =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        discussions <- discussionsService.getUserDiscussions(jwt, jwtType, userId)
      } yield Response.json(discussions.toJson)).catchAll(_.toResponse)
  }

}

object DiscussionsController {

  val layer: URLayer[DiscussionService, DiscussionsController] = ZLayer.fromFunction(DiscussionsController.apply _)

}

