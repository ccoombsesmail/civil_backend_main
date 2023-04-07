package civil.controllers

import civil.services.SearchService
import zhttp.http.{Http, Request, Response}
import zio.{URLayer, ZLayer}
import zhttp.http._
import zio.json.EncoderOps


final case class SearchController(searchService: SearchService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.GET -> !! / "search" =>
      for {
        res <- searchService.searchAll(req.url.queryParams("filterText").head)
      } yield Response.json(res.toJson)

    case req @ Method.GET -> !! / "search" / "users" =>
      for {
        res <- searchService.searchAllUsers(req.url.queryParams("filterText").head)
      } yield Response.json(res.toJson)
  }
}

object SearchController {
  val layer: URLayer[SearchService, SearchController] = ZLayer.fromFunction(SearchController.apply _)
}
