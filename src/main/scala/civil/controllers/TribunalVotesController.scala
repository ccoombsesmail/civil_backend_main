package civil.controllers

import civil.controllers.ParseUtils.{extractJwtData, parseBody}
import civil.models.TribunalVote
import civil.services.TribunalVotesService
import zhttp.http.{Http, Request, Response}
import zhttp.http._
import zio._
import zio.json.EncoderOps

final case class TribunalVotesController(tribunalVotesService: TribunalVotesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "comments" / "civility"  =>
      for {
        vote <- parseBody[TribunalVote](req)
        authDataOpt <- extractJwtData(req)
        voteRes <- tribunalVotesService.addTribunalVote(authDataOpt.get._1, authDataOpt.get._2, vote)
      } yield Response.json(voteRes.toJson)

  }

}

object TribunalVotesController {

  val layer: URLayer[TribunalVotesService, TribunalVotesController] = ZLayer.fromFunction(TribunalVotesController.apply _)

}
