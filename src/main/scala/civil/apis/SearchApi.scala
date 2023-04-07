package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.{baseEndpoint, baseEndpointAuthenticated}
import civil.models.{AppError, SearchResult}
import sttp.tapir.{Endpoint, query}


object SearchApi {
  val searchAllEndpoint: Endpoint[String, AppError,  List[SearchResult], Any] =
    baseEndpoint.get
      .in("search")
      .in(query[String]("filterText"))
      .out(jsonBody[ List[SearchResult]])

  val searchAllUsersEndpoint: Endpoint[String, AppError, List[SearchResult], Any] =
    baseEndpoint.get
      .in("search")
      .in("users")
      .in(query[String]("filterText"))
      .out(jsonBody[List[SearchResult]])
}
