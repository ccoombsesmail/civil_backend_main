package civil.repositories.topics

import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models._
import zio._

import javax.sql.DataSource

trait TopicFollowsRepository {
  def insertTopicFollow(follow: TopicFollows): ZIO[Any, AppError, Unit]
  def deleteTopicFollow(follow: TopicFollows): ZIO[Any, AppError, Unit]
}

object TopicFollowsRepository {
  def insertTopicFollow(
      follow: TopicFollows
  ): ZIO[TopicFollowsRepository, AppError, Unit] =
    ZIO.serviceWithZIO[TopicFollowsRepository](_.insertTopicFollow(follow))

  def deleteTopicFollow(
      follow: TopicFollows
  ): ZIO[TopicFollowsRepository, AppError, Unit] =
    ZIO.serviceWithZIO[TopicFollowsRepository](_.deleteTopicFollow(follow))

}

case class TopicFollowsRepositoryLive(dataSource: DataSource)
    extends TopicFollowsRepository {

  import civil.repositories.QuillContext._

  override def insertTopicFollow(
      follow: TopicFollows
  ): ZIO[Any, AppError, Unit] = (for {
    _ <- run(query[TopicFollows].insertValue(lift(follow)))

  } yield ())
    .mapError(e => InternalServerError(e.toString))
    .provideEnvironment(ZEnvironment(dataSource))

  override def deleteTopicFollow(
      follow: TopicFollows
  ): ZIO[Any, AppError, Unit] = (for {
    _ <-
      run(
        query[TopicFollows]
          .filter(f =>
            f.userId == lift(follow.userId) && f.followedTopicId == lift(
              follow.followedTopicId
            )
          )
          .delete
      )

  } yield ())
    .mapError(e => InternalServerError(e.toString))
    .provideEnvironment(ZEnvironment(dataSource))
}

object TopicFollowsRepositoryLive {

  val layer: URLayer[DataSource, TopicFollowsRepository] =
    ZLayer.fromFunction(TopicFollowsRepositoryLive.apply _)

}
