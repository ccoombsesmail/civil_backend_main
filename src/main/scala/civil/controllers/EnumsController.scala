package civil.controllers

import civil.models.enums.TopicCategories
import zio.http._
import zio._
import zio.http.model.Method
import zio.json.EncoderOps

final case class EnumsController() {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.GET -> !! / "api" / "v1" / "enums"  =>
     ZIO.succeed(Response.json(TopicCategories.list.toJson))
  }

}

object EnumsController {

  val layer: URLayer[Any, EnumsController] = ZLayer.fromFunction(EnumsController.apply _)

}