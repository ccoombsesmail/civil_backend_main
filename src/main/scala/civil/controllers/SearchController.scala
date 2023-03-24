package civil.controllers

import civil.apis.ReportsApi._
import civil.apis.SearchApi.{searchAllEndpoint, searchAllUsersEndpoint}
import civil.repositories.comments.CommentsRepositoryLive
import civil.repositories.recommendations.RecommendationsRepositoryLive
import civil.repositories.topics.{DiscussionsRepositoryLive, TopicRepositoryLive}
import civil.services.{ReportsService, ReportsServiceLive, SearchService, SearchServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{Has, ZIO}

object SearchController {

  val searchLayer =
    (RecommendationsRepositoryLive.live >>> TopicRepositoryLive.live) ++ DiscussionsRepositoryLive.live ++ CommentsRepositoryLive.live >>> SearchServiceLive.live

  val searchAllEndpointRoute: Http[Has[
    ReportsService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(searchAllEndpoint) { filterText =>
      SearchService
        .searchAll(filterText)
        .map(res => {
          Right(res)
        })
        .catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(searchLayer)
    }
  }

  val searchAllUsersEndpointRoute: Http[Has[
    ReportsService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(searchAllUsersEndpoint) { filterText =>
      SearchService
        .searchAllUsers(filterText)
        .map(res => {
          Right(res)
        })
        .catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(searchLayer)
    }
  }

}
