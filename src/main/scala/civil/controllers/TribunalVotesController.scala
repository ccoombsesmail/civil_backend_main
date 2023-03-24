package civil.controllers

import civil.apis.TribunalVotesApi._
import civil.services.{TribunalVotesService, TribunalVotesServiceLive}
import civil.repositories.TribunalVotesRepositoryLive
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{Has, ZIO}

object TribunalVotesController {
  val newTribunalVoteEndpointRoute: Http[Has[TribunalVotesService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(newTribunalVoteEndpoint){ case (jwt, jwtType, tribunalVote) =>
      TribunalVotesService.addTribunalVote(jwt, jwtType, tribunalVote)
        .map(res => {
          Right(res)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(TribunalVotesRepositoryLive.live >>> TribunalVotesServiceLive.live)
    }
  }

}
