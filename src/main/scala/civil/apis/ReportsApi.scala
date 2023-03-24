package civil.apis

import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.baseEndpointAuthenticated
import civil.models.{ErrorInfo, Report, ReportInfo}
import sttp.tapir.{Endpoint, path, query}
import sttp.tapir.json.circe.jsonBody

import java.util.UUID

object ReportsApi {
  val newReportEndpoint: Endpoint[(String, String, Report), ErrorInfo, Unit, Any] =
    baseEndpointAuthenticated.post
      .in("reports")
      .in(jsonBody[Report])

  val getReportEndpoint: Endpoint[(String, String, UUID), ErrorInfo, ReportInfo, Any] =
    baseEndpointAuthenticated.get
      .in("reports")
      .in(query[UUID]("contentId"))
      .out(jsonBody[ReportInfo])
}
