package civil.services.topics

import civil.models.{
  ErrorInfo,
  InternalServerError,
  TopicLiked,
  UpdateTopicLikes
}
import civil.models.NotifcationEvents.{GivingUserNotificationData, TopicLike}
import civil.repositories.topics.TopicLikesRepository
import civil.services.{AuthenticationServiceLive, KafkaProducerServiceLive}
import zio.{Has, ZIO, ZLayer}

trait TopicLikesService {
  def addRemoveTopicLikeOrDislike(
      jwt: String,
      jwtType: String,
      topicLikeDislikeData: UpdateTopicLikes
  ): ZIO[Any, ErrorInfo, TopicLiked]
}

object TopicLikesService {
  def addRemoveTopicLikeOrDislike(
      jwt: String,
      jwtType: String,
      topicLikeDislikeData: UpdateTopicLikes
  ): ZIO[Has[TopicLikesService], ErrorInfo, TopicLiked] =
    ZIO.serviceWith[TopicLikesService](
      _.addRemoveTopicLikeOrDislike(jwt, jwtType, topicLikeDislikeData)
    )

}

case class TopicLikesServiceLive(topicLikesRep: TopicLikesRepository)
    extends TopicLikesService {
  val kafka = new KafkaProducerServiceLive()

  override def addRemoveTopicLikeOrDislike(
      jwt: String,
      jwtType: String,
      topicLikeDislikeData: UpdateTopicLikes
  ): ZIO[Any, ErrorInfo, TopicLiked] = {
    val authenticationService = AuthenticationServiceLive()
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      data <- topicLikesRep
        .addRemoveTopicLikeOrDislike(
          topicLikeDislikeData, userData.userId
        )
      (updatedLikeData, topic) = data
      _ <- ZIO
        .when(updatedLikeData.likeState == 1)(
          ZIO
            .effect(
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
        )
        .mapError(e => {
          InternalServerError(e.toString)
        })
    } yield updatedLikeData
  }

}

object TopicLikesServiceLive {
  val live: ZLayer[Has[TopicLikesRepository], Nothing, Has[
    TopicLikesService
  ]] = {
    for {
      topicLikesRepo <- ZIO.service[TopicLikesRepository]
    } yield TopicLikesServiceLive(topicLikesRepo)
  }.toLayer
}
