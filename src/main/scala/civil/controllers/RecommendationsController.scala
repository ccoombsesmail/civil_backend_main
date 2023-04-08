package civil.controllers

import civil.services.RecommendationsService
import zhttp.http.{Http, Request, Response}
import zio.{URLayer, ZLayer}
import zhttp.http._
import zio.json.EncoderOps

import java.util.UUID


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
