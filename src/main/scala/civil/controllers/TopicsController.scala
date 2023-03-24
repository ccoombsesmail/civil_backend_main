package civil.controllers

import java.util.UUID
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import civil.services.{AuthenticationService, AuthenticationServiceLive}
import civil.apis.TopicsApi._
import civil.config.Config
import civil.models.Topics
import civil.repositories.PollsRepositoryLive
import civil.repositories.recommendations.RecommendationsRepositoryLive
import civil.repositories.topics.TopicRepositoryLive
import civil.services.topics.{TopicService, TopicServiceLive}
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._

import scala.util.{Failure, Success}

object TopicsController {

  val topicsLayer: ZLayer[Any, Throwable, Has[TopicService]] =
    ((RecommendationsRepositoryLive.live >>> TopicRepositoryLive.live) ++ PollsRepositoryLive.live ++ AuthenticationServiceLive.live) >>> TopicServiceLive.live

  val newTopicEndpointRoute
      : Http[Any, Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(newTopicEndpoint) {
      case (jwt, jwtType, incomingTopic) =>
        TopicService
          .insertTopic(jwt, jwtType, incomingTopic)
          .map(topic => {
            Right(topic)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(topicsLayer)
    }
  }

  val getTopicsEndpointRoute: Http[Has[
    TopicService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllTopicsEndpoint) {
      case () => {
        TopicService
          .getTopics()
          .map(topics => {
            Right(topics)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(topicsLayer)
      }
    }
  }

  val getTopicsEndpointAuthenticatedRoute: Http[Has[
    TopicService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getAllTopicsEndpointAuthenticated) {
      case (jwt, jwtType, offset) => {
        TopicService
          .getTopicsAuthenticated(jwt, jwtType, offset)
          .map(topics => {
            Right(topics)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(topicsLayer)
      }
    }
  }

  val getTopicEndpointRoute: Http[Has[
    TopicService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getTopicEndpoint) {
      case (jwt, jwtType, topicId) => {
        TopicService
          .getTopic(jwt, jwtType, UUID.fromString(topicId))
          .map(topic => {
            Right(topic)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(topicsLayer)
      }
    }
  }

  val getUserTopicsEndpointRoute: Http[Has[
    TopicService
  ], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(getUserTopics) {
      case (jwt, jwtType, userId) => {
        TopicService
          .getUserTopics(jwt, jwtType, userId)
          .map(userTopics => {
            Right(userTopics)
          })
          .catchAll(e => ZIO.succeed(Left(e)))
          .provideLayer(topicsLayer)
      }
    }
  }

}
