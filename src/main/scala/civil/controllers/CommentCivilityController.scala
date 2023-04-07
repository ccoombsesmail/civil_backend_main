package civil.controllers

import civil.errors.AppError.JsonDecodingError
import civil.controllers.ParseUtils.{extractJwtData, parseBody}
import civil.models.UpdateCommentCivility
import civil.services.comments.CommentCivilityService
import civil.services.topics.TopicLikesService
import zhttp.http._
import zio._
import zio.json.EncoderOps



final case class CommentCivilityController(commentCivilityService: CommentCivilityService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.PUT -> !! / "comments" / "civility"  =>
      for {
        updateCommentCivility <- parseBody[UpdateCommentCivility](req)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        civilityGivenResponse <- commentCivilityService.addOrRemoveCommentCivility(authDataOpt.get._1, authDataOpt.get._2, updateCommentCivility)
      } yield Response.json(civilityGivenResponse.toJson)

    case req@Method.PUT -> !! / "comments" / "civility-tribunal" =>
      for {
        updateCommentCivility <- parseBody[UpdateCommentCivility](req)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        civilityGivenResponse <- commentCivilityService.addOrRemoveTribunalCommentCivility(authDataOpt.get._1, authDataOpt.get._2, updateCommentCivility)
      } yield Response.json(civilityGivenResponse.toJson)
  }

}

object CommentCivilityController {

  val layer: URLayer[CommentCivilityService, CommentCivilityController] = ZLayer.fromFunction(CommentCivilityController.apply _)

}
