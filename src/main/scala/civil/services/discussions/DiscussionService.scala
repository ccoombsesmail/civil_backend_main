package civil.services.discussions

import civil.directives.OutgoingHttp.Permissions
import civil.errors.AppError
import civil.models._
import civil.models.enums.ReportStatus
import civil.models.enums.UserVerificationType._
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

  def getSpaceDiscussions(
      jwt: String,
      jwtType: String,
      spaceId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getSpaceDiscussionsUnauthenticated(
      spaceId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getDiscussion(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, AppError, OutgoingDiscussion]

  def getDiscussionUnauthenticated(
      id: UUID
  ): ZIO[Any, AppError, OutgoingDiscussion]

  def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId]

  def getUserDiscussions(
      jwt: String,
      jwtType: String,
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getUserDiscussionsUnauthenticated(
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getSimilarDiscussions(
      discussionId: UUID
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getPopularDiscussions(
      jwt: String,
      jwtType: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getPopularDiscussionsUnauthenticated(
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]]

  def getFollowedDiscussions(
      jwt: String,
      jwtType: String,
      skip: Int
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

  def getSpaceDiscussions(
      jwt: String,
      jwtType: String,
      spaceId: UUID,
      skip: Int
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getSpaceDiscussions(jwt, jwtType, spaceId, skip)
    )

  def getSpaceDiscussionsUnauthenticated(
      spaceId: UUID,
      skip: Int
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getSpaceDiscussionsUnauthenticated(spaceId, skip)
    )

  def getDiscussion(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[DiscussionService, AppError, OutgoingDiscussion] =
    ZIO.serviceWithZIO[DiscussionService](_.getDiscussion(jwt, jwtType, id))

  def getDiscussionUnauthenticated(
      id: UUID
  ): ZIO[DiscussionService, AppError, OutgoingDiscussion] =
    ZIO.serviceWithZIO[DiscussionService](_.getDiscussionUnauthenticated(id))

  def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[DiscussionService, AppError, GeneralDiscussionId] =
    ZIO.serviceWithZIO[DiscussionService](_.getGeneralDiscussionId(spaceId))

  def getUserDiscussions(
      jwt: String,
      jwtType: String,
      userId: String,
      skip: Int
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getUserDiscussions(jwt, jwtType, userId, skip)
    )

  def getUserDiscussionsUnauthenticated(
      jwt: String,
      jwtType: String,
      userId: String,
      skip: Int
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getUserDiscussionsUnauthenticated(userId, skip)
    )

  def getSimilarDiscussions(
      discussionId: UUID
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getSimilarDiscussions(discussionId)
    )

  def getPopularDiscussionsUnauthenticated(
      skip: Int
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getPopularDiscussionsUnauthenticated(skip)
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
      jwtType: String,
      skip: Int
  ): ZIO[DiscussionService, AppError, List[OutgoingDiscussion]] =
    ZIO.serviceWithZIO[DiscussionService](
      _.getFollowedDiscussions(jwt, jwtType, skip)
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

  override def getSpaceDiscussions(
      jwt: String,
      jwtType: String,
      spaceId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      discussions <- discussionRepository.getSpaceDiscussions(
        spaceId,
        skip,
        userData.userId
      )
    } yield discussions
  }

  override def getSpaceDiscussionsUnauthenticated(
      spaceId: UUID,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    discussionRepository.getSpaceDiscussionsUnauthenticated(
      spaceId,
      skip
    )
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

  override def getDiscussionUnauthenticated(
      id: UUID
  ): ZIO[Any, AppError, OutgoingDiscussion] =
    discussionRepository.getDiscussionUnauthenticated(id)

  override def getGeneralDiscussionId(
      spaceId: UUID
  ): ZIO[Any, AppError, GeneralDiscussionId] = {
    discussionRepository.getGeneralDiscussionId(spaceId)
  }

  override def getUserDiscussions(
      jwt: String,
      jwtType: String,
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      userDiscussions <- discussionRepository.getUserDiscussions(
        userData.userId,
        userId,
        skip
      )
    } yield userDiscussions
  }

  override def getUserDiscussionsUnauthenticated(
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    discussionRepository.getUserDiscussionsUnauthenticated(
      userId,
      skip
    )

  }

  override def getSimilarDiscussions(
      discussionId: UUID
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
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

  override def getPopularDiscussionsUnauthenticated(
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    discussionRepository.getPopularDiscussionsUnauthenticated(
      skip
    )
  }

  override def getFollowedDiscussions(
      jwt: String,
      jwtType: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingDiscussion]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      discussions <- discussionRepository.getFollowedDiscussions(
        userData.userId,
        skip
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
