package civil.controllers

import civil.controllers.ParseUtils.{extractJwtData, parseBody}
import civil.errors.AppError.JsonDecodingError
import civil.models.UpdateCommentLikes
import civil.services.comments.CommentLikesService
import zio.http._
import zio.http.model.Method

import zio._
import zio.json.EncoderOps


final case class CommentLikesController(commentCivilityService: CommentLikesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.PUT -> !!  / "api" / "v1" / "comments" / "likes"  =>
      (for {
        updateCommentLikes <- parseBody[UpdateCommentLikes](req)
        authData <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        (jwt, jwtType) = authData
        likeGivenResponse <- commentCivilityService.addRemoveCommentLikeOrDislike(jwt, jwtType, updateCommentLikes)
      } yield Response.json(likeGivenResponse.toJson)).catchAll(_.toResponse)

    case req @ Method.PUT -> !! / "api" / "v1" / "comments" / "likes-tribunal" =>
      (for {
        updateCommentLikes <- parseBody[UpdateCommentLikes](req)
        authData <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        (jwt, jwtType) = authData
        likeGivenResponse <- commentCivilityService.addRemoveTribunalCommentLikeOrDislike(jwt, jwtType, updateCommentLikes)
      } yield Response.json(likeGivenResponse.toJson)).catchAll(_.toResponse)
  }

}

object CommentLikesController {

  val layer: URLayer[CommentLikesService, CommentLikesController] = ZLayer.fromFunction(CommentLikesController.apply _)

}
