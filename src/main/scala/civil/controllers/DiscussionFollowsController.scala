package civil.controllers

import civil.controllers.ParseUtils._
import civil.errors.AppError.JsonDecodingError
import civil.models.{DiscussionId, UpdateDiscussionFollows}
import civil.services.discussions.DiscussionFollowsService
import zio._
import zio.http._
import zio.http.model.Method
final case class DiscussionFollowsController(
    discussionFollowsService: DiscussionFollowsService
) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "api" / "v1" / "discussion-follows" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          spaceFollowUpdate <- parseBody[UpdateDiscussionFollows](req)
          _ <- discussionFollowsService.insertDiscussionFollow(
            jwt,
            jwtType,
            spaceFollowUpdate
          )
        } yield Response.ok).catchAll(_.toResponse)

      case req @ Method.DELETE -> !! / "api" / "v1" / "discussion-follows" =>
        (for {
          discussionIdParam <- parseQuery(req, "followedDiscussionId")
          spaceIdStr <- ZIO
            .fromOption(discussionIdParam.headOption)
            .orElseFail(JsonDecodingError(new Throwable("error decoding")))
          discussionId <- DiscussionId
            .fromString(spaceIdStr)
            .mapError(e => JsonDecodingError(e))
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          _ <- discussionFollowsService.deleteDiscussionFollow(
            jwt,
            jwtType,
            discussionId
          )
        } yield Response.ok).catchAll(_.toResponse)

    }
}

object DiscussionFollowsController {

  val layer: URLayer[DiscussionFollowsService, DiscussionFollowsController] =
    ZLayer.fromFunction(DiscussionFollowsController.apply _)

}
