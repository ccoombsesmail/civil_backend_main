package civil.controllers

import civil.services.FollowsService
import civil.controllers.ParseUtils._
import civil.errors.AppError.JsonDecodingError
import civil.models.FollowedUserId
import zhttp.http.{Http, Request, Response}
import zio._
import zio.json.EncoderOps
import zhttp.http._

final case class FollowsController(followsService: FollowsService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "follows" =>
      for {
        followedUserId <- parseBody[FollowedUserId](req)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        followedUser <- followsService.insertFollow(authDataOpt.get._1, authDataOpt.get._2, followedUserId)
      } yield Response.json(followedUser.toJson)

    case req @ Method.DELETE -> !! / "follows" / followedUserId =>
      for {
        followedUserId <- parseFollowedUserId(followedUserId)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        unfollowedUser <- followsService.deleteFollow(authDataOpt.get._1, authDataOpt.get._2, followedUserId)
      } yield Response.json(unfollowedUser.toJson)

    case req @ Method.GET -> !! / "api" / "v1" / "follows" / "followers" / userId =>
      for {
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        followers <- followsService.getAllFolowers(userId)
      } yield Response.json(followers.toJson)

    case req @ Method.GET -> !! / "api" / "v1" / "follows" / "followed" / userId =>
      for {
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        followed <- followsService.getAllFollowed(userId)
      } yield Response.json(followed.toJson)
  }
}

object FollowsController {
  val layer: URLayer[FollowsService, FollowsController] = ZLayer.fromFunction(FollowsController.apply _)
}
