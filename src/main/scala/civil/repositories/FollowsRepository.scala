package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models.{Follows, OutgoingUser, Users}
import civil.models._
import io.scalaland.chimney.dsl.TransformerOps
import zio._

import javax.sql.DataSource

trait FollowsRepository {
  def insertFollow(follow: Follows): ZIO[Any, AppError, OutgoingUser]

  def deleteFollow(follow: Follows): ZIO[Any, AppError, OutgoingUser]

  def getAllFollowers(userId: String): Task[List[OutgoingUser]]

  def getAllFollowed(userId: String): Task[List[OutgoingUser]]
}

object FollowsRepository {
  def insertFollow(
      follow: Follows
  ): ZIO[FollowsRepository, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[FollowsRepository](_.insertFollow(follow))

  def deleteFollow(
      follow: Follows
  ): ZIO[FollowsRepository, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[FollowsRepository](_.deleteFollow(follow))

  def getAllFollowers(
      userId: String
  ): RIO[FollowsRepository, List[OutgoingUser]] =
    ZIO.serviceWithZIO[FollowsRepository](_.getAllFollowers(userId))

  def getAllFollowed(
      userId: String
  ): RIO[FollowsRepository, List[OutgoingUser]] =
    ZIO.serviceWithZIO[FollowsRepository](_.getAllFollowed(userId))
}

case class FollowsRepositoryLive(dataSource: DataSource)
    extends FollowsRepository {

  import civil.repositories.QuillContext._

  def executeQuery[A](
      query: ZIO[Any, Throwable, A],
      errMsg: Throwable => AppError
  ): ZIO[Any, AppError, A] =
    query.mapError(errMsg).tapError(e => ZIO.logError(e.internalMsg))

  override def insertFollow(
      follow: Follows
  ): ZIO[Any, AppError, OutgoingUser] = {
    for {
      _ <- run(query[Follows].insertValue(lift(follow)))
        .mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
      users <-
        run(
          query[Users].filter(u =>
            u.userId == lift(follow.userId) || u.userId == lift(
              follow.followedUserId
            )
          )
        )
          .mapError(DatabaseError(_))
          .provideEnvironment(ZEnvironment(dataSource))
      followedUser <- ZIO
        .fromOption(users.find(u => u.userId == follow.followedUserId))
        .orElseFail(
          DatabaseError(
            new Throwable(
              s"User Not Found with id ${follow.followedUserId} in Follows Repository -> insertFollow"
            )
          )
        )
    } yield followedUser
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(true))
      .withFieldComputed(
        _.userLevelData,
        u => Some(UserLevel.apply(u.civility.toDouble))
      )
      .enableDefaultValues
      .transform

  }

  override def deleteFollow(
      follow: Follows
  ): ZIO[Any, AppError, OutgoingUser] = {
    for {
      _ <-
        run(
          query[Follows]
            .filter(f =>
              f.userId == lift(follow.userId) && f.followedUserId == lift(
                follow.followedUserId
              )
            )
            .delete
        )
          .mapError(DatabaseError(_))
          .provideEnvironment(ZEnvironment(dataSource))
      users <-
        run(
          query[Users].filter(u => u.userId == lift(follow.followedUserId))
        )
          .mapError(DatabaseError(_))
          .provideEnvironment(ZEnvironment(dataSource))
      followedUser <- ZIO
        .fromOption(users.find(u => u.userId == follow.followedUserId))
        .orElseFail(
          DatabaseError(
            new Throwable(
              s"User Not Found with id ${follow.followedUserId} in Follows Repository -> deleteFollow"
            )
          )
        )
    } yield followedUser
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(false))
      .withFieldComputed(
        _.userLevelData,
        u => Some(UserLevel.apply(u.civility.toDouble))
      )
      .enableDefaultValues
      .transform

  }

  override def getAllFollowers(userId: String): Task[List[OutgoingUser]] = {
    for {
      users <- run(
        query[Follows]
          .join(query[Users])
          .on((f, u) => f.userId == u.userId)
          .filter(row => row._1.followedUserId == lift(userId))
      ).provideEnvironment(ZEnvironment(dataSource))
    } yield users.map { case (f, u) =>
      u.into[OutgoingUser]
        .withFieldConst(_.isFollowing, None)
        .withFieldComputed(
          _.userLevelData,
          u => Some(UserLevel.apply(u.civility.toDouble))
        )
        .enableDefaultValues
        .transform
    }
  }

  override def getAllFollowed(userId: String): Task[List[OutgoingUser]] = {

    for {
      users <- run(
        query[Follows]
          .join(query[Users])
          .on((f, u) => f.followedUserId == u.userId)
          .filter(row => row._1.userId == lift(userId))
      ).provideEnvironment(ZEnvironment(dataSource))
    } yield users.map { case (_, u) =>
      u.into[OutgoingUser]
        .withFieldConst(_.isFollowing, None)
        .withFieldComputed(
          _.userLevelData,
          u => Some(UserLevel.apply(u.civility.toDouble))
        )
        .enableDefaultValues
        .transform
    }

  }
}

object FollowsRepositoryLive {

  val layer: URLayer[DataSource, FollowsRepository] =
    ZLayer.fromFunction(FollowsRepositoryLive.apply _)

}
