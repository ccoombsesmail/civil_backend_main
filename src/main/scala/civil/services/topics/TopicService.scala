package civil.services.topics

import civil.errors.AppError
import civil.models.{ExternalLinks, IncomingTopic, OutgoingTopic, Topics}
import civil.directives.OutgoingHttp._
import civil.models.enums.UserVerificationType.{CAPTCHA_VERIFIED, FACE_ID_AND_CAPTCHA_VERIFIED, FACE_ID_VERIFIED, NO_VERIFICATION}
import civil.repositories.PollsRepository
import civil.repositories.topics.TopicRepository
import civil.services.AuthenticationService
import io.scalaland.chimney.dsl._
import zio._

import java.time.LocalDateTime
import java.util.UUID
import scala.language.postfixOps

trait TopicService {
  def insertTopic(
      jwt: String,
      jwtType: String,
      incomingTopic: IncomingTopic
  ): ZIO[Any, AppError, OutgoingTopic]
  def getTopics: ZIO[Any, AppError, List[OutgoingTopic]]

  def getTopicsAuthenticated(
      jwt: String,
      jwtType: String,
      offset: Int
  ): ZIO[Any, AppError, List[OutgoingTopic]]
  def getTopic(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, AppError, OutgoingTopic]

  def getUserTopics(
                jwt: String,
                jwtType: String,
                userId: String
              ): ZIO[Any, AppError, List[OutgoingTopic]]
}

object TopicService {
  def insertTopic(
      jwt: String,
      jwtType: String,
      incomingTopic: IncomingTopic
  ): ZIO[TopicService, AppError, OutgoingTopic] =
    ZIO.serviceWithZIO[TopicService](_.insertTopic(jwt, jwtType, incomingTopic))

  def getTopics(): ZIO[TopicService, AppError, List[OutgoingTopic]] =
    ZIO.serviceWithZIO[TopicService](_.getTopics)

  def getTopicsAuthenticated(
      jwt: String,
      jwtType: String,
      offset: Int
  ): ZIO[TopicService, AppError, List[OutgoingTopic]] =
    ZIO.serviceWithZIO[TopicService](_.getTopicsAuthenticated(jwt, jwtType, offset))

  def getTopic(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[TopicService, AppError, OutgoingTopic] =
    ZIO.serviceWithZIO[TopicService](_.getTopic(jwt, jwtType, id))

  def getUserTopics(
                jwt: String,
                jwtType: String,
                userId: String
              ): ZIO[TopicService, AppError, List[OutgoingTopic]] =
    ZIO.serviceWithZIO[TopicService](_.getUserTopics(jwt, jwtType, userId))
}

case class TopicServiceLive(topicRepository: TopicRepository, pollsRepository: PollsRepository, authService: AuthenticationService)
    extends TopicService {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  override def insertTopic(
      jwt: String,
      jwtType: String,
      incomingTopic: IncomingTopic
  ): ZIO[Any, AppError, OutgoingTopic] = {

    for {
      userData <- authService.extractUserData(jwt, jwtType)
      topicId = UUID.randomUUID()
      insertedTopic <- topicRepository.insertTopic(
        incomingTopic
          .into[Topics]
          .withFieldConst(_.id, topicId)
          .withFieldConst(_.likes, 0)
          .withFieldConst(_.createdAt, LocalDateTime.now())
          .withFieldConst(_.updatedAt, LocalDateTime.now())
          .withFieldConst(_.id, UUID.randomUUID())
          .withFieldConst(_.createdByUserId, userData.userId)
          .withFieldConst(_.createdByUsername, userData.username)
          .withFieldConst(
            _.userVerificationType,
            userData.permissions match {
              case Permissions(false, false) => NO_VERIFICATION
              case Permissions(false, true)  => CAPTCHA_VERIFIED
              case Permissions(true, false)  => FACE_ID_VERIFIED
              case Permissions(true, true)   => FACE_ID_AND_CAPTCHA_VERIFIED
            }
          )
          .transform,
        incomingTopic.externalContentData.map(_.into[ExternalLinks]
          .withFieldConst(_.topicId, topicId)
          .withFieldComputed(_.externalContentUrl, data => data.externalContentUrl)
          .withFieldComputed(_.thumbImgUrl, data => data.thumbImgUrl)
          .withFieldComputed(_.embedId, data => data.embedId)
          .transform
        )
      )
    } yield insertedTopic
  }

  override def getTopics: ZIO[Any, AppError, List[OutgoingTopic]] = {
    topicRepository.getTopics
  }

  override def getTopicsAuthenticated(
      jwt: String,
      jwtType: String,
      offset: Int
  ): ZIO[Any, AppError, List[OutgoingTopic]] = {

    for {
      userData <- authService.extractUserData(jwt, jwtType)
      topics <- topicRepository.getTopicsAuthenticated(
        userData.userId,
        userData,
        offset
      )
    } yield topics
  }

  override def getTopic(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, AppError, OutgoingTopic] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      topic <- topicRepository
        .getTopic(id, userData.userId)
        .tapError(e => {
          println(e)
          ZIO.fail(e)
        })
    } yield topic
  }

  override def getUserTopics(jwt: String, jwtType: String, userId: String): ZIO[Any, AppError, List[OutgoingTopic]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      topics <- topicRepository.getUserTopics(
        userData.userId,
        userId
      )
    } yield topics
  }

}

object TopicServiceLive {

  val layer: URLayer[
    TopicRepository with PollsRepository with AuthenticationService,
    TopicService
  ] = ZLayer.fromFunction(TopicServiceLive.apply _)
}
