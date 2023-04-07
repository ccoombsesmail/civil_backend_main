package civil.services.comments

import civil.errors.AppError
import civil.errors.AppError.GeneralError
import civil.models.NotifcationEvents.{CommentCivilityGiven, GivingUserNotificationData}
import civil.models.{AppError, UpdateCommentCivility}
import civil.models._
import civil.repositories.comments.CommentCivilityRepository
import civil.services.{AuthenticationServiceLive, KafkaProducerServiceLive}
import zio._
// import civil.directives.SentimentAnalyzer

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
    commentCivilityRepo: CommentCivilityRepository
) extends CommentCivilityService {
  val authenticationService = AuthenticationServiceLive()
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
        .mapError(e => GeneralError(e.toString))
      (civilityGiven, comment) = res
      _ <- ZIO
        .attempt(
          kafka.publish(
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
              topicId = comment.topicId,
              subtopicId = comment.discussionId
            ),
            civilityData.receivingUserId,
            CommentCivilityGiven.commentCivilityGivenSerde
          )
        )
        .fork
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
          civilityData: UpdateCommentCivility
        )
        .mapError(e => GeneralError(e.toString))
    } yield res
  }

}

object CommentCivilityServiceLive {

  val layer: URLayer[CommentCivilityRepository, CommentCivilityService] = ZLayer.fromFunction(CommentCivilityServiceLive.apply _)

}
