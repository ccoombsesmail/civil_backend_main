package civil.controllers

import civil.controllers.ParseUtils.{extractJwtData, parseBody, parseSpaceId}
import civil.errors.AppError
import civil.models.IncomingSpace
import civil.services.spaces.SpacesService
import zio.http._
import zio._
import zio.http.model.Method
import zio.json.EncoderOps


final case class SpacesController(spacesService: SpacesService) {
  val routes: Http[Any, AppError, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "api" / "v1" / "spaces" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        incomingSpace <- parseBody[IncomingSpace](req)
        res <- spacesService.insertSpace(jwt, jwtType, incomingSpace)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "spaces" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        spaces <- spacesService.getSpacesAuthenticated(
          jwt, jwtType,
          req.url.queryParams("skip").head.toInt
        )
      } yield Response.json(spaces.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "spaces" / "user" / userId =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        res <- spacesService.getUserSpaces(jwt, jwtType, userId)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "spaces" / spaceId =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        spaceId <- parseSpaceId(spaceId)
        res <- spacesService.getSpace(jwt, jwtType, spaceId.id)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req@Method.GET -> !! / "api" / "v1" / "spaces-followed" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        spaces <- spacesService.getFollowedSpaces(
          jwt, jwtType,
        )
      } yield Response.json(spaces.toJson)).catchAll(_.toResponse)
  }
}

object SpacesController {
  val layer: URLayer[SpacesService, SpacesController] = ZLayer.fromFunction(SpacesController.apply _)
}