package civil.services.spaces

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
import civil.models._
import civil.models.NotifcationEvents.{GivingUserNotificationData, SpaceLike}
import civil.models.actions.LikedState
import civil.repositories.spaces.SpaceLikesRepository
import civil.services.{AuthenticationService, AuthenticationServiceLive, KafkaProducerServiceLive}
import zio.{URLayer, ZIO, ZLayer}

import javax.sql.DataSource

trait SpaceLikesService {
  def addRemoveSpaceLikeOrDislike(
      jwt: String,
      jwtType: String,
      spaceLikeDislikeData: UpdateSpaceLikes
  ): ZIO[Any, AppError, SpaceLiked]
}

object SpaceLikesService {
  def addRemoveSpaceLikeOrDislike(
      jwt: String,
      jwtType: String,
      spaceLikeDislikeData: UpdateSpaceLikes
  ): ZIO[SpaceLikesService, AppError, SpaceLiked] =
    ZIO.serviceWithZIO[SpaceLikesService](
      _.addRemoveSpaceLikeOrDislike(jwt, jwtType, spaceLikeDislikeData)
    )

}

case class SpaceLikesServiceLive(spaceLikesRep: SpaceLikesRepository, authenticationService: AuthenticationService)
    extends SpaceLikesService {
  val kafka = new KafkaProducerServiceLive()

  override def addRemoveSpaceLikeOrDislike(
      jwt: String,
      jwtType: String,
      spaceLikeDislikeData: UpdateSpaceLikes
  ): ZIO[Any, AppError, SpaceLiked] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType).mapError(e => InternalServerError(e.toString))
      data <- spaceLikesRep
        .addRemoveSpaceLikeOrDislike(
          spaceLikeDislikeData, spaceLikeDislikeData.createdByUserId.getOrElse(userData.userId)
        ).mapError(e => InternalServerError(e.toString))
      (updatedLikeData, space) = data
      _ <- ZIO
        .when(updatedLikeData.likeState == LikedState)(
              kafka.publish(
                SpaceLike(
                  eventType = "SpaceLike",
                  spaceId = updatedLikeData.id,
                  receivingUserId = space.createdByUserId,
                  givingUserData = GivingUserNotificationData(
                    givingUserId = userData.userId,
                    givingUserUsername = userData.username,
                    givingUserTag = Some(userData.userCivilTag),
                    givingUserIconSrc = Some(userData.userIconSrc)
                  )
                ),
                space.createdByUserId,
                SpaceLike.spaceLikeSerde
              )

        )
        .mapError(e => {
          println(e)
          InternalServerError(e.toString)
        }).forkDaemon
    } yield updatedLikeData
  }

}

object SpaceLikesServiceLive {

  val layer: URLayer[SpaceLikesRepository with AuthenticationService, SpaceLikesService] = ZLayer.fromFunction(SpaceLikesServiceLive.apply _)

}
