package civil.repositories

import civil.models.{AppError, InternalServerError, TribunalJuryMembers}
import civil.models.InternalServerError
import zio.{ZIO, ZLayer}

import java.util.UUID

trait TribunalJuryRepository {
  def insertJuryMember(
      userId: String,
      contentId: UUID
  ): ZIO[Any, AppError, Unit]

}

object TribunalJuryRepository {
  def insertJuryMember(
      userId: String,
      contentId: UUID
  ): ZIO[TribunalJuryRepository, AppError, Unit] =
    ZIO.serviceWithZIO[TribunalJuryRepository](
      _.insertJuryMember(userId, contentId)
    )

}



case class TribunalJuryRepositoryLive() extends TribunalJuryRepository {
  import QuillContextHelper.ctx._

  override def insertJuryMember(userId: String, contentId: UUID): ZIO[Any, AppError, Unit] = {
    for {
      _ <- ZIO.attempt(run(
        query[TribunalJuryMembers].insert(lift(TribunalJuryMembers(userId, contentId, contentType = "TOPIC" )))
      )).mapError(e => InternalServerError(e.toString))
    } yield ()
  }
}


object TribunalJuryRepositoryLive {
  val live: ZLayer[Any, Throwable, TribunalJuryRepository] =
    ZLayer.succeed(TribunalJuryRepositoryLive())
}

