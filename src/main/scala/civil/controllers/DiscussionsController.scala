package civil.controllers

import civil.errors.AppError.JsonDecodingError
import civil.models.IncomingDiscussion
import zio.http._
import zio._
import zio.json.EncoderOps
import civil.controllers.ParseUtils._
import civil.services.discussions.DiscussionService
import zio.http.model.Method

import java.util.UUID

final case class DiscussionsController(discussionsService: DiscussionService) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "api" / "v1" / "discussions" =>
        (for {
          incomingDiscussion <- parseBody[IncomingDiscussion](req)
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          insertedDiscussion <- discussionsService.insertDiscussion(
            jwt,
            jwtType,
            incomingDiscussion
          )
        } yield Response.json(insertedDiscussion.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "discussions" / "space-discussions" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          spaceId <- parseQueryFirst(req, "spaceId")
          skip <- parseQueryFirst(req, "skip")
          discussions <- discussionsService.getSpaceDiscussions(
            jwt,
            jwtType,
            UUID.fromString(spaceId),
            skip.toInt
          )
        } yield Response.json(discussions.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "discussions" / "get-one" / discussionId =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          id <- parseDiscussionId(discussionId)
          discussion <- discussionsService.getDiscussion(jwt, jwtType, id.id)
        } yield Response.json(discussion.toJson)).catchAll(_.toResponse)

      case _ @Method.GET -> !! / "api" / "v1" / "discussions" / "general" / spaceId =>
        (for {
          id <- parseSpaceId(spaceId)
          genDiscussionId <- discussionsService.getGeneralDiscussionId(id.id)
        } yield Response.json(genDiscussionId.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "discussions" / "user" / userId =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          skip <- parseQueryFirst(req, "skip")
          discussions <- discussionsService.getUserDiscussions(
            jwt,
            jwtType,
            userId,
            skip.toInt
          )
        } yield Response.json(discussions.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "discussions-followed" =>
        (for {
          authData <- extractJwtData(req)
          skip <- parseQueryFirst(req, "skip")
          (jwt, jwtType) = authData
          discussions <- discussionsService.getFollowedDiscussions(
            jwt,
            jwtType,
            skip.toInt
          )
        } yield Response.json(discussions.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "discussions" / "similar-discussions" / discussionId =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          id <- parseDiscussionId(discussionId)
          discussions <- discussionsService.getSimilarDiscussions(
            jwt,
            jwtType,
            id.id
          )
        } yield Response.json(discussions.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "discussions" / "popular-discussions" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          skip <- parseQueryFirst(req, "skip")
          discussions <- discussionsService.getPopularDiscussions(
            jwt,
            jwtType,
            skip.toInt
          )
        } yield Response.json(discussions.toJson)).catchAll(_.toResponse)
    }

}

object DiscussionsController {

  val layer: URLayer[DiscussionService, DiscussionsController] =
    ZLayer.fromFunction(DiscussionsController.apply _)

}
