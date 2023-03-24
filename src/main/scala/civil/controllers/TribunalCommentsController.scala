package civil.controllers

import civil.apis.TribunalCommentsApi._
import civil.models.enums.TribunalCommentType
import civil.repositories.{TribunalCommentsRepositoryLive, UsersRepositoryLive}
import civil.services.{TribunalCommentsService, TribunalCommentsServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{Has, ZIO}

object TribunalCommentsController {
  val newTopicTribunalVoteEndpointRoute: Http[Has[TribunalCommentsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(newTribunalCommentEndpoint){ case (jwt, jwtType, tribunalComment) =>
     TribunalCommentsService.insertComment(jwt, jwtType, tribunalComment)
        .map(res => {
          Right(res)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer((TribunalCommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> TribunalCommentsServiceLive.live)
    }
  }

  val getTribunalCommentsEndpointRoute: Http[Has[TribunalCommentsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getTribunalCommentsEndpoint){ case (jwt, jwtType, contentId, commentType) =>
      TribunalCommentsService.getComments(jwt, jwtType, java.util.UUID.fromString(contentId), TribunalCommentType.withName(commentType))
        .map(res => {
          Right(res)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer((TribunalCommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> TribunalCommentsServiceLive.live)
    }
  }


  val getTribunalCommentsBatchEndpointRoute: Http[Has[TribunalCommentsService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getTribunalCommentsBatchEndpoint){ case (jwt, jwtType, contentId) =>
      TribunalCommentsService.getCommentsBatch(jwt, jwtType, java.util.UUID.fromString(contentId))
        .map(res => {
          Right(res)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer((TribunalCommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> TribunalCommentsServiceLive.live)
    }
  }
}
