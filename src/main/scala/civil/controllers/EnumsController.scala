package civil.controllers

import civil.apis.EnumsApi.getAllEnumsEndpoint
import civil.models.enums.TopicCategories
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._

object EnumsController {
  val getAllEnumsEndpointRoute: Http[Any, Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllEnumsEndpoint)(_ => {
      ZIO.succeed(Right(TopicCategories.values))
    })
  }

}
