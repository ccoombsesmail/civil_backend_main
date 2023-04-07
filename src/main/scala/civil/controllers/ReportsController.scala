package civil.controllers

import civil.services.ReportsService
import zhttp.http.{Http, Request, Response}
import zio.{URLayer, ZLayer}
import zhttp.http._
import zio.json.EncoderOps

import java.util.UUID
import civil.controllers.ParseUtils._
import civil.models.Report

final case class ReportsController(reportsService: ReportsService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "reports" =>
      for {
        authDataOpt <- extractJwtData(req)
        incomingReport <- parseBody[Report](req)
        _ <- reportsService.addReport(authDataOpt.get._1, authDataOpt.get._2, incomingReport)
      } yield Response.ok

    case req @ Method.GET -> !! / "reports" =>
      for {
        authDataOpt <- extractJwtData(req)
        reportInfo <- reportsService.getReport(authDataOpt.get._1, authDataOpt.get._2, UUID.fromString(req.url.queryParams("contentId").head))
      } yield Response.json(reportInfo.toJson)
  }
}

object ReportsController {
  val layer: URLayer[ReportsService, ReportsController] = ZLayer.fromFunction(ReportsController.apply _)
}