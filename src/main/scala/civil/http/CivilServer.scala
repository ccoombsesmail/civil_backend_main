package civil.http

import cats.implicits.catsSyntaxOptionId
import civil.config.Config
import civil.controllers._
import zio.http.{HttpAppMiddleware, _}
import zio.http.Server._
import zio._
import zio.http.Method.{DELETE, GET, OPTIONS, PATCH, POST, PUT}
import zio.http.internal.middlewares.Cors.CorsConfig

case class CivilServer(
    topicLikesController: SpaceLikesController,
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
    topicsController: SpacesController,
    tribunalCommentsController: TribunalCommentsController,
    usersController: UsersController,
    tribunalVotesController: TribunalVotesController,
    topicFollowsController: SpaceFollowsController,
    discussionLikesController: DiscussionLikesController,
    discussionFollowsController: DiscussionFollowsController,
    tribunalJuryMembersController: TribunalJuryMembersController
) {

  private val allRoutes: Http[Any, Throwable, Request, Response] = {
    topicsController.routes ++ usersController.routes ++ topicLikesController.routes ++ commentCivilityController.routes ++ commentLikesController.routes ++
      commentsController.routes ++ followsController.routes ++ discussionsController.routes ++ enumsController.routes ++ healthCheckController.routes ++
      opposingRecommendationsController.routes ++ discussionFollowsController.routes ++ recommendationsController.routes ++ pollVotesController.routes ++
      reportsController.routes ++ searchController.routes ++ tribunalCommentsController.routes ++ tribunalVotesController.routes ++ topicFollowsController.routes ++
      discussionLikesController.routes ++ tribunalJuryMembersController.routes
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

  def start = {
    import civil.config.Config
    val scheme = Config().getString("access-control-header.scheme")
    val host = Config().getString("access-control-header.host")
    val port =
      if (Config().hasPath("access-control-header.port"))
        Some(Config().getInt("access-control-header.port"))
      else
        None

    val corsMiddleware = HttpAppMiddleware.cors(
      CorsConfig(
        allowCredentials = Header.AccessControlAllowCredentials.allow(true),
        allowedMethods = Header.AccessControlAllowMethods.Some(NonEmptyChunk(PUT, PATCH, GET, POST, DELETE, OPTIONS)),
        allowedOrigin = (header) => {
          println(header)
          Header.AccessControlAllowOrigin(scheme, host, port).some
        }
      )
    )
    HttpAppMiddleware.beautifyErrors

    for {
      _ <- ZIO.logInfo("Starting Server")
      _ <- serve {
        (allRoutes @@ HttpAppMiddleware.debug @@ HttpAppMiddleware.beautifyErrors @@ corsMiddleware).withDefaultErrorResponse
      }
    } yield ()

  }
}

object CivilServer {

  val layer: ZLayer[
    SpaceLikesController
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
      with SpacesController
      with TribunalCommentsController
      with UsersController
      with TribunalVotesController
      with SpaceFollowsController
      with PollVotesController
      with DiscussionLikesController
      with DiscussionFollowsController
      with TribunalJuryMembersController,
    Nothing,
    CivilServer
  ] =
    ZLayer.fromFunction(CivilServer.apply _)

}
