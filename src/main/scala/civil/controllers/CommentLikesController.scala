package civil.controllers

import civil.controllers.ParseUtils.{extractJwtData, parseBody}
import civil.errors.AppError.JsonDecodingError
import civil.models.UpdateCommentLikes
import civil.services.comments.CommentLikesService
import zhttp.http.{Http, Method, Request, Response}
import zhttp.http._
import zio._
import zio.json.EncoderOps


final case class CommentLikesController(commentCivilityService: CommentLikesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.PUT -> !! / "comments" / "likes"  =>
      for {
        updateCommentLikes <- parseBody[UpdateCommentLikes](req)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        likeGivenResponse <- commentCivilityService.addRemoveCommentLikeOrDislike(authDataOpt.get._1, authDataOpt.get._2, updateCommentLikes)
      } yield Response.json(likeGivenResponse.toJson)

    case req @ Method.PUT -> !! / "comments" / "likes-tribunal" =>
      for {
        updateCommentLikes <- parseBody[UpdateCommentLikes](req)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        likeGivenResponse <- commentCivilityService.addRemoveTribunalCommentLikeOrDislike(authDataOpt.get._1, authDataOpt.get._2, updateCommentLikes)
      } yield Response.json(likeGivenResponse.toJson)
  }

}

object CommentLikesController {

  val layer: URLayer[CommentLikesService, CommentLikesController] = ZLayer.fromFunction(CommentLikesController.apply _)

}
