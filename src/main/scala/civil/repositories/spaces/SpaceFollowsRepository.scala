package civil.repositories.spaces

import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models._
import zio._

import javax.sql.DataSource

trait SpaceFollowsRepository {
  def insertSpaceFollow(follow: SpaceFollows): ZIO[Any, AppError, Unit]

  def deleteSpaceFollow(follow: SpaceFollows): ZIO[Any, AppError, Unit]
}

object SpaceFollowsRepository {
  def insertSpaceFollow(
      follow: SpaceFollows
  ): ZIO[SpaceFollowsRepository, AppError, Unit] =
    ZIO.serviceWithZIO[SpaceFollowsRepository](_.insertSpaceFollow(follow))

  def deleteSpaceFollow(
      follow: SpaceFollows
  ): ZIO[SpaceFollowsRepository, AppError, Unit] =
    ZIO.serviceWithZIO[SpaceFollowsRepository](_.deleteSpaceFollow(follow))

}

case class SpaceFollowsRepositoryLive(dataSource: DataSource)
    extends SpaceFollowsRepository {

  import civil.repositories.QuillContext._

  override def insertSpaceFollow(
      follow: SpaceFollows
  ): ZIO[Any, AppError, Unit] = (for {
    _ <- run(query[SpaceFollows].insertValue(lift(follow)))

  } yield ())
    .mapError(DatabaseError(_))
    .provideEnvironment(ZEnvironment(dataSource))

  override def deleteSpaceFollow(
      follow: SpaceFollows
  ): ZIO[Any, AppError, Unit] = (for {
    _ <-
      run(
        query[SpaceFollows]
          .filter(f =>
            f.userId == lift(follow.userId) && f.followedSpaceId == lift(
              follow.followedSpaceId
            )
          )
          .delete
      )

  } yield ())
    .mapError(DatabaseError(_))
    .provideEnvironment(ZEnvironment(dataSource))
}

object SpaceFollowsRepositoryLive {

  val layer: URLayer[DataSource, SpaceFollowsRepository] =
    ZLayer.fromFunction(SpaceFollowsRepositoryLive.apply _)

}
