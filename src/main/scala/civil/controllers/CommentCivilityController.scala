package civil.controllers

import civil.apis.CommentCivilityApi._
import civil.repositories.comments.CommentCivilityRepositoryLive
import civil.services.comments.{CommentCivilityService, CommentCivilityServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http._
import zio._

object CommentCivilityController {
  val updateCivilityEndpointRoute: Http[Has[CommentCivilityService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(updateCommentCivilityEndpoint){ case(jwt, jwtType, civilityData) =>
      CommentCivilityService.addOrRemoveCommentCivility(jwt, jwtType, civilityData)
        .map(comment => {
          Right(comment)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(CommentCivilityRepositoryLive.live >>> CommentCivilityServiceLive.live)
    }
  }

  val updateTribunalCommentCivilityEndpointRoute: Http[Has[CommentCivilityService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(updateTribunalCommentCivilityEndpoint){ case(jwt, jwtType, civilityData) =>
      CommentCivilityService.addOrRemoveTribunalCommentCivility(jwt, jwtType, civilityData)
        .map(comment => {
          Right(comment)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(CommentCivilityRepositoryLive.live >>> CommentCivilityServiceLive.live)
    }
  }
}
