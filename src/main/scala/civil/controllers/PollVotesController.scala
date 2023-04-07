package civil.controllers
import civil.models.IncomingPollVote
import civil.services.PollVotesService
import zhttp.http.{Http, Request, Response}
import zio.{URLayer, ZLayer}
import zhttp.http._
import zio.json.EncoderOps

import java.util.UUID
import civil.controllers.ParseUtils._





final case class PollVotesController(pollVotesService: PollVotesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "poll-votes" =>
      for {
        authDataOpt <- extractJwtData(req)
        pollVote <- parseBody[IncomingPollVote](req)
        res <- pollVotesService.createPollVote(authDataOpt.get._1, authDataOpt.get._2, pollVote)
      } yield Response.json(res.toJson)

    case req @ Method.DELETE -> !! / "poll-votes" / pollOptionId =>
      for {
        authDataOpt <- extractJwtData(req)
        res <- pollVotesService.deletePollVote(authDataOpt.get._1, authDataOpt.get._2, UUID.fromString(pollOptionId))
      } yield Response.json(res.toJson)

    case req @ Method.GET -> !! / "poll-votes" =>
      for {
        authDataOpt <- extractJwtData(req)
        _ = println(req.url.queryParams)
//        pollOptionsIds <- parsePollOptionsIds(req)
        res <- pollVotesService.getPollVoteData(authDataOpt.get._1, authDataOpt.get._2, List())
      } yield Response.json(res.toJson)
  }
}

object PollVotesController {
  val layer: URLayer[PollVotesService, PollVotesController] = ZLayer.fromFunction(PollVotesController.apply _)

}
