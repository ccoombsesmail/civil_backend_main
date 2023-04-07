package civil.controllers

import civil.services.UsersService
import civil.controllers.ParseUtils.{extractJwtData, parseBody, parseQuery}
import civil.models.{IncomingUser, TagData, UpdateUserBio, UpdateUserIcon}
import zhttp.http.{Http, Method, Request, Response}
import zio._
import zhttp.http._
import zio.json.EncoderOps




final case class UsersController(usersService: UsersService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "users" / "did-user" =>
      for {
        didUser <- parseBody[IncomingUser](req)
        res <- usersService.upsertDidUser(didUser)
      } yield Response.json(res.toJson)

    case req @ Method.GET -> !! / "users" =>
      for {
        authDataOpt <- extractJwtData(req)
        userId <- parseQuery[String](req, "userId")
        user <- usersService.getUser(
          authDataOpt.get._1,
          authDataOpt.get._2,
          userId
        )
      } yield Response.json(user.toJson)

    case req @ Method.PUT -> !! / "users" =>
      for {
        authDataOpt <- extractJwtData(req)
        userIconInfo <- parseBody[UpdateUserIcon](req)
        res <- usersService.updateUserIcon(userIconInfo.username, userIconInfo.iconSrc)
      } yield Response.json(res.toJson)

    case req @ Method.PATCH -> !! / "users" / "bio-experience" =>
      for {
        authDataOpt <- extractJwtData(req)
        userBioInfo <- parseBody[UpdateUserBio](req)
        res <- usersService.updateUserBio(authDataOpt.get._1, authDataOpt.get._2, userBioInfo)
      } yield Response.json(res.toJson)

    case req@Method.POST -> !! / "users" / "upload" =>
      for {
        authDataOpt <- extractJwtData(req)
        userIconInfo <- parseBody[UpdateUserIcon](req)
        res <- usersService.updateUserIcon(userIconInfo.username, userIconInfo.iconSrc)
      } yield Response.json(res.toJson)

    case req@Method.PATCH -> !! / "users" / "tag" =>
      for {
        authDataOpt <- extractJwtData(req)
        tag <- parseBody[TagData](req)
        res <- usersService.createUserTag(authDataOpt.get._1, authDataOpt.get._2, tag.tag)
      } yield Response.json(res.toJson)

    case req@Method.PATCH -> !! / "users" / "tag-exists"=>
      for {
        authDataOpt <- extractJwtData(req)
        tag <- parseQuery[String](req, "tag")
        res <- usersService.checkIfTagExists(tag)
      } yield Response.json(res.toJson)
  }
}

object UsersController {
  val layer: URLayer[UsersService, UsersController] = ZLayer.fromFunction(UsersController.apply _)
}


