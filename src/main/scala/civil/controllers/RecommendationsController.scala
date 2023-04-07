package civil.controllers

import civil.apis.RecommendationsApi.getAllRecommendationsEndpoint
import civil.repositories.recommendations.RecommendationsRepositoryLive
import civil.services.{RecommendationsService, RecommendationsServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{URLayer, ZIO, ZLayer}
import zhttp.http._
import zio.json.EncoderOps

import java.util.UUID
import civil.controllers.ParseUtils._


final case class RecommendationsController(recommendationsService: RecommendationsService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.GET -> !! / "recommendations" =>
      for {
        recs <- recommendationsService.getAllRecommendations(UUID.fromString(req.url.queryParams("targetContentId").head))
      } yield Response.json(recs.toJson)
  }
}

object RecommendationsController {
  val layer: URLayer[RecommendationsService, RecommendationsController] = ZLayer.fromFunction(RecommendationsController.apply _)
}
