package civil.controllers

import java.util.UUID
import civil.services.{AuthenticationServiceLive, DiscussionService, DiscussionServiceLive}
import civil.apis.DiscussionsApi._
import civil.repositories.topics.DiscussionsRepositoryLive
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._

object DiscussionsController {

  val layer =(DiscussionsRepositoryLive.live ++ AuthenticationServiceLive.live) >>> DiscussionServiceLive.live
 
val newDiscussionEndpointRoute: Http[Has[DiscussionService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(newDiscussionEndpoint) { case (jwt, jwtType, incomingDiscussion) =>
      DiscussionService.insertDiscussion(jwt, jwtType, incomingDiscussion)
        .map(discussion => {
          Right(discussion)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(layer)
    }
  }


  val getAllDiscussionsEndpointRoute: Http[Has[DiscussionService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllDiscussionsEndpoint) { case (topicId, skip) => {
      DiscussionService.getDiscussions(UUID.fromString(topicId), skip)
        .map(discussions => {
          Right(discussions)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(layer)
    }}
  }

 val getDiscussionEndpointRoute: Http[Has[DiscussionService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getDiscussionEndpoint)(id => {
      DiscussionService.getDiscussion(UUID.fromString(id))
        .map(discussion => {
          Right(discussion)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(layer)
    }) 
  }

  val getGeneralDiscussionIdEndpointRoute: Http[Has[DiscussionService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getGeneralDiscussionIdEndpoint)(topicId => {
      DiscussionService.getGeneralDiscussionId(UUID.fromString(topicId))
        .map(genDicussionId => {
          Right(genDicussionId)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(layer)
    })
  }

  val getUserDiscussionsEndpointRoute: Http[Has[
    DiscussionService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getUserDiscussions) {
      case (jwt, jwtType, userId) => {
        DiscussionService
          .getUserDiscussions(jwt, jwtType, userId)
          .map(userDiscussions => {
            Right(userDiscussions)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(layer)
      }
    }
  }

}
