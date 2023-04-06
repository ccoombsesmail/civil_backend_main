package civil.services

import civil.models.{Discussions, ErrorInfo, ExternalLinksDiscussions, GeneralDiscussionId, IncomingDiscussion, OutgoingDiscussion}
import civil.directives.MetaData
import civil.directives.OutgoingHttp.getTweetInfo

import java.util.UUID
import civil.repositories.topics.DiscussionRepository

import scala.concurrent.duration._
import io.scalaland.chimney.dsl._

import java.time.LocalDateTime
import zio._

import scala.concurrent.Await
import scala.util.Try

trait DiscussionService {
  def insertDiscussion(
      jwt: String,
      jwtType: String,
      incomingDiscussion: IncomingDiscussion,
): ZIO[Any, ErrorInfo, Discussions]
  def getDiscussions(
      topicId: UUID,
      skip: Int
                    ): ZIO[Any, ErrorInfo, List[OutgoingDiscussion]]
  def getDiscussion(id: UUID): ZIO[Any, ErrorInfo, OutgoingDiscussion]

  def getGeneralDiscussionId(topicId: UUID): ZIO[Any, ErrorInfo, GeneralDiscussionId]

  def getUserDiscussions(
                     jwt: String,
                     jwtType: String,
                     userId: String
                   ): ZIO[Any, ErrorInfo, List[OutgoingDiscussion]]

}

object DiscussionService {
  def insertDiscussion(
      jwt: String,
      jwtType: String,
      incomingDiscussion: IncomingDiscussion
  ): ZIO[Has[DiscussionService], ErrorInfo, Discussions] =
    ZIO.serviceWith[DiscussionService](
      _.insertDiscussion(jwt, jwtType, incomingDiscussion)
    )

  def getDiscussions(
      topicId: UUID,
      skip: Int
  ): ZIO[Has[DiscussionService], ErrorInfo, List[OutgoingDiscussion]] =
    ZIO.serviceWith[DiscussionService](_.getDiscussions(topicId, skip))

  def getDiscussion(
      id: UUID
  ): ZIO[Has[DiscussionService], ErrorInfo, OutgoingDiscussion] =
    ZIO.serviceWith[DiscussionService](_.getDiscussion(id))

  def getGeneralDiscussionId(
                              topicId: UUID
                            ): ZIO[Has[DiscussionService], ErrorInfo, GeneralDiscussionId] =
    ZIO.serviceWith[DiscussionService](_.getGeneralDiscussionId(topicId))

  def getUserDiscussions(
                     jwt: String,
                     jwtType: String,
                     userId: String
                   ): ZIO[Has[DiscussionService], ErrorInfo, List[OutgoingDiscussion]] =
    ZIO.serviceWith[DiscussionService](_.getUserDiscussions(jwt, jwtType, userId))
}

case class DiscussionServiceLive(discussionRepository: DiscussionRepository, authService: AuthenticationService)
    extends DiscussionService {
  val authenticationService = AuthenticationServiceLive()

  override def insertDiscussion(
      jwt: String,
      jwtType: String,
      incomingDiscussion: IncomingDiscussion
  ): ZIO[Any, ErrorInfo, Discussions] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      uuid = UUID.randomUUID()
      discussion <- discussionRepository.insertDiscussion(
        incomingDiscussion
          .into[Discussions]
          .withFieldConst(_.likes, 0)
          .withFieldConst(_.createdAt, LocalDateTime.now())
          .withFieldConst(_.createdByUsername, userData.username)
          .withFieldConst(_.createdByUserId, userData.userId)
          .withFieldConst(_.id, uuid)
          .withFieldConst(
            _.topicId,
            UUID.fromString(incomingDiscussion.topicId)
          )
          .transform,
        incomingDiscussion.externalContentData.map(_.into[ExternalLinksDiscussions]
          .withFieldConst(_.discussionId, uuid)
          .withFieldComputed(_.externalContentUrl, data => data.externalContentUrl)
          .withFieldComputed(_.thumbImgUrl, data => data.thumbImgUrl)
          .withFieldComputed(_.embedId, data => data.embedId)
          .transform
        )
      )
    } yield discussion
  }

  override def getDiscussions(
      topicId: UUID,
      skip: Int

  ): ZIO[Any, ErrorInfo, List[OutgoingDiscussion]] = {
    discussionRepository.getDiscussions(topicId, skip)
  }

  override def getDiscussion(
      id: UUID
  ): ZIO[Any, ErrorInfo, OutgoingDiscussion] = {
    discussionRepository.getDiscussion(id)
  }

  override def getGeneralDiscussionId(topicId: UUID): ZIO[Any, ErrorInfo, GeneralDiscussionId] = {
    discussionRepository.getGeneralDiscussionId(topicId)
  }

  override def getUserDiscussions(jwt: String, jwtType: String, userId: String): ZIO[Any, ErrorInfo, List[OutgoingDiscussion]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      topics <- discussionRepository.getUserDiscussions(
        userData.userId,
        userId
      )
    } yield topics
  }
}

object DiscussionServiceLive {
  val live
      : ZLayer[Has[DiscussionRepository] with Has[AuthenticationService], Throwable, Has[DiscussionService]] = {
    for {
      discussionRepo <- ZIO.service[DiscussionRepository]
      authService <- ZIO.service[AuthenticationService]
    } yield DiscussionServiceLive(discussionRepo, authService)
  }.toLayer
}
