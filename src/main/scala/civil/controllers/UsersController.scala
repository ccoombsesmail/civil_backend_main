package civil.controllers

import civil.services.UsersService
import civil.controllers.ParseUtils.{
  extractJwtData,
  parseBody,
  parseQuery,
  parseQueryFirst
}
import civil.errors.AppError.JsonDecodingError
import civil.models.{
  IncomingUser,
  OutgoingUser,
  OutgoingUserUnauthenticated,
  TagData,
  UpdateUserBio,
  UpdateUserIcon
}
import zio._
import zio.http._
import zio.http.model.Method
import zio.json.EncoderOps

final case class UsersController(usersService: UsersService) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "api" / "v1" / "users" / "did-user" =>
        (for {
          didUser <- parseBody[IncomingUser](req)
          res <- usersService.upsertDidUser(didUser)
        } yield Response.json(res.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "users" =>
        (for {
          userId <- parseQueryFirst(req, "userId")
          user <- req.bearerToken match {
            case Some(jwt) =>
              for {
                jwtTypeHeader <- ZIO
                  .fromOption(req.header("X-JWT-TYPE"))
                  .orElseFail(JsonDecodingError(new Throwable("error")))
                jwtType = jwtTypeHeader.value.toString
                // Call the function for authenticated users
                user <- usersService.getUser(
                  jwt,
                  jwtType,
                  userId
                )
              } yield user.toJson
            case None =>
              // Call the function for non-authenticated users
              for {
                user <- usersService.getUserUnauthenticated(
                  userId
                )
              } yield user.toJson
          }
        } yield Response.json(user)).catchAll(_.toResponse)

      case req @ Method.PUT -> !! / "api" / "v1" / "users" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          userIconInfo <- parseBody[UpdateUserIcon](req)
          res <- usersService.updateUserIcon(
            userIconInfo.username,
            userIconInfo.iconSrc
          )
        } yield Response.json(res.toJson)).catchAll(_.toResponse)

      case req @ Method.PATCH -> !! / "api" / "v1" / "users" / "bio-experience" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          userBioInfo <- parseBody[UpdateUserBio](req)
          res <- usersService.updateUserBio(jwt, jwtType, userBioInfo)
        } yield Response.json(res.toJson)).catchAll(_.toResponse)

      case req @ Method.POST -> !! / "api" / "v1" / "users" / "upload" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          userIconInfo <- parseBody[UpdateUserIcon](req)
          res <- usersService.updateUserIcon(
            userIconInfo.username,
            userIconInfo.iconSrc
          )
        } yield Response.json(res.toJson)).catchAll(_.toResponse)

      case req @ Method.PUT -> !! / "api" / "v1" / "users" / "tag" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          tag <- parseBody[TagData](req)
          res <- usersService.createUserTag(jwt, jwtType, tag.tag)
        } yield Response.json(res.toJson)).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "users" / "tag-exists" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          tagParams <- parseQuery(req, "tag")
          tag <- ZIO
            .fromOption(tagParams.headOption)
            .orElseFail(
              JsonDecodingError(new Throwable("Can't get tag from query param"))
            )
          res <- usersService.checkIfTagExists(tag)
        } yield Response.json(res.toJson)).catchAll(_.toResponse)
    }
}

object UsersController {
  val layer: URLayer[UsersService, UsersController] =
    ZLayer.fromFunction(UsersController.apply _)
}
