package civil.services

import civil.errors.AppError
import civil.models.{
  Discussions,
  ExternalLinksDiscussions,
  GeneralDiscussionId,
  IncomingDiscussion,
  OutgoingDiscussion
}

import java.util.UUID
import civil.repositories.topics.DiscussionRepository
import io.scalaland.chimney.dsl._

import java.time.{ZoneId, ZonedDateTime}
import zio._

trait DiscussionService {
  def insertDiscussion(
      jwt: String,
      jwtType: String,
      incomingDiscussion: IncomingDiscussion
  ): ZIO[Any, AppError, Discussions]
  def getDiscussions(
      jwt: String,
      jwtType: String,
      topicId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]
  def getDiscussion(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, AppError, OutgoingDiscussion]

  def getGeneralDiscussionId(
      topicId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId]

  def getUserDiscussions(
      jwt: String,
      jwtType: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

}

object DiscussionService {
  def insertDiscussion(
      jwt: String,
      jwtType: String,
      incomingDiscussion: IncomingDiscussion
  ): ZIO[DiscussionService, AppError, Discussions] =
    ZIO.serviceWithZIO[DiscussionService](
      _.insertDiscussion(jwt, jwtType, incomingDiscussion)
    )

  def getDiscussions(
      jwt: String,
      jwtType: String,
      topicId: UUID,
      skip: Int
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getDiscussions(jwt, jwtType, topicId, skip)
    )

  def getDiscussion(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[DiscussionService, AppError, OutgoingDiscussion] =
    ZIO.serviceWithZIO[DiscussionService](_.getDiscussion(jwt, jwtType, id))

  def getGeneralDiscussionId(
      topicId: UUID
  ): ZIO[DiscussionService, AppError, GeneralDiscussionId] =
    ZIO.serviceWithZIO[DiscussionService](_.getGeneralDiscussionId(topicId))

  def getUserDiscussions(
      jwt: String,
      jwtType: String,
      userId: String
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getUserDiscussions(jwt, jwtType, userId)
    )
}

case class DiscussionServiceLive(
    discussionRepository: DiscussionRepository,
    authService: AuthenticationService
) extends DiscussionService {
  override def insertDiscussion(
      jwt: String,
      jwtType: String,
      incomingDiscussion: IncomingDiscussion
  ): ZIO[Any, AppError, Discussions] = {

    for {
      userData <- authService.extractUserData(jwt, jwtType)
      uuid = UUID.randomUUID()
      discussion <- discussionRepository.insertDiscussion(
        incomingDiscussion
          .into[Discussions]
          .withFieldConst(_.likes, 0)
          .withFieldConst(
            _.createdAt,
            ZonedDateTime.now(ZoneId.systemDefault())
          )
          .withFieldConst(_.createdByUsername, userData.username)
          .withFieldConst(_.createdByUserId, userData.userId)
          .withFieldConst(_.id, uuid)
          .withFieldConst(
            _.topicId,
            UUID.fromString(incomingDiscussion.topicId)
          )
          .transform,
        incomingDiscussion.externalContentData.map(
          _.into[ExternalLinksDiscussions]
            .withFieldConst(_.discussionId, uuid)
            .withFieldComputed(
              _.externalContentUrl,
              data => data.externalContentUrl
            )
            .withFieldComputed(_.thumbImgUrl, data => data.thumbImgUrl)
            .withFieldComputed(_.embedId, data => data.embedId)
            .transform
        )
      )
    } yield discussion
  }

  override def getDiscussions(
      jwt: String,
      jwtType: String,
      topicId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
      _ <- authService.extractUserData(jwt, jwtType)
      discussions <- discussionRepository.getDiscussions(topicId, skip)
    } yield discussions
  }

  override def getDiscussion(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, AppError, OutgoingDiscussion] = {
    for {
      _ <- authService.extractUserData(jwt, jwtType)
      discussions <- discussionRepository.getDiscussion(id)
    } yield discussions

  }

  override def getGeneralDiscussionId(
      topicId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId] = {
    discussionRepository.getGeneralDiscussionId(topicId)
  }

  override def getUserDiscussions(
      jwt: String,
      jwtType: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      userDiscussions <- discussionRepository.getUserDiscussions(
        userData.userId,
        userId
      )
    } yield userDiscussions
  }
}

object DiscussionServiceLive {
  val layer: URLayer[
    DiscussionRepository with AuthenticationService,
    DiscussionService
  ] = ZLayer.fromFunction(DiscussionServiceLive.apply _)
}
