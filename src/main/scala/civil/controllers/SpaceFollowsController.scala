package civil.controllers

import civil.controllers.ParseUtils._
import civil.errors.AppError.JsonDecodingError
import civil.models.{SpaceId, UpdateSpaceFollows}
import civil.services.spaces.SpaceFollowsService
import zio._
import zio.http._
import zio.http.model.Method
final case class SpaceFollowsController(
    spaceFollowsService: SpaceFollowsService
) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "api" / "v1" / "space-follows" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          spaceFollowUpdate <- parseBody[UpdateSpaceFollows](req)
          _ <- spaceFollowsService.insertSpaceFollow(
            jwt,
            jwtType,
            spaceFollowUpdate
          )
        } yield Response.ok).catchAll(_.toResponse)

      case req @ Method.DELETE -> !! / "api" / "v1" / "space-follows" =>
        (for {
          spaceIdParam <- parseQuery(req, "followedSpaceId")
          spaceIdStr <- ZIO
            .fromOption(spaceIdParam.headOption)
            .orElseFail(JsonDecodingError(new Throwable("error decoding")))
          spaceId <- SpaceId
            .fromString(spaceIdStr)
            .mapError(e => JsonDecodingError(e))
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          _ <- spaceFollowsService.deleteSpaceFollow(jwt, jwtType, spaceId)
        } yield Response.ok).catchAll(_.toResponse)

    }
}

object SpaceFollowsController {

  val layer: URLayer[SpaceFollowsService, SpaceFollowsController] =
    ZLayer.fromFunction(SpaceFollowsController.apply _)

}
