package civil.services.comments

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.NotifcationEvents.{
  CommentCivilityGiven,
  GivingUserNotificationData
}
import civil.models._
import civil.repositories.comments.CommentCivilityRepository
import civil.services.{AuthenticationService, KafkaProducerServiceLive}
import zio._

trait CommentCivilityService {
  def addOrRemoveCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: UpdateCommentCivility
  ): ZIO[Any, AppError, CivilityGivenResponse]
  def addOrRemoveTribunalCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: UpdateCommentCivility
  ): ZIO[Any, AppError, CivilityGivenResponse]

}

object CommentCivilityService {
  def addOrRemoveCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: UpdateCommentCivility
  ): ZIO[CommentCivilityService, AppError, CivilityGivenResponse] =
    ZIO.serviceWithZIO[CommentCivilityService](
      _.addOrRemoveCommentCivility(
        jwt,
        jwtType,
        civilityData
      )
    )
  def addOrRemoveTribunalCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: UpdateCommentCivility
  ): ZIO[CommentCivilityService, AppError, CivilityGivenResponse] =
    ZIO.serviceWithZIO[CommentCivilityService](
      _.addOrRemoveTribunalCommentCivility(
        jwt,
        jwtType,
        civilityData
      )
    )
}

case class CommentCivilityServiceLive(
    commentCivilityRepo: CommentCivilityRepository,
    authenticationService: AuthenticationService
) extends CommentCivilityService {
  val kafka = new KafkaProducerServiceLive()

  override def addOrRemoveCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: UpdateCommentCivility
  ): ZIO[Any, AppError, CivilityGivenResponse] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- authenticationService.canPerformCaptchaRequiredAction(userData)
      res <- commentCivilityRepo
        .addOrRemoveCommentCivility(
          givingUserId = userData.userId,
          givingUserUsername = userData.username,
          civilityData
        )
        .mapError(InternalServerError)
      (civilityGiven, comment) = res
      _ <- kafka
        .publish(
          CommentCivilityGiven(
            eventType = "CommentCivilityGiven",
            value = civilityData.value,
            commentId = civilityData.commentId,
            receivingUserId = civilityData.receivingUserId,
            givingUserData = GivingUserNotificationData(
              givingUserId = userData.userId,
              givingUserUsername = userData.username,
              givingUserTag = Some(userData.userCivilTag),
              givingUserIconSrc = Some(userData.userIconSrc)
            ),
            spaceId = comment.spaceId,
            subtopicId = comment.discussionId
          ),
          civilityData.receivingUserId,
          CommentCivilityGiven.commentCivilityGivenSerde
        )
        .forkDaemon
    } yield civilityGiven

  }

  override def addOrRemoveTribunalCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: UpdateCommentCivility
  ): ZIO[Any, AppError, CivilityGivenResponse] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- authenticationService.canPerformCaptchaRequiredAction(userData)
      res <- commentCivilityRepo
        .addOrRemoveTribunalCommentCivility(
          givingUserId = userData.userId,
          givingUserUsername = userData.username,
          civilityData
        )
        .mapError(InternalServerError)
    } yield res
  }

}

object CommentCivilityServiceLive {

  val layer: URLayer[
    CommentCivilityRepository with AuthenticationService,
    CommentCivilityService
  ] = ZLayer.fromFunction(CommentCivilityServiceLive.apply _)

}
