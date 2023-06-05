package civil.services.spaces

import civil.errors.AppError
import civil.models.{SpaceFollows, SpaceId, UpdateSpaceFollows}
import civil.repositories.spaces.SpaceFollowsRepository
import civil.services.{AuthenticationService, KafkaProducerServiceLive}
import zio.{URLayer, ZIO, ZLayer}

import javax.sql.DataSource

trait SpaceFollowsService {
  def insertSpaceFollow(
      jwt: String,
      jwtType: String,
      updateSpaceFollows: UpdateSpaceFollows
  ): ZIO[Any, AppError, Unit]

  def deleteSpaceFollow(
      jwt: String,
      jwtType: String,
      topicId: SpaceId
  ): ZIO[Any, AppError, Unit]

}

object SpaceFollowsService {
  def insertSpaceFollow(
      jwt: String,
      jwtType: String,
      updateSpaceFollows: UpdateSpaceFollows
  ): ZIO[SpaceFollowsService, AppError, Unit] =
    ZIO.serviceWithZIO[SpaceFollowsService](
      _.insertSpaceFollow(jwt, jwtType, updateSpaceFollows)
    )

  def deleteSpaceFollow(
      jwt: String,
      jwtType: String,
      spaceId: SpaceId
  ): ZIO[SpaceFollowsService, AppError, Unit] =
    ZIO.serviceWithZIO[SpaceFollowsService](
      _.deleteSpaceFollow(jwt, jwtType, spaceId)
    )
}

case class SpaceFollowsServiceLive(
    topicFollowsRep: SpaceFollowsRepository,
    authenticationService: AuthenticationService
) extends SpaceFollowsService {
  val kafka = new KafkaProducerServiceLive()

  override def insertSpaceFollow(
      jwt: String,
      jwtType: String,
      updateSpaceFollows: UpdateSpaceFollows
  ): ZIO[Any, AppError, Unit] = for {
    userData <- authenticationService.extractUserData(jwt, jwtType)
    _ <- topicFollowsRep.insertSpaceFollow(
      SpaceFollows(userId = updateSpaceFollows.createdByUserId.getOrElse(userData.userId), followedSpaceId = updateSpaceFollows.id)
    )
  } yield ()

  override def deleteSpaceFollow(
      jwt: String,
      jwtType: String,
      topicId: SpaceId
  ): ZIO[Any, AppError, Unit] = for {
    userData <- authenticationService.extractUserData(jwt, jwtType)
    _ <- topicFollowsRep.deleteSpaceFollow(
      SpaceFollows(userId = userData.userId, followedSpaceId = topicId.id)
    )
  } yield ()
}

object SpaceFollowsServiceLive {

  val layer: URLayer[
    SpaceFollowsRepository with AuthenticationService,
    SpaceFollowsService
  ] = ZLayer.fromFunction(SpaceFollowsServiceLive.apply _)

}
