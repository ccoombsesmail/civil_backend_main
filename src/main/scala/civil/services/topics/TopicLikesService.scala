package civil.services.topics

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
import civil.models.{TopicLiked, UpdateTopicLikes}
import civil.models.NotifcationEvents.{GivingUserNotificationData, TopicLike}
import civil.repositories.topics.TopicLikesRepository
import civil.services.{AuthenticationService, AuthenticationServiceLive, KafkaProducerServiceLive}
import zio.{URLayer, ZIO, ZLayer}

import javax.sql.DataSource

trait TopicLikesService {
  def addRemoveTopicLikeOrDislike(
      jwt: String,
      jwtType: String,
      topicLikeDislikeData: UpdateTopicLikes
  ): ZIO[Any, AppError, TopicLiked]
}

object TopicLikesService {
  def addRemoveTopicLikeOrDislike(
      jwt: String,
      jwtType: String,
      topicLikeDislikeData: UpdateTopicLikes
  ): ZIO[TopicLikesService, AppError, TopicLiked] =
    ZIO.serviceWithZIO[TopicLikesService](
      _.addRemoveTopicLikeOrDislike(jwt, jwtType, topicLikeDislikeData)
    )

}

case class TopicLikesServiceLive(topicLikesRep: TopicLikesRepository, authenticationService: AuthenticationService)
    extends TopicLikesService {
  val kafka = new KafkaProducerServiceLive()

  override def addRemoveTopicLikeOrDislike(
      jwt: String,
      jwtType: String,
      topicLikeDislikeData: UpdateTopicLikes
  ): ZIO[Any, AppError, TopicLiked] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType).mapError(e => InternalServerError(e.toString))
      data <- topicLikesRep
        .addRemoveTopicLikeOrDislike(
          topicLikeDislikeData, userData.userId
        ).mapError(e => InternalServerError(e.toString))
      (updatedLikeData, topic) = data
      _ <- ZIO
        .when(updatedLikeData.likeState == 1)(
              kafka.publish(
                TopicLike(
                  eventType = "TopicLike",
                  topicId = updatedLikeData.id,
                  receivingUserId = topic.createdByUserId,
                  givingUserData = GivingUserNotificationData(
                    givingUserId = userData.userId,
                    givingUserUsername = userData.username,
                    givingUserTag = Some(userData.userCivilTag),
                    givingUserIconSrc = Some(userData.userIconSrc)
                  )
                ),
                topic.createdByUserId,
                TopicLike.topicLikeSerde
              )

        )
        .mapError(e => {
          println(e)
          InternalServerError(e.toString)
        }).forkDaemon
    } yield updatedLikeData
  }

}

object TopicLikesServiceLive {

  val layer: URLayer[TopicLikesRepository with AuthenticationService, TopicLikesService] = ZLayer.fromFunction(TopicLikesServiceLive.apply _)

}
