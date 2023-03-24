package civil.controllers

import civil.apis.ReportsApi._
import civil.repositories.{ReportsRepositoryLive}
import civil.services.{ReportsService, ReportsServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{Has, ZIO}

object ReportsController {
  val newReportEndpointRoute: Http[Has[ReportsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(newReportEndpoint){ case (jwt, jwtType, incomingReport) =>
      ReportsService.addReport(jwt, jwtType, incomingReport)
        .map(res => {
          Right(res)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(ReportsRepositoryLive.live >>> ReportsServiceLive.live)
    }
  }

  val getReportEndpointRoute: Http[Has[ReportsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getReportEndpoint){ case (jwt, jwtType, contentId) =>
      ReportsService.getReport(jwt, jwtType, contentId)
        .map(reportInfo => {
          Right(reportInfo)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(ReportsRepositoryLive.live >>> ReportsServiceLive.live)
    }
  }
}
