package civil.http

import civil.controllers._
import zhttp.http._
import zhttp.http.middleware.HttpMiddleware
import zhttp.service.Server
import zio._

case class CivilServer(
    topicLikesController: TopicLikesController,
    commentCivilityController: CommentCivilityController,
    commentLikesController: CommentLikesController,
    commentsController: CommentsController,
    followsController: FollowsController,
    discussionsController: DiscussionsController,
    enumsController: EnumsController,
    healthCheckController: HealthCheckController,
    opposingRecommendationsController: OpposingRecommendationsController,
    recommendationsController: RecommendationsController,
    pollVotesController: PollVotesController,
    reportsController: ReportsController,
    searchController: SearchController,
    topicsController: TopicsController,
    tribunalCommentsController: TribunalCommentsController,
    usersController: UsersController
) {

  private val allRoutes = {
    topicsController.routes ++ usersController.routes ++ topicLikesController.routes ++ commentCivilityController.routes ++ commentLikesController.routes ++ commentsController.routes ++
      followsController.routes ++ discussionsController.routes ++ enumsController.routes ++ healthCheckController.routes ++ opposingRecommendationsController.routes ++
      recommendationsController.routes ++ pollVotesController.routes ++ reportsController.routes ++ searchController.routes ++ tribunalCommentsController.routes
  }

  private val loggingMiddleware: HttpMiddleware[Any, Nothing] =
    new HttpMiddleware[Any, Nothing] {
      override def apply[R1 <: Any, E1 >: Nothing](
          http: Http[R1, E1, Request, Response]
      ): Http[R1, E1, Request, Response] =
        Http.fromOptionFunction[Request] { request =>
          Random.nextUUID.flatMap { requestId =>
            ZIO.logAnnotate("REQUEST-ID", requestId.toString) {
              for {
                _ <- ZIO.logInfo(s"Request: $request")
                result <- http(request)
              } yield result
            }
          }
        }
    }

  def start: ZIO[Any, Throwable, Unit] =
    for {
      port <- System.envOrElse("PORT", "8080").map(_.toInt)
      _ <- Server.start(
        port,
        allRoutes @@ Middleware.cors() @@ loggingMiddleware
      )
    } yield ()

}

object CivilServer {

  val layer: ZLayer[
    TopicLikesController
      with OpposingRecommendationsController
      with RecommendationsController
      with HealthCheckController
      with DiscussionsController
      with EnumsController
      with CommentCivilityController
      with CommentLikesController
      with CommentsController
      with FollowsController
      with ReportsController
      with SearchController
      with TopicsController
      with TribunalCommentsController
      with UsersController
      with PollVotesController,
    Nothing,
    CivilServer
  ] =
    ZLayer.fromFunction(CivilServer.apply _)

}
