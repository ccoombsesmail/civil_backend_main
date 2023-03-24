package civil.controllers

import civil.apis.RecommendationsApi.getAllRecommendationsEndpoint
import civil.repositories.recommendations.RecommendationsRepositoryLive
import civil.services.{RecommendationsService, RecommendationsServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{Has, ZIO}

import java.util.UUID

object RecommendationsController {
  val getAllRecommendationEndpointRoute: Http[Has[RecommendationsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllRecommendationsEndpoint)(targetContentId => {
      RecommendationsService.getAllRecommendations(UUID.fromString(targetContentId))
        .map(recs => {
          Right((recs))
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(RecommendationsRepositoryLive.live >>> RecommendationsServiceLive.live)
    })
  }

}
