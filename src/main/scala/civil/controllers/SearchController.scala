package civil.controllers

import civil.services.SearchService
import zio.http._

import zio.{URLayer, ZLayer}
import zio.json.EncoderOps


final case class SearchController(searchService: SearchService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.GET -> !! / "api" / "v1" / "search" =>
      (for {
        res <- searchService.searchAll(req.url.queryParams.get("filterText").head.asString)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "search" / "users" =>
      (for {
        res <- searchService.searchAllUsers(req.url.queryParams.get("filterText").head.asString)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)
  }
}

object SearchController {
  val layer: URLayer[SearchService, SearchController] = ZLayer.fromFunction(SearchController.apply _)
}
