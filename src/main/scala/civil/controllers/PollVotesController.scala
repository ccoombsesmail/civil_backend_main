package civil.controllers
import civil.models.IncomingPollVote
import civil.services.PollVotesService
import zio.http._
import zio.{URLayer, ZLayer}
import zio.json.EncoderOps

import java.util.UUID
import civil.controllers.ParseUtils._






final case class PollVotesController(pollVotesService: PollVotesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "api" / "v1" / "poll-votes" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        pollVote <- parseBody[IncomingPollVote](req)
        res <- pollVotesService.createPollVote(jwt, jwtType, pollVote)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req @ Method.DELETE -> !! / "api" / "v1" / "poll-votes" / pollOptionId =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        res <- pollVotesService.deletePollVote(jwt, jwtType, UUID.fromString(pollOptionId))
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "poll-votes" =>
     ( for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
//        pollOptionsIds <- parsePollOptionsIds(req) TODO
        res <- pollVotesService.getPollVoteData(jwt, jwtType, List())
      } yield Response.json(res.toJson)).catchAll(_.toResponse)
  }
}

object PollVotesController {
  val layer: URLayer[PollVotesService, PollVotesController] = ZLayer.fromFunction(PollVotesController.apply _)

}
