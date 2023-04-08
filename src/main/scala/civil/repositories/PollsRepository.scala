package civil.repositories


import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.{JsonPoll, PollOptions, Polls}
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import java.util.UUID
import javax.sql.DataSource

trait PollsRepository {
  def insertPoll(contentId: UUID, poll: JsonPoll): ZIO[Any, AppError, Option[JsonPoll]]
}

object PollsRepository {
  def insertPoll(
      contentId: UUID,
      poll: JsonPoll
  ): ZIO[PollsRepository, AppError, Option[JsonPoll]] =
    ZIO.serviceWithZIO[PollsRepository](_.insertPoll(contentId, poll))

}

case class PollsRepositoryLive(dataSource: DataSource) extends PollsRepository {

  import civil.repositories.QuillContext._

  override def insertPoll(
      contentId: UUID,
      poll: JsonPoll
  ): ZIO[Any, AppError, Option[JsonPoll]] = {
    val newPoll = Polls(
      contentId = contentId,
      question = poll.question,
      version = poll.version
    )
    for {
      _ <- transaction {
          val insertedPoll = run(
            query[Polls]
              .insertValue(lift(newPoll))
              .returning(inserted => inserted)
          ).provideEnvironment(ZEnvironment(dataSource))
          insertedPoll.map(inserted =>
            run(
              liftQuery(poll.options.toList).foreach(opt =>
                query[PollOptions].insert(
                  _.pollId -> lift(inserted.id),
                  _.uid -> opt.uid,
                  _.text -> opt.text
                )
              )
            ).provideEnvironment(ZEnvironment(dataSource))
          )
        }
        .mapError(e => InternalServerError(e.toString)).provideEnvironment(ZEnvironment(dataSource))
    } yield Some(poll)
  }
}

object PollsRepositoryLive {
  val layer: URLayer[DataSource, PollsRepository] = ZLayer.fromFunction(PollsRepositoryLive.apply _)

}
