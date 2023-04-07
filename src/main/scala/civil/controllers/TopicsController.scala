package civil.controllers

import civil.controllers.ParseUtils.{extractJwtData, parseBody, parseTopicId}
import civil.models.IncomingTopic
import civil.services.topics.TopicService
import zhttp.http._
import zio._
import zio.json.EncoderOps


final case class TopicsController(topicService: TopicService) {
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "topics" =>
      for {
        authDataOpt <- extractJwtData(req)
        incomingTopic <- parseBody[IncomingTopic](req)
        res <- topicService.insertTopic(authDataOpt.get._1, authDataOpt.get._2, incomingTopic)
      } yield Response.json(res.toJson)

    case req @ Method.GET -> !! / "topics" =>
      for {
        authDataOpt <- extractJwtData(req)
        topics <- topicService.getTopicsAuthenticated(
          authDataOpt.get._1,
          authDataOpt.get._2,
          req.url.queryParams("skip").head.toInt
        )
      } yield Response.json(topics.toJson)

    case req @ Method.GET -> !! / "topics" / "user" / userId =>
      for {
        authDataOpt <- extractJwtData(req)
        res <- topicService.getUserTopics(authDataOpt.get._1, authDataOpt.get._2, userId)
      } yield Response.json(res.toJson)

    case req @ Method.GET -> !! / "topics" / topicId =>
      for {
        authDataOpt <- extractJwtData(req)
        topicId <- parseTopicId(topicId)
        res <- topicService.getTopic(authDataOpt.get._1, authDataOpt.get._2, topicId.id)
      } yield Response.json(res.toJson)
  }
}

object TopicsController {
  val layer: URLayer[TopicService, TopicsController] = ZLayer.fromFunction(TopicsController.apply _)
}