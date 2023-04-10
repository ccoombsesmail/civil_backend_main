package civil.controllers

import civil.services.ReportsService
import zio.http._
import zio.{URLayer, ZLayer}
import zio.json.EncoderOps

import java.util.UUID
import civil.controllers.ParseUtils._
import civil.models.Report
import zio.http.model.Method

final case class ReportsController(reportsService: ReportsService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "api" / "v1" / "reports" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        incomingReport <- parseBody[Report](req)
        _ <- reportsService.addReport(jwt, jwtType, incomingReport)
      } yield Response.ok).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "reports" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        reportInfo <- reportsService.getReport(jwt, jwtType, UUID.fromString(req.url.queryParams("contentId").head))
      } yield Response.json(reportInfo.toJson)).catchAll(_.toResponse)
  }
}

object ReportsController {
  val layer: URLayer[ReportsService, ReportsController] = ZLayer.fromFunction(ReportsController.apply _)
}