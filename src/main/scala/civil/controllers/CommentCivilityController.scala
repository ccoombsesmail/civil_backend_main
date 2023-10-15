package civil.controllers

import civil.errors.AppError.JsonDecodingError
import civil.controllers.ParseUtils.{extractJwtData, parseBody}
import civil.models.UpdateCommentCivility
import civil.services.comments.CommentCivilityService
import zio.http._
import zio._

import zio.json.EncoderOps

final case class CommentCivilityController(
    commentCivilityService: CommentCivilityService
) {
  val routes: Http[Any, Throwable, Request, Response] =
    Http.collectZIO[Request] {
      case req @ Method.PUT -> !! / "api" / "v1" / "comments" / "civility" =>
        (for {
          updateCommentCivility <- parseBody[UpdateCommentCivility](req)
          authData <- extractJwtData(req).mapError(e => JsonDecodingError(e))
          (jwt, jwtType) = authData
          civilityGivenResponse <- commentCivilityService
            .addOrRemoveCommentCivility(jwt, jwtType, updateCommentCivility)
        } yield Response.json(civilityGivenResponse.toJson))
          .catchAll(_.toResponse)

      case req @ Method.PUT -> !! / "api" / "v1" / "comments" / "civility-tribunal" =>
        (for {
          updateCommentCivility <- parseBody[UpdateCommentCivility](req)
          authData <- extractJwtData(req).mapError(e => JsonDecodingError(e))
          (jwt, jwtType) = authData
          civilityGivenResponse <- commentCivilityService
            .addOrRemoveTribunalCommentCivility(
              jwt,
              jwtType,
              updateCommentCivility
            )
        } yield Response.json(civilityGivenResponse.toJson))
          .catchAll(_.toResponse)
    }

}

object CommentCivilityController {

  val layer: URLayer[CommentCivilityService, CommentCivilityController] =
    ZLayer.fromFunction(CommentCivilityController.apply _)

}
