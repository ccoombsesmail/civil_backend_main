package civil.controllers

import civil.controllers.ParseUtils._
import civil.errors.AppError.JsonDecodingError
import civil.models.{TopicId, UpdateTopicFollows}
import civil.services.topics.TopicFollowsService
import zio._
import zio.http._
import zio.http.model.Method
final case class TopicFollowsController(topicFollowsService: TopicFollowsService){
  val routes: Http[Any, Throwable, Request, Response] = Http.collectZIO[Request] {
    case req@Method.POST -> !! / "api" / "v1" / "topic-follows" => (for {
      authData <- extractJwtData(req)
      (jwt, jwtType) = authData
      topicFollowUpdate <- parseBody[UpdateTopicFollows](req)
        _ <- topicFollowsService.insertTopicFollow(jwt, jwtType, topicFollowUpdate)
      } yield Response.ok).catchAll(_.toResponse)

    case req@Method.DELETE -> !! / "api" / "v1" / "topic-follows" => (for {
      topicIdParam <- parseQuery(req, "followedTopicId")
      topicIdStr <- ZIO.fromOption(topicIdParam.headOption).mapError(e => JsonDecodingError(e.toString))
      topicId <- TopicId.fromString(topicIdStr).mapError(e => JsonDecodingError(e.toString))
      authData <- extractJwtData(req)
      (jwt, jwtType) = authData
      _ <- topicFollowsService.deleteTopicFollow(jwt, jwtType, topicId)
    } yield Response.ok).catchAll(_.toResponse)

  }
}

object TopicFollowsController {

  val layer: URLayer[TopicFollowsService, TopicFollowsController] = ZLayer.fromFunction(TopicFollowsController.apply _)

}
