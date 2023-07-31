package civil.services.discussions

import civil.errors.AppError
import civil.models._
import civil.models.enums.ReportStatus
import civil.repositories.discussions.DiscussionRepository
import civil.services.AuthenticationService
import io.scalaland.chimney.dsl._
import zio._

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

trait DiscussionService {
  def insertDiscussion(
      jwt: String,
      jwtType: String,
      incomingDiscussion: IncomingDiscussion
  ): ZIO[Any, AppError, Discussions]
  def getDiscussions(
      jwt: String,
      jwtType: String,
      spaceId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]
  def getDiscussion(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, AppError, OutgoingDiscussion]

  def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId]

  def getUserDiscussions(
      jwt: String,
      jwtType: String,
      userId: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getSimilarDiscussions(
      jwt: String,
      jwtType: String,
      discussionId: UUID
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getPopularDiscussions(
      jwt: String,
      jwtType: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getFollowedDiscussions(
      jwt: String,
      jwtType: String
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
      spaceId: UUID,
      skip: Int
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getDiscussions(jwt, jwtType, spaceId, skip)
    )

  def getDiscussion(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[DiscussionService, AppError, OutgoingDiscussion] =
    ZIO.serviceWithZIO[DiscussionService](_.getDiscussion(jwt, jwtType, id))

  def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[DiscussionService, AppError, GeneralDiscussionId] =
    ZIO.serviceWithZIO[DiscussionService](_.getGeneralDiscussionId(spaceId))

  def getUserDiscussions(
      jwt: String,
      jwtType: String,
      userId: String
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getUserDiscussions(jwt, jwtType, userId)
    )

  def getSimilarDiscussions(
      jwt: String,
      jwtType: String,
      discussionId: UUID
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getSimilarDiscussions(jwt, jwtType, discussionId)
    )

  def getPopularDiscussions(
      jwt: String,
      jwtType: String,
      skip: Int
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getPopularDiscussions(jwt, jwtType, skip)
    )

  def getFollowedDiscussions(
      jwt: String,
      jwtType: String
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getFollowedDiscussions(jwt, jwtType)
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
          .withFieldConst(_.reportStatus, ReportStatus.CLEAN.entryName)
          .withFieldConst(_.popularityScore, 0.0)
          .withFieldConst(
            _.createdAt,
            ZonedDateTime.now(ZoneId.systemDefault())
          )
          .withFieldConst(_.createdByUsername, userData.username)
          .withFieldConst(_.createdByUserId, userData.userId)
          .withFieldConst(_.id, uuid)
          .withFieldConst(
            _.spaceId,
            UUID.fromString(incomingDiscussion.spaceId)
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
      spaceId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      discussions <- discussionRepository.getDiscussions(
        spaceId,
        skip,
        userData.userId
      )
    } yield discussions
  }

  override def getDiscussion(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, AppError, OutgoingDiscussion] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      discussions <- discussionRepository.getDiscussion(id, userData.userId)
    } yield discussions

  }

  override def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId] = {
    discussionRepository.getGeneralDiscussionId(spaceId)
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

  override def getSimilarDiscussions(
      jwt: String,
      jwtType: String,
      discussionId: UUID
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      discussions <- discussionRepository.getSimilarDiscussions(
        discussionId
      )
    } yield discussions
  }

  override def getPopularDiscussions(
      jwt: String,
      jwtType: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = for {
    userData <- authService.extractUserData(jwt, jwtType)
    discussions <- discussionRepository.getPopularDiscussions(
      userData.userId,
      skip
    )
  } yield discussions

  override def getFollowedDiscussions(
      jwt: String,
      jwtType: String
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      discussions <- discussionRepository.getFollowedDiscussions(
        userData.userId
      )
    } yield discussions
  }
}

object DiscussionServiceLive {
  val layer: URLayer[
    DiscussionRepository with AuthenticationService,
    DiscussionService
  ] = ZLayer.fromFunction(DiscussionServiceLive.apply _)
}
