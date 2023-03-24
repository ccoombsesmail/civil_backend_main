package civil.controllers

import civil.apis.CommentLikesApi.{updateCommentLikesEndpoint, updateTribunalCommentLikesEndpoint}
import civil.repositories.comments.CommentLikesRepositoryLive
import civil.services.comments.{CommentLikesService, CommentLikesServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{Has, ZIO}


object CommentLikesController {
  val updateCommentLikesEndpointRoute: Http[Has[CommentLikesService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(updateCommentLikesEndpoint) { case (jwt, jwtType, commentLikeDislikeData) =>
      CommentLikesService.addRemoveCommentLikeOrDislike(jwt, jwtType, commentLikeDislikeData)
        .map(likedDislikedData => {
          Right(likedDislikedData)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(CommentLikesRepositoryLive.live >>> CommentLikesServiceLive.live)
    }
  }

  val updateTribunalCommentLikesEndpointRoute: Http[Has[CommentLikesService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(updateTribunalCommentLikesEndpoint) { case (jwt, jwtType, commentLikeDislikeData) =>
      CommentLikesService.addRemoveTribunalCommentLikeOrDislike(jwt, jwtType, commentLikeDislikeData)
        .map(likedDislikedData => {
          Right(likedDislikedData)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(CommentLikesRepositoryLive.live >>> CommentLikesServiceLive.live)
    }
  }
}
