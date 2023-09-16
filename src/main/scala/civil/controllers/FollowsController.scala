package civil.controllers

import civil.services.FollowsService
import civil.controllers.ParseUtils._
import civil.models.FollowedUserId
import zio.http._
import zio._
import zio.http.model.Method
import zio.json.EncoderOps

final case class FollowsController(followsService: FollowsService) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "api" / "v1" / "follows" =>
        (for {
          followedUserId <- parseBody[FollowedUserId](req)
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          followedUser <- followsService.insertFollow(
            jwt,
            jwtType,
            followedUserId
          )
        } yield Response.json(followedUser.toJson)).catchAll(_.toResponse)

      case req @ Method.DELETE -> !! / "api" / "v1" / "follows" =>
        (for {
          followedUserId <- parseQuery(req, "followedUserId")
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          unfollowedUser <- followsService.deleteFollow(
            jwt,
            jwtType,
            FollowedUserId(followedUserId.head)
          )
        } yield Response.json(unfollowedUser.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "follows" / "followers" / userId =>
        for {
          followers <- followsService.getAllFolowers(userId)
        } yield Response.json(followers.toJson)

      case req @ Method.GET -> !! / "api" / "v1" / "follows" / "followed" / userId =>
        for {
          followed <- followsService.getAllFollowed(userId)
        } yield Response.json(followed.toJson)
    }
}

object FollowsController {
  val layer: URLayer[FollowsService, FollowsController] =
    ZLayer.fromFunction(FollowsController.apply _)
}
