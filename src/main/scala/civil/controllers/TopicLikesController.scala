package civil.controllers

import civil.services.topics.TopicLikesService
import zhttp.http._
import zio._
import zio.json._
import ParseUtils._
import civil.errors.AppError.JsonDecodingError
import civil.models.UpdateTopicLikes


final case class TopicLikesController(topicLikeService: TopicLikesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.PUT -> !! / "topic-likes"  =>
      for {
        updateTopicLikes <- parseBody[UpdateTopicLikes](req)
        authDataOpt <- extractJwtData(req).mapError(e => JsonDecodingError(e.toString))
        topicLiked <- topicLikeService.addRemoveTopicLikeOrDislike(authDataOpt.get._1, authDataOpt.get._2, updateTopicLikes)
      } yield Response.json(topicLiked.toJson)
    }

}

object TopicLikesController {

  val layer: URLayer[TopicLikesService, TopicLikesController] = ZLayer.fromFunction(TopicLikesController.apply _)

}