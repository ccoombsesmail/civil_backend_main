package civil.controllers

import zio.ZIO
import zio.http._
import zio._
import zio.http.model.Method

final case class HealthCheckController() {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.GET -> !! / "healthcheck" =>
      (for {
        _ <- ZIO.succeed("success")
      } yield Response.text("success"))
  }
}

object HealthCheckController {
  val layer: URLayer[Any, HealthCheckController] = ZLayer.fromFunction(HealthCheckController.apply _)
}