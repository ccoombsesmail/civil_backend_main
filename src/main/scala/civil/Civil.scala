package civil

import civil.controllers._
import civil.http.CivilServer
import civil.services._
import civil.repositories._
import civil.repositories.comments.{CommentCivilityRepositoryLive, CommentLikesRepositoryLive, CommentsRepositoryLive}
import civil.repositories.discussions.{DiscussionLikesRepositoryLive, DiscussionRepositoryLive}
import civil.repositories.recommendations.{OpposingRecommendationsRepositoryLive, RecommendationsRepositoryLive}
import civil.repositories.spaces._
import civil.services.comments._
import civil.services.discussions.{DiscussionLikesServiceLive, DiscussionServiceLive}
import civil.services.spaces._
import zio._
import zio.http.ServerConfig

object Civil extends zio.ZIOAppDefault {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  override val run: Task[Unit] = {
    val ONE_MB = 1000000
    ZIO
      .serviceWithZIO[CivilServer](_.start)
      .provide(
        CivilServer.layer,
        zio.http.Server.live,
        zio.http.ServerConfig.live(ServerConfig.default.port(8090)),
        SpaceLikesServiceLive.layer,
        SpaceLikesController.layer,
        SpaceLikesRepositoryLive.layer,
        CommentCivilityController.layer,
        CommentCivilityServiceLive.layer,
        CommentCivilityRepositoryLive.layer,
        CommentLikesController.layer,
        CommentLikesServiceLive.layer,
        CommentLikesRepositoryLive.layer,
        CommentsController.layer,
        CommentsServiceLive.layer,
        CommentsRepositoryLive.layer,
        FollowsController.layer,
        FollowsServiceLive.layer,
        FollowsRepositoryLive.layer,
        DiscussionsController.layer,
        DiscussionServiceLive.layer,
        DiscussionRepositoryLive.layer,
        DiscussionLikesController.layer,
        DiscussionLikesServiceLive.layer,
        DiscussionLikesRepositoryLive.layer,
        EnumsController.layer,
        HealthCheckController.layer,
        OpposingRecommendationsController.layer,
        OpposingRecommendationsServiceLive.layer,
        OpposingRecommendationsRepositoryLive.layer,
        PollVotesController.layer,
        PollVotesServiceLive.layer,
        PollVotesRepositoryLive.layer,
        RecommendationsController.layer,
        RecommendationsServiceLive.layer,
        RecommendationsRepositoryLive.layer,
        ReportsController.layer,
        ReportsServiceLive.layer,
        ReportsRepositoryLive.layer,
        SearchController.layer,
        SearchServiceLive.layer,
        SpacesController.layer,
        SpacesServiceLive.layer,
        SpaceRepositoryLive.layer,
        TribunalCommentsController.layer,
        TribunalCommentsServiceLive.layer,
        TribunalCommentsRepositoryLive.layer,
        UsersController.layer,
        UsersServiceLive.layer,
        UsersRepositoryLive.layer,
        QuillContext.dataSourceLayer,
        AuthenticationServiceLive.layer,
        PollsRepositoryLive.layer,
        TribunalVotesController.layer,
        TribunalVotesRepositoryLive.layer,
        TribunalVotesServiceLive.layer,
        SpaceFollowsController.layer,
        SpaceFollowsServiceLive.layer,
        SpaceFollowsRepositoryLive.layer
      )

  }
}
