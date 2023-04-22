package civil.controllers

import civil.controllers.ParseUtils.{extractJwtData, parseBody, parseTopicId}
import civil.errors.AppError
import civil.errors.AppError.{InternalServerError, JsonDecodingError}
import civil.models.IncomingTopic
import civil.services.topics.TopicService
import zio.http._
import zio._
import zio.http.model.{HTTP_CHARSET, Method, Status}
import zio.json.EncoderOps


final case class TopicsController(topicService: TopicService) {
  val routes: Http[Any, AppError, Request, Response] = Http.collectZIO[Request] {
    case req @ Method.POST -> !! / "api" / "v1" / "topics" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        incomingTopic <- parseBody[IncomingTopic](req)
        res <- topicService.insertTopic(jwt, jwtType, incomingTopic)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "topics" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        _ = println(req.url.queryParams("skip").head.toInt)
        topics <- topicService.getTopicsAuthenticated(
          jwt, jwtType,
          req.url.queryParams("skip").head.toInt
        )
      } yield Response.json(topics.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "topics" / "user" / userId =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        res <- topicService.getUserTopics(jwt, jwtType, userId)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req @ Method.GET -> !! / "api" / "v1" / "topics" / topicId =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        topicId <- parseTopicId(topicId)
        res <- topicService.getTopic(jwt, jwtType, topicId.id)
      } yield Response.json(res.toJson)).catchAll(_.toResponse)

    case req@Method.GET -> !! / "api" / "v1" / "topics-followed" =>
      (for {
        authData <- extractJwtData(req)
        (jwt, jwtType) = authData
        topics <- topicService.getFollowedTopics(
          jwt, jwtType,
        )
      } yield Response.json(topics.toJson)).catchAll(_.toResponse)
  }
}

object TopicsController {
  val layer: URLayer[TopicService, TopicsController] = ZLayer.fromFunction(TopicsController.apply _)
}