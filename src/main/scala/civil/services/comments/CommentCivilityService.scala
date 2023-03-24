package civil.services.comments

import civil.models.NotifcationEvents.{
  CommentCivilityGiven,
  GivingUserNotificationData
}
import civil.models.{Civility, CivilityGiven, ErrorInfo, InternalServerError}
import civil.models._
import civil.repositories.comments.CommentCivilityRepository
import civil.services.{AuthenticationServiceLive, KafkaProducerServiceLive}
import zio._
// import civil.directives.SentimentAnalyzer

trait CommentCivilityService {
  def addOrRemoveCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven]
  def addOrRemoveTribunalCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven]

}

object CommentCivilityService {
  def addOrRemoveCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Has[CommentCivilityService], ErrorInfo, CivilityGiven] =
    ZIO.serviceWith[CommentCivilityService](
      _.addOrRemoveCommentCivility(
        jwt,
        jwtType,
        civilityData
      )
    )
  def addOrRemoveTribunalCommentCivility(
      jwt: String,
      jwtType: String,
      civilityData: Civility
  ): ZIO[Has[CommentCivilityService], ErrorInfo, CivilityGiven] =
    ZIO.serviceWith[CommentCivilityService](
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
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- authenticationService.canPerformCaptchaRequiredAction(userData)
      res <- commentCivilityRepo
        .addOrRemoveCommentCivility(
          givingUserId = userData.userId,
          givingUserUsername = userData.username,
          civilityData
        )
        .mapError(e => InternalServerError(e.toString))
      (civilityGiven, comment) = res
      _ <- ZIO
        .effect(
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
      civilityData: Civility
  ): ZIO[Any, ErrorInfo, CivilityGiven] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- authenticationService.canPerformCaptchaRequiredAction(userData)
      res <- commentCivilityRepo
        .addOrRemoveTribunalCommentCivility(
          givingUserId = userData.userId,
          givingUserUsername = userData.username,
          civilityData: Civility
        )
        .mapError(e => InternalServerError(e.toString))
    } yield res
  }

}

object CommentCivilityServiceLive {
  val live: ZLayer[Has[CommentCivilityRepository], Throwable, Has[
    CommentCivilityService
  ]] = {
    for {
      commentCivilityRepo <- ZIO.service[CommentCivilityRepository]
    } yield CommentCivilityServiceLive(commentCivilityRepo)
  }.toLayer
}
