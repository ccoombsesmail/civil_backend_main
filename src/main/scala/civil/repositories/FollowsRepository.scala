package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.{Follows, NotFound, OutgoingUser, Users}
import civil.models.NotifcationEvents.NewFollower
import civil.models._
import civil.services.KafkaProducerServiceLive
import io.scalaland.chimney.dsl._
import zio._

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

case class FollowsRepositoryLive() extends FollowsRepository {

  import QuillContextHelper.ctx._

  override def insertFollow(
      follow: Follows
  ): ZIO[Any, AppError, OutgoingUser] = {
    for {
      _ <- ZIO
        .attempt(run(query[Follows].insertValue(lift(follow))))
        .mapError(e => InternalServerError(e.toString))
      users <- ZIO
        .attempt(
          run(
            query[Users].filter(u => u.userId == lift(follow.userId) || u.userId == lift(follow.followedUserId))
          )
        )
        .mapError(e => InternalServerError(e.toString))
      user <- ZIO.fromOption(users.find(u => u.userId == follow.userId)).orElseFail(InternalServerError("User Not Found"))
      followedUser <-  ZIO.fromOption(users.find(u => u.userId == follow.followedUserId)).orElseFail(InternalServerError("User Not Found"))
    } yield followedUser
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(true))
      .withFieldComputed(_.userLevelData, u => Some(UserLevel.apply(u.civility.toDouble)))
      .transform

  }
  override def deleteFollow(
      follow: Follows
  ): ZIO[Any, AppError, OutgoingUser] = {
    for {
      _ <- ZIO
        .attempt(
          run(
            query[Follows]
              .filter(f =>
                f.userId == lift(follow.userId) && f.followedUserId == lift(
                  follow.followedUserId
                )
              )
              .delete
          )
        )
        .mapError(e => InternalServerError(e.toString))
      users <- ZIO
        .attempt(
          run(
            query[Users].filter(u => u.userId == lift(follow.followedUserId))
          )
        )
        .mapError(e => InternalServerError(e.toString))
      followedUser <-  ZIO.fromOption(users.find(u => u.userId == follow.followedUserId)).orElseFail(InternalServerError("User Not Found"))
    } yield followedUser
      .into[OutgoingUser]
      .withFieldConst(_.isFollowing, Some(false))
      .withFieldComputed(_.userLevelData, u => Some(UserLevel.apply(u.civility.toDouble)))
      .transform

  }
  override def getAllFollowers(userId: String): Task[List[OutgoingUser]] = {
    val users = run(
      query[Follows]
        .join(query[Users])
        .on((f, u) => f.userId == u.userId)
        .filter(row => row._1.followedUserId == lift(userId))
    )
    ZIO.succeed(users.map { case (f, u) =>
      u.into[OutgoingUser]
        .withFieldConst(_.isFollowing, None)
        .withFieldComputed(_.userLevelData, u => Some(UserLevel.apply(u.civility.toDouble)))
        .transform
    })
  }
  override def getAllFollowed(userId: String): Task[List[OutgoingUser]] = {
    println(userId)
    val users = run(
      query[Follows]
        .join(query[Users])
        .on((f, u) => f.followedUserId == u.userId)
        .filter(row => row._1.userId == lift(userId))
    )
    ZIO.succeed(users.map { case (f, u) =>
      u.into[OutgoingUser]
        .withFieldConst(_.isFollowing, None)
        .withFieldComputed(_.userLevelData, u => Some(UserLevel.apply(u.civility.toDouble)))
        .transform
    })
  }

}

object FollowsRepositoryLive {

  val layer: URLayer[Any, FollowsRepository] = ZLayer.fromFunction(FollowsRepositoryLive.apply _)

}
