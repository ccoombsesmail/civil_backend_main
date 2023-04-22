package civil.controllers

import civil.services.topics.TopicLikesService
import zio.http._
import zio._
import zio.json._
import ParseUtils._
import civil.models.UpdateTopicLikes
import zio.http.model.{HTTP_CHARSET, Method}


final case class TopicLikesController(topicLikeService: TopicLikesService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.PUT -> !!  / "api" / "v1" / "topic-likes"  => {
      println(req.body.asString(HTTP_CHARSET))
      (for {
        updateTopicLikes <- parseBody[UpdateTopicLikes](req)
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        topicLiked <- topicLikeService.addRemoveTopicLikeOrDislike(jwt, jwtType, updateTopicLikes)
      } yield Response.json(topicLiked.toJson)).catchAll(_.toResponse)
    }
    }

}

object TopicLikesController {

  val layer: URLayer[TopicLikesService, TopicLikesController] = ZLayer.fromFunction(TopicLikesController.apply _)

}