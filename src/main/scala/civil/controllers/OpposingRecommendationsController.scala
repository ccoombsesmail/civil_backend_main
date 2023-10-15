package civil.controllers

import java.util.UUID
import civil.services.OpposingRecommendationsService
import civil.models.OpposingRecommendations
import zio.http._
import zio._
import civil.controllers.ParseUtils._

import zio.json.EncoderOps

final case class OpposingRecommendationsController(opposingRecommendationsService: OpposingRecommendationsService) {
    val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "api" / "v1" / "opposing-recommendations" =>
        (for {
          opposingRec <- parseBody[OpposingRecommendations](req)
          _ <- opposingRecommendationsService.insertOpposingRecommendation(opposingRec)
        } yield Response.ok).catchAll(_.toResponse)

      case req@Method.GET -> !! / "api" / "v1" / "opposing-recommendations" if (req.url.queryParams.nonEmpty) =>
        (for {
          recs <- opposingRecommendationsService.getAllOpposingRecommendations(UUID.fromString(req.url.queryParams.get("targetContentId").head.asString))
        } yield Response.json(recs.toJson)).catchAll(_.toResponse)
    }
  }

  object OpposingRecommendationsController {
    val layer: URLayer[OpposingRecommendationsService, OpposingRecommendationsController] = ZLayer.fromFunction(OpposingRecommendationsController.apply _)
  }

