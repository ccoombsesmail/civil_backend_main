package civil.controllers

import civil.services.SearchService
import zio.http._
import zio.http.model.Method
import zio.{URLayer, ZLayer}
import zio.json.EncoderOps


final case class SearchController(searchService: SearchService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.GET -> !! / "api" / "v1" / "search" =>
      (for {
        res <- searchService.searchAll(req.url.queryParams("filterText").head)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "search" / "users" =>
      (for {
        res <- searchService.searchAllUsers(req.url.queryParams("filterText").head)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)
  }
}

object SearchController {
  val layer: URLayer[SearchService, SearchController] = ZLayer.fromFunction(SearchController.apply _)
}
