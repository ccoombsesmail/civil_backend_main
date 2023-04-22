package civil.http

import civil.controllers._
import civil.errors.AppError
import zio.http.{HttpAppMiddleware, _}
import zio.http.Server._
import zio._
import zio.http.middleware.Cors.CorsConfig
import zio.http.model.Method.{DELETE, GET, OPTIONS, PATCH, POST, PUT}

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
    usersController: UsersController,
    tribunalVotesController: TribunalVotesController,
    topicFollowsController: TopicFollowsController
) {

  private val allRoutes: Http[Any, Throwable, Request, Response] = {
    topicsController.routes ++ usersController.routes ++ topicLikesController.routes ++ commentCivilityController.routes ++ commentLikesController.routes ++ commentsController.routes ++
      followsController.routes ++ discussionsController.routes ++ enumsController.routes ++ healthCheckController.routes ++ opposingRecommendationsController.routes ++
      recommendationsController.routes ++ pollVotesController.routes ++ reportsController.routes ++ searchController.routes ++ tribunalCommentsController.routes ++ tribunalVotesController.routes ++ topicFollowsController.routes
  }

//  private val loggingMiddleware: HttpMiddleware[Any, Nothing] =
//    new HttpMiddleware[Any, Nothing] {
//      override def apply[R1 <: Any, E1 >: Nothing](
//          http: Http[R1, E1, Request, Response]
//      ): Http[R1, E1, Request, Response] =
//        Http.fromOptionFunction[Request] { request =>
//          Random.nextUUID.flatMap { requestId =>
//            ZIO.logAnnotate("REQUEST-ID", requestId.toString) {
//              for {
//                _ <- ZIO.logInfo(s"Request: $request")
//                result <- http(request)
//              } yield result
//            }
//          }
//        }
//    }

  def start =  {
    val corsMiddleware = HttpAppMiddleware.cors(
      CorsConfig(
        anyOrigin = true,
        allowCredentials = true,
        allowedMethods = Some(Set(PUT, PATCH, GET, POST, DELETE, OPTIONS))
      )
    )
    HttpAppMiddleware.beautifyErrors

    for {
      _ <- serve { (allRoutes @@ corsMiddleware @@ HttpAppMiddleware.debug @@ HttpAppMiddleware.beautifyErrors).withDefaultErrorResponse }
    } yield ()

  }
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
      with TribunalVotesController
      with TopicFollowsController
      with PollVotesController,
    Nothing,
    CivilServer
  ] =
    ZLayer.fromFunction(CivilServer.apply _)

}
