package civil.controllers

import java.util.UUID
import civil.services.OpposingRecommendationsService
import civil.models.OpposingRecommendations
import zhttp.http.{Http, Request, Response}
import zio._
import zhttp.http._
import civil.controllers.ParseUtils._
import zio.json.EncoderOps

final case class OpposingRecommendationsController(opposingRecommendationsService: OpposingRecommendationsService) {
    val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "opposing-recommendations" =>
        for {
          opposingRec <- parseBody[OpposingRecommendations](req)
          _ <- opposingRecommendationsService.insertOpposingRecommendation(opposingRec)
        } yield Response.ok

      case req@Method.GET -> !! / "opposing-recommendations" if (req.url.queryParams.nonEmpty) =>
        for {
          recs <- opposingRecommendationsService.getAllOpposingRecommendations(UUID.fromString(req.url.queryParams("targetContentId").head))
        } yield Response.json(recs.toJson)
    }
  }

  object OpposingRecommendationsController {
    val layer: URLayer[OpposingRecommendationsService, OpposingRecommendationsController] = ZLayer.fromFunction(OpposingRecommendationsController.apply _)
  }

