package civil.services

import civil.errors.AppError
import civil.errors.AppError.InternalServerError

import civil.models.NotifcationEvents.{GivingUserNotificationData, NewFollower}
import civil.models.{BadRequest, FollowedUserId, Follows, OutgoingUser}
import civil.repositories.FollowsRepository
import zio._

trait FollowsService {
  def insertFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Any, AppError, OutgoingUser]
  def deleteFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Any, AppError, OutgoingUser]
  def getAllFolowers(userId: String): Task[List[OutgoingUser]]
  def getAllFollowed(userId: String): Task[List[OutgoingUser]]
}

object FollowsService {
  def insertFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[FollowsService, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[FollowsService](
      _.insertFollow(jwt, jwtType, followedUserId)
    )

  def deleteFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[FollowsService, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[FollowsService](
      _.deleteFollow(jwt, jwtType, followedUserId)
    )

  def getAllFolowers(
      userId: String
  ): RIO[FollowsService, List[OutgoingUser]] =
    ZIO.serviceWithZIO[FollowsService](_.getAllFolowers(userId))

  def getAllFollowed(
      userId: String
  ): RIO[FollowsService, List[OutgoingUser]] =
    ZIO.serviceWithZIO[FollowsService](_.getAllFollowed(userId))
}

case class FollowsServiceLive(
    followsRepository: FollowsRepository,
    authenticationService: AuthenticationService
) extends FollowsService {
  val kafka = new KafkaProducerServiceLive()

  override def insertFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Any, AppError, OutgoingUser] = {

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- ZIO
        .fail(InternalServerError(new Throwable("User can't follow self")))
        .when(userData.userId == followedUserId.followedUserId)
      outgoingUser <- followsRepository.insertFollow(
        Follows(
          userId = userData.userId,
          followedUserId = followedUserId.followedUserId
        )
      )
      _ <- ZIO
        .attempt(
          kafka.publish(
            NewFollower(
              eventType = "NewFollower",
              followedUserId = followedUserId.followedUserId,
              givingUserData = GivingUserNotificationData(
                givingUserId = userData.userId,
                givingUserTag = Some(userData.userCivilTag),
                givingUserIconSrc = Some(userData.userIconSrc),
                givingUserUsername = userData.username
              )
            ),
            followedUserId.followedUserId,
            NewFollower.newFollowerSerde
          )
        )
        .fork
    } yield outgoingUser
  }
  override def deleteFollow(
      jwt: String,
      jwtType: String,
      followedUserId: FollowedUserId
  ): ZIO[Any, AppError, OutgoingUser] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      _ <- ZIO
        .fail(InternalServerError(new Throwable("User can't unfollow self")))
        .when(userData.userId == followedUserId.followedUserId)
      outgoingUser <- followsRepository.deleteFollow(
        Follows(
          userId = userData.userId,
          followedUserId = followedUserId.followedUserId
        )
      )
    } yield outgoingUser
  }
  override def getAllFolowers(userId: String): Task[List[OutgoingUser]] = {
    followsRepository.getAllFollowers(userId)
  }
  override def getAllFollowed(userId: String): Task[List[OutgoingUser]] = {
    followsRepository.getAllFollowed(userId)
  }

}

object FollowsServiceLive {
  val layer
      : URLayer[FollowsRepository with AuthenticationService, FollowsService] =
    ZLayer.fromFunction(FollowsServiceLive.apply _)
}
