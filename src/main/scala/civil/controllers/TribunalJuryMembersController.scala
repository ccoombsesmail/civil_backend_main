package civil.controllers

import zio.http._
import zio._
import zio.json.EncoderOps
import civil.controllers.ParseUtils._
import civil.services.TribunalJuryMembersService
import zio.http.model.Method

final case class TribunalJuryMembersController(
    tribunalJuryMembersService: TribunalJuryMembersService
) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.GET -> !! / "api" / "v1" / "jury" / "user-duties" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          juryDuties <- tribunalJuryMembersService.getUserJuryDuties(
            jwt,
            jwtType
          )
        } yield Response.json(juryDuties.toJson)).catchAll(_.toResponse)
    }
}

object TribunalJuryMembersController {
  val layer
      : URLayer[TribunalJuryMembersService, TribunalJuryMembersController] =
    ZLayer.fromFunction(TribunalJuryMembersController.apply _)
}
