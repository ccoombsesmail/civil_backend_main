package civil.controllers

import sttp.tapir._
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._
import akka.http.scaladsl.server.Route


// trait BaseController {
//   implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

//   def zioRuntime: Runtime[ZEnv]

//   def apiToRoute[I, A](endpoint: Endpoint[Any, I, Unit, A])(f: I => IO[Throwable, A]): UIO[Route] =
//       ZioHttpInterpreter().toHttp(endpoint) 
  
// }
