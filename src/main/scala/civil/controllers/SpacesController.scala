package civil.controllers

import civil.controllers.ParseUtils.{
  extractJwtData,
  parseBody,
  parseQueryFirst,
  parseSpaceId
}
import civil.errors.AppError
import civil.errors.AppError._
import civil.models.IncomingSpace
import civil.services.spaces.SpacesService
import zio.http._
import zio._

import zio.json.EncoderOps

final case class SpacesController(spacesService: SpacesService) {
  val routes: Http[Any, AppError, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "api" / "v1" / "spaces" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          incomingSpace <- parseBody[IncomingSpace](req)
          res <- spacesService.insertSpace(jwt, jwtType, incomingSpace)
        } yield Response.json(res.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "spaces" =>
        (for {
          skip <- parseQueryFirst(req, "skip")
          spaces <- req.headers.get("authorization") match {
            case Some(jwt) =>
              for {
                jwtTypeHeader <- ZIO
                  .fromOption(req.headers.get("X-JWT-TYPE"))
                  .orElseFail(
                    Unauthorized(
                      new Throwable("X-JWT-TYPE header not provided")
                    )
                  )
                jwtType = jwtTypeHeader
                spaces <- spacesService.getSpacesAuthenticated(
                  jwt,
                  jwtType,
                  skip.toInt
                )
              } yield spaces
            case None =>
              spacesService.getSpacesUnauthenticated(
                skip.toInt
              )
          }
        } yield Response.json(spaces.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "spaces" / "user" / userId =>
        (for {
          skip <- parseQueryFirst(req, "skip")
          spaces <- req.headers.get("authorization") match {
            case Some(jwt) =>
              for {
                jwtTypeHeader <- ZIO
                  .fromOption(req.headers.get("X-JWT-TYPE"))
                  .orElseFail(
                    Unauthorized(
                      new Throwable("X-JWT-TYPE header not provided")
                    )
                  )
                jwtType = jwtTypeHeader
                spaces <- spacesService.getUserSpaces(
                  jwt,
                  jwtType,
                  userId,
                  skip.toInt
                )
              } yield spaces
            case None =>
              spacesService.getUserSpacesUnauthenticated(userId, skip.toInt)
          }
        } yield Response.json(spaces.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "spaces" / spaceId =>
        (for {
          spaceId <- parseSpaceId(spaceId)
          space <- req.headers.get("authorization") match {
            case Some(jwt) =>
              for {
                jwtTypeHeader <- ZIO
                  .fromOption(req.headers.get("X-JWT-TYPE"))
                  .orElseFail(
                    Unauthorized(
                      new Throwable("X-JWT-TYPE header not provided")
                    )
                  )
                jwtType = jwtTypeHeader
                space <- spacesService.getSpace(
                  jwt,
                  jwtType,
                  spaceId.id
                )
              } yield space
            case None =>
              spacesService.getSpaceUnauthenticated(
                spaceId.id
              )
          }
        } yield Response.json(space.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "spaces-followed" =>
        (for {
          skip <- parseQueryFirst(req, "skip")
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          spaces <- spacesService.getFollowedSpaces(
            jwt,
            jwtType,
            skip.toInt
          )
        } yield Response.json(spaces.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "spaces" / "similar-spaces" / spaceId =>
        (for {
          spaceId <- parseSpaceId(spaceId)
          spaces <- spacesService.getSimilarSpaces(
            spaceId.id
          )
        } yield Response.json(spaces.toJson)).catchAll(_.toResponse)
    }
}

object SpacesController {
  val layer: URLayer[SpacesService, SpacesController] =
    ZLayer.fromFunction(SpacesController.apply _)
}
