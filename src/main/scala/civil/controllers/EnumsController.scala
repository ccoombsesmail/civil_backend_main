package civil.controllers

import civil.models.enums.SpaceCategories
import zio.http._
import zio._
import zio.http.model.Method
import zio.json.{DeriveJsonCodec, EncoderOps, JsonCodec}

case class EnumValue(name: String, value: String)

object EnumValue {
  implicit val codec: JsonCodec[EnumValue] =
    DeriveJsonCodec.gen[EnumValue]
}
final case class EnumsController() {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case _ @ Method.GET -> !! / "api" / "v1" / "enums"  =>
     ZIO.succeed(Response.json(SpaceCategories.list.map(cat => EnumValue(cat, cat)).toJson))
  }

}

object EnumsController {

  val layer: URLayer[Any, EnumsController] = ZLayer.fromFunction(EnumsController.apply _)

}