package civil.controllers

import civil.services.spaces.SpaceLikesService
import zio.http._
import zio._
import zio.json._
import ParseUtils._
import civil.models.UpdateSpaceLikes
import zio.http.model.{HTTP_CHARSET, Method}


final case class SpaceLikesController(spaceLikeService: SpaceLikesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.PUT -> !!  / "api" / "v1" / "space-likes"  => {
      println(req.body.asString(HTTP_CHARSET))
      (for {
        updateSpaceLikes <- parseBody[UpdateSpaceLikes](req)
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        spaceLiked <- spaceLikeService.addRemoveSpaceLikeOrDislike(jwt, jwtType, updateSpaceLikes)
      } yield Response.json(spaceLiked.toJson)).catchAll(_.toResponse)
    }
    }

}

object SpaceLikesController {

  val layer: URLayer[SpaceLikesService, SpaceLikesController] = ZLayer.fromFunction(SpaceLikesController.apply _)

}