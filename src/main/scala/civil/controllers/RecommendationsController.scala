package civil.controllers

import civil.services.RecommendationsService
import zio.http._
import zio.http.model.Method
import zio.{URLayer, ZLayer}
import zio.json.EncoderOps

import java.util.UUID


final case class RecommendationsController(recommendationsService: RecommendationsService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.GET -> !! / "api" / "v1" / "recommendations" =>
      (for {
        recs <- recommendationsService.getAllRecommendations(UUID.fromString(req.url.queryParams("targetContentId").head))
      } yield Response.json(recs.toJson)).catchAll(_.toResponse)
  }
}

object RecommendationsController {
  val layer: URLayer[RecommendationsService, RecommendationsController] = ZLayer.fromFunction(RecommendationsController.apply _)
}
