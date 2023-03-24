package civil

import civil.controllers.SearchController.searchLayer
import civil.controllers._
import civil.repositories._
import civil.repositories.comments.{CommentCivilityRepositoryLive, CommentLikesRepositoryLive, CommentsRepositoryLive}
import civil.repositories.recommendations.{OpposingRecommendationsRepositoryLive, RecommendationsRepositoryLive}
import civil.repositories.topics.{DiscussionsRepositoryLive, TopicLikesRepositoryLive, TopicRepositoryLive}
import civil.services._
import civil.services.comments._
import civil.services.topics.{TopicLikesService, TopicLikesServiceLive, TopicService, TopicServiceLive}
import zhttp.http._
import zhttp.service._
import zhttp.service.server.ServerChannelFactory
import zio._

object Civil extends zio.App {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  val PORT = 8090
  val routes = {
    HealthCheckController.healthCheckEndpointRoute <>
      UsersController.upsertDidUserEndpointRoute <>
      UsersController.getUserEndpointRoute <>
      UsersController.updateUserIconEndpointRoute <>
      UsersController.uploadUserIconEndpointRoute <>
      UsersController.receiveWebHookEndpointRoute <>
      UsersController.updateUserBioInformationEndpointRoute <>
      UsersController.createUserTagEndpointRoute <>
      UsersController.checkIfTagExistsEndpointRoute <>
      TopicsController.newTopicEndpointRoute <>
      TopicsController.getUserTopicsEndpointRoute <>
      TopicsController.getTopicsEndpointAuthenticatedRoute <>
      TopicsController.getTopicEndpointRoute <>
      TopicLikesController.updateTopicLikesEndpointRoute <>
      DiscussionsController.newDiscussionEndpointRoute <>
      DiscussionsController.getAllDiscussionsEndpointRoute <>
      DiscussionsController.getDiscussionEndpointRoute <>
      DiscussionsController.getGeneralDiscussionIdEndpointRoute <>
      DiscussionsController.getUserDiscussionsEndpointRoute <>
      CommentsController.newCommentEndpointRoute <>
      CommentsController.getAllCommentsEndpointRoute <>
      CommentsController.getCommentEndpointRoute <>
      CommentsController.getAllCommentRepliesEndpointRoute <>
      CommentsController.getUserCommentsEndpointRoute <>
      CommentCivilityController.updateCivilityEndpointRoute <>
      CommentCivilityController.updateTribunalCommentCivilityEndpointRoute <>
      CommentLikesController.updateCommentLikesEndpointRoute <>
      CommentLikesController.updateTribunalCommentLikesEndpointRoute <>
      EnumsController.getAllEnumsEndpointRoute <>
      FollowsController.newFollowEndpointRoute <>
      FollowsController.deleteFollowEndpointRoute <>
      FollowsController.getAllFollowersEndpointRoute <>
      FollowsController.getAllFollowedEndpointRoute <>
      OpposingRecommendationsController.getAllOpposingRecommendationEndpointRoute <>
      OpposingRecommendationsController.newOpposingRecommendationEndpointRoute <>
      RecommendationsController.getAllRecommendationEndpointRoute <>
      ReportsController.newReportEndpointRoute <>
      ReportsController.getReportEndpointRoute <>
      TribunalVotesController.newTribunalVoteEndpointRoute <>
      TribunalCommentsController.newTopicTribunalVoteEndpointRoute <>
      TribunalCommentsController.getTribunalCommentsEndpointRoute <>
      TribunalCommentsController.getTribunalCommentsBatchEndpointRoute <>
      PollVotesController.createPollVoteEndpointRoute <>
      PollVotesController.deletePollVoteEndpointRoute <>
      PollVotesController.getPollVoteDataEndpointRoute <>
      SearchController.searchAllEndpointRoute <>
      SearchController.searchAllUsersEndpointRoute


  }

  val app: HttpApp[
    ZEnv
      with Has[TopicService]
      with Has[TribunalCommentsService]
      with Has[DiscussionService]
      with Has[UsersService]
      with Has[CommentCivilityService]
      with Has[TopicLikesService]
      with Has[CommentLikesService]
      with Has[CommentsService]
      with Has[FollowsService]
      with Has[OpposingRecommendationsService]
      with Has[RecommendationsService]
      with Has[ReportsService]
      with Has[TribunalVotesService]
      with Has[PollVotesService]
      with Has[AuthenticationService]
      with Has[SearchService],
    Throwable
  ] =
    CORS(routes, config = CORSConfig(anyOrigin = true, allowCredentials = true))


  val authLayer = AuthenticationServiceLive.live
  val topicLayer = TopicsController.topicsLayer
  val discussionsLayer = DiscussionsController.layer
  val userLayer = UsersRepositoryLive.live >>> UsersServiceLive.live
  val commentsLayer = CommentsController.layer
  val followsLayer = FollowsRepositoryLive.live >>> FollowsServiceLive.live
  val opposingRecsLayer =
    OpposingRecommendationsRepositoryLive.live >>> OpposingRecommendationsServiceLive.live
  val recsLayer =
    RecommendationsRepositoryLive.live >>> RecommendationsServiceLive.live
  val commentCivilityLayer =
    CommentCivilityRepositoryLive.live >>> CommentCivilityServiceLive.live
  val commentLikesLayer =
    CommentLikesRepositoryLive.live >>> CommentLikesServiceLive.live
  val topicLikesLayer =
    TopicLikesRepositoryLive.live >>> TopicLikesServiceLive.live
  val reportsLayer = {
    ReportsRepositoryLive.live >>> ReportsServiceLive.live
  }
  val tribunalVotesLayer = {
    TribunalVotesRepositoryLive.live >>> TribunalVotesServiceLive.live
  }
  val tribunalCommentsLayer = {
    (TribunalCommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> TribunalCommentsServiceLive.live
  }
  val pollVotesLayer = PollVotesController.pollVotesLayer


  val fullLayer =
    topicLayer ++ discussionsLayer ++ userLayer ++ commentsLayer ++ followsLayer ++ opposingRecsLayer ++ commentLikesLayer ++
      recsLayer ++ commentCivilityLayer ++ topicLikesLayer ++ reportsLayer ++ tribunalVotesLayer ++ tribunalCommentsLayer ++ pollVotesLayer ++ authLayer ++ searchLayer

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val ONE_MB = 1000000

    val appServer =
      Server.port(PORT) ++
        Server.app(app) ++
        Server.simpleLeakDetection ++
        Server.maxRequestSize(ONE_MB)

    appServer.make.useForever
      .provideCustomLayer(
        fullLayer ++ EventLoopGroup.auto(5) ++ ServerChannelFactory.auto
      )
      .exitCode

  }
}
