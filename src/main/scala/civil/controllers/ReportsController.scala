package civil.controllers

import civil.services.ReportsService
import zio.http._
import zio.{URLayer, ZIO, ZLayer}
import zio.json.EncoderOps

import java.util.UUID
import civil.controllers.ParseUtils._
import civil.errors.AppError.JsonDecodingError
import civil.models.Report


final case class ReportsController(reportsService: ReportsService) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.POST -> !! / "api" / "v1" / "reports" =>
        (for {
          authData <- extractJwtData(req)
          (jwt, jwtType) = authData
          incomingReport <- parseBody[Report](req)
          _ <- reportsService.addReport(jwt, jwtType, incomingReport)
        } yield Response.ok).catchAll(_.toResponse)

      case req @ Method.GET -> !! / "api" / "v1" / "reports" =>
        (for {
          contentId <- parseQueryFirst(req, "contentId")

          reportInfo <- req.headers.get("authorization") match {
            case Some(jwt) =>
              for {
                jwtTypeHeader <- ZIO
                  .fromOption(req.headers.get("X-JWT-TYPE"))
                  .orElseFail(JsonDecodingError(new Throwable("error")))
                jwtType = jwtTypeHeader
                // Call the function for authenticated users
                reportInfo <- reportsService.getReport(
                  jwt,
                  jwtType,
                  UUID.fromString(contentId)
                )
              } yield reportInfo
            case None =>
              // Call the function for non-authenticated users
              reportsService.getReportUnauthenticated(
                UUID.fromString(contentId)
              )
          }
        } yield Response.json(reportInfo.toJson)).catchAll(_.toResponse)
    }
}

object ReportsController {
  val layer: URLayer[ReportsService, ReportsController] =
    ZLayer.fromFunction(ReportsController.apply _)
}
