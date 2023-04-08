package civil

import civil.controllers._
import civil.http.CivilServer
import civil.services._
import civil.repositories._
import civil.repositories.comments.{CommentCivilityRepositoryLive, CommentLikesRepositoryLive, CommentsRepositoryLive}
import civil.repositories.recommendations.{OpposingRecommendationsRepositoryLive, RecommendationsRepositoryLive}
import civil.repositories.topics._
import civil.services.comments._
import civil.services.topics._
import zio._

object Civil extends zio.ZIOAppDefault {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global
//
//  private val PORT = 8090
//  private val routes = {
//    HealthCheckController.healthCheckEndpointRoute
//      UsersController.upsertDidUserEndpointRoute <>
//      UsersController.getUserEndpointRoute <>
//      UsersController.updateUserIconEndpointRoute <>
//      UsersController.uploadUserIconEndpointRoute <>
//      UsersController.receiveWebHookEndpointRoute <>
//      UsersController.updateUserBioInformationEndpointRoute <>
//      UsersController.createUserTagEndpointRoute <>
//      UsersController.checkIfTagExistsEndpointRoute <>
//      TopicsController.newTopicEndpointRoute <>
//      TopicsController.getUserTopicsEndpointRoute <>
//      TopicsController.getTopicsEndpointAuthenticatedRoute <>
//      TopicsController.getTopicEndpointRoute <>
//      TopicLikesController.updateTopicLikesEndpointRoute <>
//      DiscussionsController.newDiscussionEndpointRoute <>
//      DiscussionsController.getAllDiscussionsEndpointRoute <>
//      DiscussionsController.getDiscussionEndpointRoute <>
//      DiscussionsController.getGeneralDiscussionIdEndpointRoute <>
//      DiscussionsController.getUserDiscussionsEndpointRoute <>
//      CommentsController.newCommentEndpointRoute <>
//      CommentsController.getAllCommentsEndpointRoute <>
//      CommentsController.getCommentEndpointRoute <>
//      CommentsController.getAllCommentRepliesEndpointRoute <>
//      CommentsController.getUserCommentsEndpointRoute <>
//      CommentCivilityController.updateCivilityEndpointRoute <>
//      CommentCivilityController.updateTribunalCommentCivilityEndpointRoute <>
//      CommentLikesController.updateCommentLikesEndpointRoute <>
//      CommentLikesController.updateTribunalCommentLikesEndpointRoute <>
//      EnumsController.getAllEnumsEndpointRoute <>
//      FollowsController.newFollowEndpointRoute <>
//      FollowsController.deleteFollowEndpointRoute <>
//      FollowsController.getAllFollowersEndpointRoute <>
//      FollowsController.getAllFollowedEndpointRoute <>
//      OpposingRecommendationsController.getAllOpposingRecommendationEndpointRoute <>
//      OpposingRecommendationsController.newOpposingRecommendationEndpointRoute <>
//      RecommendationsController.getAllRecommendationEndpointRoute <>
//      ReportsController.newReportEndpointRoute <>
//      ReportsController.getReportEndpointRoute <>
//      TribunalVotesController.newTribunalVoteEndpointRoute <>
//      TribunalCommentsController.newTopicTribunalVoteEndpointRoute <>
//      TribunalCommentsController.getTribunalCommentsEndpointRoute <>
//      TribunalCommentsController.getTribunalCommentsBatchEndpointRoute <>
//      PollVotesController.createPollVoteEndpointRoute <>
//      PollVotesController.deletePollVoteEndpointRoute <>
//      PollVotesController.getPollVoteDataEndpointRoute <>
//      SearchController.searchAllEndpointRoute <>
//      SearchController.searchAllUsersEndpointRoute
//
//
//  }
//
//  val app: HttpApp[
//    ZEnv
//      with TopicService
//      with TribunalCommentsService
//      with DiscussionService
//      with UsersService
//      with CommentCivilityService
//      with TopicLikesService
//      with CommentLikesService
//      with CommentsService
//      with FollowsService
//      with OpposingRecommendationsService
//      with RecommendationsService
//      with ReportsService
//      with TribunalVotesService
//      with PollVotesService
//      with AuthenticationService
//      with SearchService,
//    Throwable
//  ] =
//    CORS(routes, config = CORSConfig(anyOrigin = true, allowCredentials = true))
//
//
//  val authLayer = AuthenticationServiceLive.live
//  val topicLayer = TopicsController.topicsLayer
//  val discussionsLayer = DiscussionsController.layer
//  val userLayer = UsersRepositoryLive.live >>> UsersServiceLive.live
//  val commentsLayer = CommentsController.layer
//  val followsLayer = FollowsRepositoryLive.live >>> FollowsServiceLive.live
//  val opposingRecsLayer =
//    OpposingRecommendationsRepositoryLive.live >>> OpposingRecommendationsServiceLive.live
//  val recsLayer =
//    RecommendationsRepositoryLive.live >>> RecommendationsServiceLive.live
//  val commentCivilityLayer =
//    CommentCivilityRepositoryLive.live >>> CommentCivilityServiceLive.live
//  val commentLikesLayer =
//    CommentLikesRepositoryLive.live >>> CommentLikesServiceLive.live
//  val topicLikesLayer =
//    TopicLikesRepositoryLive.live >>> TopicLikesServiceLive.live
//  val reportsLayer = {
//    ReportsRepositoryLive.live >>> ReportsServiceLive.live
//  }
//  val tribunalVotesLayer = {
//    TribunalVotesRepositoryLive.live >>> TribunalVotesServiceLive.live
//  }
//  val tribunalCommentsLayer = {
//    (TribunalCommentsRepositoryLive.live ++ UsersRepositoryLive.live) >>> TribunalCommentsServiceLive.live
//  }
//  val pollVotesLayer = PollVotesController.pollVotesLayer
//
//
//  val fullLayer =
//    topicLayer ++ discussionsLayer ++ userLayer ++ commentsLayer ++ followsLayer ++ opposingRecsLayer ++ commentLikesLayer ++
//      recsLayer ++ commentCivilityLayer ++ topicLikesLayer ++ reportsLayer ++ tribunalVotesLayer ++ tribunalCommentsLayer ++ pollVotesLayer ++ authLayer ++ searchLayer

  override val run: Task[Unit] = {
    val ONE_MB = 1000000
    ZIO
      .serviceWithZIO[CivilServer](_.start)
      .provide(
        CivilServer.layer,
        TopicLikesServiceLive.layer,
        TopicLikesController.layer,
        TopicLikesRepositoryLive.layer,
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
        TopicsController.layer,
        TopicServiceLive.layer,
        TopicRepositoryLive.layer,
        TribunalCommentsController.layer,
        TribunalCommentsServiceLive.layer,
        TribunalCommentsRepositoryLive.layer,
        UsersController.layer,
        UsersServiceLive.layer,
        UsersRepositoryLive.layer,
        QuillContext.dataSourceLayer,
        AuthenticationServiceLive.layer,
        PollsRepositoryLive.layer
      )

  }
}
