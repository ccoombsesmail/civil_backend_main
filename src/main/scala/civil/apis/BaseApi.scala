package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

import civil.models.{BadRequest, AppError, NoContent, NotFound, Unauthorized, Unknown}
import sttp.tapir._
import sttp.tapir.ztapir.header
import sttp.model.StatusCode


object BaseApi {
//  val baseEndpoint: Endpoint[Unit, AppError, Unit, Any] = {
//    endpoint.in("api" / "v1")
//    (
//        oneOf(
//          oneOfMapping(StatusCode.InternalServerError, jsonBody[InternalServerError].description("internal error")),
//          oneOfMapping(StatusCode.NotFound, jsonBody[NotFound].description("not found")),
//          oneOfMapping(StatusCode.Unauthorized, jsonBody[Unauthorized].description("unauthorized")),
//          oneOfMapping(StatusCode.BadRequest, jsonBody[BadRequest].description("bad request")),
//          oneOfMapping(StatusCode.NoContent, emptyOutput.map(_ => NoContent)(_ => ())),
//          oneOfDefaultMapping(jsonBody[Unknown].description("unknown"))
//        )
//      )
//
//  }
//
//  val baseEndpointAuthenticated: Endpoint[(String, String), AppError, Unit, Any] = {
//    endpoint.in("api" / "v1")
//      .in(header[String]("authorization"))
//      .in(header[String]("X-JWT-TYPE"))
//      .errorOut(
//        oneOf(
//          oneOfMapping(StatusCode.InternalServerError, jsonBody[InternalServerError].description("internal error")),
//          oneOfMapping(StatusCode.NotFound, jsonBody[NotFound].description("not found")),
//          oneOfMapping(StatusCode.Unauthorized, jsonBody[Unauthorized].description("unauthorized")),
//          oneOfMapping(StatusCode.NoContent, emptyOutput.map(_ => NoContent)(_ => ())),
//          oneOfDefaultMapping(jsonBody[Unknown].description("unknown"))
//        )
//      )
//  }


}
