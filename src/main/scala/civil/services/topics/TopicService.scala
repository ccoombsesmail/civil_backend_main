package civil.services.topics

import civil.models.{ErrorInfo, OutgoingTopicsPayload, ExternalLinks, IncomingTopic, OutgoingTopic, Topics}
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
  ): ZIO[Any, ErrorInfo, OutgoingTopic]
  def getTopics: ZIO[Any, ErrorInfo, List[OutgoingTopic]]

  def getTopicsAuthenticated(
      jwt: String,
      jwtType: String,
      offset: Int
  ): ZIO[Any, ErrorInfo, List[OutgoingTopic]]
  def getTopic(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, ErrorInfo, OutgoingTopic]

  def getUserTopics(
                jwt: String,
                jwtType: String,
                userId: String
              ): ZIO[Any, ErrorInfo, List[OutgoingTopic]]
}

object TopicService {
  def insertTopic(
      jwt: String,
      jwtType: String,
      incomingTopic: IncomingTopic
  ): ZIO[Has[TopicService], ErrorInfo, OutgoingTopic] =
    ZIO.serviceWith[TopicService](_.insertTopic(jwt, jwtType, incomingTopic))

  def getTopics(): ZIO[Has[TopicService], ErrorInfo, List[OutgoingTopic]] =
    ZIO.serviceWith[TopicService](_.getTopics)

  def getTopicsAuthenticated(
      jwt: String,
      jwtType: String,
      offset: Int
  ): ZIO[Has[TopicService], ErrorInfo, List[OutgoingTopic]] =
    ZIO.serviceWith[TopicService](_.getTopicsAuthenticated(jwt, jwtType, offset))

  def getTopic(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Has[TopicService], ErrorInfo, OutgoingTopic] =
    ZIO.serviceWith[TopicService](_.getTopic(jwt, jwtType, id))

  def getUserTopics(
                jwt: String,
                jwtType: String,
                userId: String
              ): ZIO[Has[TopicService], ErrorInfo, List[OutgoingTopic]] =
    ZIO.serviceWith[TopicService](_.getUserTopics(jwt, jwtType, userId))
}

case class TopicServiceLive(topicRepository: TopicRepository, pollsRepository: PollsRepository, authService: AuthenticationService)
    extends TopicService {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  override def insertTopic(
      jwt: String,
      jwtType: String,
      incomingTopic: IncomingTopic
  ): ZIO[Any, ErrorInfo, OutgoingTopic] = {

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

  override def getTopics: ZIO[Any, ErrorInfo, List[OutgoingTopic]] = {
    topicRepository.getTopics
  }

  override def getTopicsAuthenticated(
      jwt: String,
      jwtType: String,
      offset: Int
  ): ZIO[Any, ErrorInfo, List[OutgoingTopic]] = {

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
  ): ZIO[Any, ErrorInfo, OutgoingTopic] = {
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

  override def getUserTopics(jwt: String, jwtType: String, userId: String): ZIO[Any, ErrorInfo, List[OutgoingTopic]] = {
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
  val live: ZLayer[Has[TopicRepository] with Has[PollsRepository] with Has[AuthenticationService], Nothing, Has[TopicService]] = {
    for {
      topicRepo <- ZIO.service[TopicRepository]
      pollsRepo <- ZIO.service[PollsRepository]
      authService <- ZIO.service[AuthenticationService]
    } yield TopicServiceLive(topicRepo, pollsRepo, authService)
  }.toLayer
}
