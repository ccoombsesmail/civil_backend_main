package civil.controllers

import civil.services.discussions.DiscussionLikesService
import zio.http._
import zio._
import zio.json._
import ParseUtils._
import civil.models.UpdateDiscussionLikes



final case class DiscussionLikesController(discussionLikeService: DiscussionLikesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.PUT -> !!  / "api" / "v1" / "discussion-likes"  =>
      (for {
        updateDiscussionLikes <- parseBody[UpdateDiscussionLikes](req)
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        discussionLiked <- discussionLikeService.addRemoveDiscussionLikeOrDislike(jwt, jwtType, updateDiscussionLikes)
      } yield Response.json(discussionLiked.toJson)).catchAll(_.toResponse)
  }

}

object DiscussionLikesController {

  val layer: URLayer[DiscussionLikesService, DiscussionLikesController] = ZLayer.fromFunction(DiscussionLikesController.apply _)

}