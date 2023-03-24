package civil.controllers
import civil.apis.PollVotesApi.{createPollVoteEndpoint, deletePollVoteEndpoint, getPollVoteDataEndpoint}
import civil.apis.TribunalVotesApi._
import civil.repositories.{PollVotesRepositoryLive, TribunalVotesRepositoryLive}
import civil.services.{PollVotesService, PollVotesServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{Has, Task, ZIO}

import java.util.UUID


object PollVotesController {
  val pollVotesLayer = PollVotesRepositoryLive.live >>> PollVotesServiceLive.live
  val createPollVoteEndpointRoute: Http[Has[PollVotesService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(createPollVoteEndpoint) { case (jwt, jwtType, pollVote) =>
      PollVotesService.createPollVote(jwt, jwtType, pollVote)
        .map(res => {
          Right(res)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(pollVotesLayer)
    }
  }

  val deletePollVoteEndpointRoute: Http[Has[PollVotesService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(deletePollVoteEndpoint) { case (jwt, jwtType, pollOptionId) =>
      PollVotesService.deletePollVote(jwt, jwtType, UUID.fromString(pollOptionId))
        .map(res => {
          Right(res)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(pollVotesLayer)
    }
  }

  val getPollVoteDataEndpointRoute: Http[Has[PollVotesService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getPollVoteDataEndpoint) { case (jwt, jwtType, pollOptionsIds) => {
      val ids = pollOptionsIds.ps.head._2.head.split(",").toList.map(UUID.fromString)
      PollVotesService.getPollVoteData(jwt, jwtType, ids)
        .map(res => {
          Right(res)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(pollVotesLayer)
    }
    }
  }

  val routes = List(createPollVoteEndpointRoute)

}
