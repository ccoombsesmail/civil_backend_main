package civil.controllers

import civil.controllers.ParseUtils.{extractJwtData, parseBody}
import civil.models.TribunalVote
import civil.services.TribunalVotesService
import zio.http._
import zio._
import zio.http.model.Method
import zio.json.EncoderOps

final case class TribunalVotesController(tribunalVotesService: TribunalVotesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "api" / "v1" / "comments" / "civility"  =>
      (for {
        vote <- parseBody[TribunalVote](req)
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        voteRes <- tribunalVotesService.addTribunalVote(jwt, jwtType, vote)
      } yield Response.json(voteRes.toJson)).catchAll(_.toResponse)

  }

}

object TribunalVotesController {

  val layer: URLayer[TribunalVotesService, TribunalVotesController] = ZLayer.fromFunction(TribunalVotesController.apply _)

}
