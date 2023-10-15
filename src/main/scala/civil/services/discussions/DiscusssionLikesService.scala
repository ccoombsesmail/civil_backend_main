package civil.services.discussions

import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.errors.AppError
import civil.models._
import civil.models.NotifcationEvents.{
  DiscussionLike,
  GivingUserNotificationData
}
import civil.models.actions.LikedState
import civil.repositories.discussions.DiscussionLikesRepository
import civil.services.{
  AlgorithmScoresCalculationService,
  AuthenticationService,
  KafkaProducerServiceLive
}
import zio.{URLayer, ZIO, ZLayer}

import javax.sql.DataSource

trait DiscussionLikesService {
  def addRemoveDiscussionLikeOrDislike(
      jwt: String,
      jwtType: String,
      discussionLikeDislikeData: UpdateDiscussionLikes
  ): ZIO[Any, AppError, DiscussionLiked]
}

object DiscussionLikesService {
  def addRemoveDiscussionLikeOrDislike(
      jwt: String,
      jwtType: String,
      discussionLikeDislikeData: UpdateDiscussionLikes
  ): ZIO[DiscussionLikesService, AppError, DiscussionLiked] =
    ZIO.serviceWithZIO[DiscussionLikesService](
      _.addRemoveDiscussionLikeOrDislike(
        jwt,
        jwtType,
        discussionLikeDislikeData
      )
    )

}

case class DiscussionLikesServiceLive(
    discussionLikesRep: DiscussionLikesRepository,
    authenticationService: AuthenticationService,
    algorithmScoresCalculationService: AlgorithmScoresCalculationService
) extends DiscussionLikesService {
  val kafka = new KafkaProducerServiceLive()

  override def addRemoveDiscussionLikeOrDislike(
      jwt: String,
      jwtType: String,
      discussionLikeDislikeData: UpdateDiscussionLikes
  ): ZIO[Any, AppError, DiscussionLiked] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      data <- discussionLikesRep
        .addRemoveDiscussionLikeOrDislike(
          discussionLikeDislikeData,
          userData.userId
        )
      (updatedLikeData, discussion) = data
      _ <- ZIO
        .when(updatedLikeData.likeState == LikedState)(
          kafka.publish(
            DiscussionLike(
              eventType = "DiscussionLike",
              spaceId = discussionLikeDislikeData.spaceId,
              discussionId = updatedLikeData.id,
              receivingUserId = discussion.createdByUserId,
              givingUserData = GivingUserNotificationData(
                givingUserId = userData.userId,
                givingUserUsername = userData.username,
                givingUserTag = Some(userData.userCivilTag),
                givingUserIconSrc = Some(userData.userIconSrc)
              )
            ),
            discussion.createdByUserId,
            DiscussionLike.discussionLikeSerde
          )
        )
        .mapError(InternalServerError)
        .forkDaemon
      _ <- algorithmScoresCalculationService.calculatePopularityScore(
        discussionLikeDislikeData.id
      )
    } yield updatedLikeData
  }

}

object DiscussionLikesServiceLive {

  val layer: URLayer[
    DiscussionLikesRepository
      with AuthenticationService
      with AlgorithmScoresCalculationService,
    DiscussionLikesService
  ] = ZLayer.fromFunction(DiscussionLikesServiceLive.apply _)

}
