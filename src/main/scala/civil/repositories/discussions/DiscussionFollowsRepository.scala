package civil.repositories.discussions

import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models._
import zio._

import javax.sql.DataSource

trait DiscussionFollowsRepository {
  def insertDiscussionFollow(
      follow: DiscussionFollows
  ): ZIO[Any, AppError, Unit]

  def deleteDiscussionFollow(
      follow: DiscussionFollows
  ): ZIO[Any, AppError, Unit]
}

object DiscussionFollowsRepository {
  def insertDiscussionFollow(
      follow: DiscussionFollows
  ): ZIO[DiscussionFollowsRepository, AppError, Unit] =
    ZIO.serviceWithZIO[DiscussionFollowsRepository](
      _.insertDiscussionFollow(follow)
    )

  def deleteDiscussionFollow(
      follow: DiscussionFollows
  ): ZIO[DiscussionFollowsRepository, AppError, Unit] =
    ZIO.serviceWithZIO[DiscussionFollowsRepository](
      _.deleteDiscussionFollow(follow)
    )

}

case class DiscussionFollowsRepositoryLive(dataSource: DataSource)
    extends DiscussionFollowsRepository {

  import civil.repositories.QuillContext._

  override def insertDiscussionFollow(
      follow: DiscussionFollows
  ): ZIO[Any, AppError, Unit] = (for {
    _ <- run(query[DiscussionFollows].insertValue(lift(follow)))

  } yield ())
    .mapError(DatabaseError(_))
    .provideEnvironment(ZEnvironment(dataSource))

  override def deleteDiscussionFollow(
      follow: DiscussionFollows
  ): ZIO[Any, AppError, Unit] = (for {
    _ <-
      run(
        query[DiscussionFollows]
          .filter(f =>
            f.userId == lift(follow.userId) && f.followedDiscussionId == lift(
              follow.followedDiscussionId
            )
          )
          .delete
      )

  } yield ())
    .mapError(DatabaseError(_))
    .provideEnvironment(ZEnvironment(dataSource))
}

object DiscussionFollowsRepositoryLive {

  val layer: URLayer[DataSource, DiscussionFollowsRepository] =
    ZLayer.fromFunction(DiscussionFollowsRepositoryLive.apply _)

}
