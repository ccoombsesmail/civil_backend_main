package civil.services.topics

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
import civil.models.{TopicFollows, TopicId, TopicLiked, UpdateTopicLikes}
import civil.models.NotifcationEvents.{GivingUserNotificationData, TopicLike}
import civil.models.actions.LikedState
import civil.repositories.topics.{TopicFollowsRepository, TopicLikesRepository}
import civil.services.{AuthenticationService, KafkaProducerServiceLive}
import zio.{URLayer, ZIO, ZLayer}

import javax.sql.DataSource

trait TopicFollowsService {
  def insertTopicFollow(
      jwt: String,
      jwtType: String,
      topicId: TopicId
  ): ZIO[Any, AppError, Unit]

  def deleteTopicFollow(
      jwt: String,
      jwtType: String,
      topicId: TopicId
  ): ZIO[Any, AppError, Unit]

}

object TopicFollowsService {
  def insertTopicFollow(
      jwt: String,
      jwtType: String,
      topicId: TopicId
  ): ZIO[TopicFollowsService, AppError, Unit] =
    ZIO.serviceWithZIO[TopicFollowsService](
      _.insertTopicFollow(jwt, jwtType, topicId)
    )

  def deleteTopicFollow(
      jwt: String,
      jwtType: String,
      topicId: TopicId
  ): ZIO[TopicFollowsService, AppError, Unit] =
    ZIO.serviceWithZIO[TopicFollowsService](
      _.deleteTopicFollow(jwt, jwtType, topicId)
    )
}

case class TopicFollowsServiceLive(
    topicFollowsRep: TopicFollowsRepository,
    authenticationService: AuthenticationService
) extends TopicFollowsService {
  val kafka = new KafkaProducerServiceLive()

  override def insertTopicFollow(
      jwt: String,
      jwtType: String,
      topicId: TopicId
  ): ZIO[Any, AppError, Unit] = for {
    userData <- authenticationService.extractUserData(jwt, jwtType)
    _ <- topicFollowsRep.insertTopicFollow(
      TopicFollows(userId = userData.userId, followedTopicId = topicId.id)
    )
  } yield ()

  override def deleteTopicFollow(
      jwt: String,
      jwtType: String,
      topicId: TopicId
  ): ZIO[Any, AppError, Unit] = for {
    userData <- authenticationService.extractUserData(jwt, jwtType)
    _ <- topicFollowsRep.deleteTopicFollow(
      TopicFollows(userId = userData.userId, followedTopicId = topicId.id)
    )
  } yield ()
}

object TopicFollowsServiceLive {

  val layer: URLayer[
    TopicFollowsRepository with AuthenticationService,
    TopicFollowsService
  ] = ZLayer.fromFunction(TopicFollowsServiceLive.apply _)

}
