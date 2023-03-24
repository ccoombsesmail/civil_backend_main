package civil.controllers

import civil.apis.TopicLikesApi.updateTopicLikesEndpoint
import civil.repositories.topics.TopicLikesRepositoryLive
import civil.services.topics.{TopicLikesService, TopicLikesServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio.{Has, ZIO}


object TopicLikesController {
  val updateTopicLikesEndpointRoute: Http[Has[TopicLikesService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(updateTopicLikesEndpoint){ case (jwt, jwtType, topicLikeDislikeData) => {
      TopicLikesService.addRemoveTopicLikeOrDislike(jwt, jwtType, topicLikeDislikeData)
        .map(topic => {
          Right(topic)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(TopicLikesRepositoryLive.live >>> TopicLikesServiceLive.live)
    }}
  }
}
