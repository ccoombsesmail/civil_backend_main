package civil.controllers

import java.util.UUID
import civil.services.{OpposingRecommendationsService, OpposingRecommendationsServiceLive}
import civil.apis.OpposingRecommendationsApi._
import civil.models.OpposingRecommendations
import civil.repositories.recommendations.OpposingRecommendationsRepositoryLive
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zhttp.http.{Http, Request, Response}
import zio._

object OpposingRecommendationsController {
  val newOpposingRecommendationEndpointRoute: Http[Has[OpposingRecommendationsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(newOpposingRecommendationEndpoint)(opposingRec => {
      OpposingRecommendationsService.insertOpposingRecommendation(opposingRec)
        .map(_ => {
          Right(())
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(OpposingRecommendationsRepositoryLive.live >>> OpposingRecommendationsServiceLive.live)
    })
  }



  val getAllOpposingRecommendationEndpointRoute: Http[Has[OpposingRecommendationsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getOpposingRecommendationEndpoint)(targetContentId => {
      OpposingRecommendationsService.getAllOpposingRecommendations(UUID.fromString(targetContentId))
        .map(recs => {
          Right(recs)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(OpposingRecommendationsRepositoryLive.live >>> OpposingRecommendationsServiceLive.live)
    })
  }



}

