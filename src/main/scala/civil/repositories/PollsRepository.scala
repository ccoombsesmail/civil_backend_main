package civil.repositories

import civil.models.{
  AppError,
  InternalServerError,
  JsonPoll,
  PollOptions,
  Polls
}
import zio.{ZIO, ZLayer}

import java.util.UUID

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

case class PollsRepositoryLive() extends PollsRepository {

  import QuillContextHelper.ctx._

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
      _ <- ZIO
        .attempt(transaction {
          val insertedPoll = run(
            query[Polls]
              .insert(lift(newPoll))
              .returning(inserted => inserted)
          )
          run(
            liftQuery(poll.options.toList).foreach(opt =>
              query[PollOptions].insert(
                _.pollId -> lift(insertedPoll.id),
                _.uid -> opt.uid,
               _.text -> opt.text
              )
            )
          )
        })
        .mapError(e => InternalServerError(e.toString))
    } yield Some(poll)
  }
}

object PollsRepositoryLive {
  val live: ZLayer[Any, Throwable, PollsRepository] =
    ZLayer.succeed(PollsRepositoryLive())
}
