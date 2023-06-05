package civil.repositories.spaces

import civil.errors.AppError.InternalServerError
import civil.errors.AppError
import civil.models.actions.{DislikedState, LikeAction, LikedState, NeutralState}
import civil.models.enums.Sentiment.NEUTRAL
import civil.models._
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import javax.sql.DataSource

trait SpaceLikesRepository {
  def addRemoveSpaceLikeOrDislike(
      spaceLikeDislikeData: UpdateSpaceLikes,
      userId: String
  ): ZIO[Any, AppError, (SpaceLiked, Spaces)]
}

object SpaceLikesRepository {
  def addRemoveSpaceLikeOrDislike(
      spaceLikeDislikeData: UpdateSpaceLikes,
      userId: String
  ): ZIO[SpaceLikesRepository, AppError, (SpaceLiked, Spaces)] =
    ZIO.serviceWithZIO[SpaceLikesRepository](
      _.addRemoveSpaceLikeOrDislike(spaceLikeDislikeData, userId)
    )

}

case class SpaceLikesRepositoryLive(dataSource: DataSource) extends SpaceLikesRepository {
  import civil.repositories.QuillContext._

  override def addRemoveSpaceLikeOrDislike(
      spaceLikeDislikeData: UpdateSpaceLikes,
      userId: String
  ): ZIO[Any, AppError, (SpaceLiked, Spaces)] = {
   (for {
      previousLikeState <- run(
        query[SpaceLikes].filter(tl => tl.spaceId == lift(spaceLikeDislikeData.id) && tl.userId == lift(userId))
      ).mapError(e => InternalServerError(e.toString))
      newLikeState = spaceLikeDislikeData.likeAction
      prevLikeState = previousLikeState.headOption.getOrElse(SpaceLikes(spaceLikeDislikeData.id, userId, NeutralState)).likeState
      _ = println((prevLikeState, newLikeState) + Console.RED_B)
      _ = println(Console.RESET)
      likeValueToAdd = (prevLikeState, newLikeState) match {
        case (LikedState, NeutralState) => -1
        case (NeutralState, LikedState) => 1
        case (DislikedState, NeutralState) => 1
        case (NeutralState, DislikedState) => -1
        case (LikedState, DislikedState) => -2
        case (DislikedState, LikedState) => 2
        case (NeutralState, NeutralState) => 0
        case _ => -100
      }
      _ <- ZIO.when(likeValueToAdd == -100)(ZIO.fail(InternalServerError("Invalid like value")))
        .mapError(e => InternalServerError(e.toString))
        space <- transaction {
          for {
            _ <- run(
              query[SpaceLikes]
                .insertValue(lift(SpaceLikes(spaceLikeDislikeData.id, userId, spaceLikeDislikeData.likeAction)))
                .onConflictUpdate(_.spaceId, _.userId)((t, e) => t.likeState -> e.likeState)
                .returning(r => r)
            )
            updatedSpace <- run(query[Spaces]
              .filter(t => t.id == lift(spaceLikeDislikeData.id))
              .update(space => space.likes -> (space.likes + lift(likeValueToAdd)))
              .returning(t => t)
            )
          } yield updatedSpace
        }.mapError(e => InternalServerError(e.toString))
    } yield (SpaceLiked(space.id, space.likes, spaceLikeDislikeData.likeAction), space)).provideEnvironment(ZEnvironment(dataSource))
  }

}

object SpaceLikesRepositoryLive {

  val layer: URLayer[DataSource, SpaceLikesRepository] = ZLayer.fromFunction(SpaceLikesRepositoryLive.apply _)

}