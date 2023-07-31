package civil.services.discussions

import civil.errors.AppError
import civil.models.{DiscussionFollows, DiscussionId, UpdateDiscussionFollows}
import civil.repositories.discussions.DiscussionFollowsRepository
import civil.services.{AuthenticationService, KafkaProducerServiceLive}
import zio.{URLayer, ZIO, ZLayer}

trait DiscussionFollowsService {
  def insertDiscussionFollow(
      jwt: String,
      jwtType: String,
      updateDiscussionFollows: UpdateDiscussionFollows
  ): ZIO[Any, AppError, Unit]

  def deleteDiscussionFollow(
      jwt: String,
      jwtType: String,
      topicId: DiscussionId
  ): ZIO[Any, AppError, Unit]

}

object DiscussionFollowsService {
  def insertDiscussionFollow(
      jwt: String,
      jwtType: String,
      updateDiscussionFollows: UpdateDiscussionFollows
  ): ZIO[DiscussionFollowsService, AppError, Unit] =
    ZIO.serviceWithZIO[DiscussionFollowsService](
      _.insertDiscussionFollow(jwt, jwtType, updateDiscussionFollows)
    )

  def deleteDiscussionFollow(
      jwt: String,
      jwtType: String,
      discussionId: DiscussionId
  ): ZIO[DiscussionFollowsService, AppError, Unit] =
    ZIO.serviceWithZIO[DiscussionFollowsService](
      _.deleteDiscussionFollow(jwt, jwtType, discussionId)
    )
}

case class DiscussionFollowsServiceLive(
    discussionFollowsRepo: DiscussionFollowsRepository,
    authenticationService: AuthenticationService
) extends DiscussionFollowsService {
  val kafka = new KafkaProducerServiceLive()

  override def insertDiscussionFollow(
      jwt: String,
      jwtType: String,
      updateDiscussionFollows: UpdateDiscussionFollows
  ): ZIO[Any, AppError, Unit] = for {
    userData <- authenticationService.extractUserData(jwt, jwtType)
    _ <- discussionFollowsRepo.insertDiscussionFollow(
      DiscussionFollows(
        userId =
          updateDiscussionFollows.createdByUserId.getOrElse(userData.userId),
        followedDiscussionId = updateDiscussionFollows.id
      )
    )
  } yield ()

  override def deleteDiscussionFollow(
      jwt: String,
      jwtType: String,
      discussionId: DiscussionId
  ): ZIO[Any, AppError, Unit] = for {
    userData <- authenticationService.extractUserData(jwt, jwtType)
    _ <- discussionFollowsRepo.deleteDiscussionFollow(
      DiscussionFollows(
        userId = userData.userId,
        followedDiscussionId = discussionId.id
      )
    )
  } yield ()
}

object DiscussionFollowsServiceLive {

  val layer: URLayer[
    DiscussionFollowsRepository with AuthenticationService,
    DiscussionFollowsService
  ] = ZLayer.fromFunction(DiscussionFollowsServiceLive.apply _)

}
