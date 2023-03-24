package civil.controllers

import civil.apis.HealthCheck.healthCheckEndpoint
import civil.services.comments.CommentCivilityService
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{Has, ZIO}

object HealthCheckController {
  val healthCheckEndpointRoute: Http[Has[CommentCivilityService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(healthCheckEndpoint) { case () =>
      ZIO.succeed(Right("success"))
    }
  }
}
