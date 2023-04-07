package civil.controllers

import civil.apis.EnumsApi.getAllEnumsEndpoint
import civil.models.enums.TopicCategories
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._
import zhttp.http._
import zio._
import zio.json.EncoderOps

final case class EnumsController() {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.GET -> !! / "enums"  =>
     ZIO.succeed(Response.json(TopicCategories.values.toJson))
  }

}

object EnumsController {

  val layer: URLayer[Any, EnumsController] = ZLayer.fromFunction(EnumsController.apply _)

}