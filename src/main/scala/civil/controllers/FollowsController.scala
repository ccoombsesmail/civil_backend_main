package civil.controllers

import civil.services.{FollowsService, FollowsServiceLive}
import civil.repositories.FollowsRepositoryLive
import civil.apis.FollowsApi._
import civil.models.FollowedUserId
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zhttp.http.{Http, Request, Response}
import zio._

object FollowsController {
  val newFollowEndpointRoute: Http[Has[FollowsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(newFollowEndpoint) { case (jwt, jwtType, followedUserId) => {
      FollowsService.insertFollow(jwt, jwtType, followedUserId)
        .map(user => {
          Right(user)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(FollowsRepositoryLive.live >>> FollowsServiceLive.live)
      }
    }
  }


  val deleteFollowEndpointRoute: Http[Has[FollowsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(deleteFollowEndpoint) { case (jwt, jwtType, followedUserId) =>
      FollowsService.deleteFollow(jwt, jwtType, FollowedUserId(followedUserId))
        .map(user => {
          Right(user)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(FollowsRepositoryLive.live >>> FollowsServiceLive.live)
    }
  }


   val getAllFollowersEndpointRoute: Http[Has[FollowsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllFollowersEndpoint)(userId => { 
      FollowsService.getAllFolowers(userId)
      .map(users => {
        Right((users))
      })
      .provideLayer(FollowsRepositoryLive.live >>> FollowsServiceLive.live)
    }) 
  }

   val getAllFollowedEndpointRoute: Http[Has[FollowsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllFollowedEndpoint)(userId => { 
      FollowsService.getAllFollowed(userId)
      .map(users => {
        Right((users))
      })
      .provideLayer(FollowsRepositoryLive.live >>> FollowsServiceLive.live)
    }) 
  }


}
