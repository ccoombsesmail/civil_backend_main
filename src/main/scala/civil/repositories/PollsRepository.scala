package civil.repositories

import civil.models.{
  ErrorInfo,
  InternalServerError,
  JsonPoll,
  PollOptions,
  Polls
}
import zio.{Has, ZIO, ZLayer}

import java.util.UUID

trait PollsRepository {
  def insertPoll(contentId: UUID, poll: JsonPoll): ZIO[Any, ErrorInfo, Option[JsonPoll]]
}

object PollsRepository {
  def insertPoll(
      contentId: UUID,
      poll: JsonPoll
  ): ZIO[Has[PollsRepository], ErrorInfo, Option[JsonPoll]] =
    ZIO.serviceWith[PollsRepository](_.insertPoll(contentId, poll))

}

case class PollsRepositoryLive() extends PollsRepository {

  import QuillContextHelper.ctx._

  override def insertPoll(
      contentId: UUID,
      poll: JsonPoll
  ): ZIO[Any, ErrorInfo, Option[JsonPoll]] = {
    val newPoll = Polls(
      contentId = contentId,
      question = poll.question,
      version = poll.version
    )
    for {
      _ <- ZIO
        .effect(transaction {
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
  val live: ZLayer[Any, Throwable, Has[PollsRepository]] =
    ZLayer.succeed(PollsRepositoryLive())
}
