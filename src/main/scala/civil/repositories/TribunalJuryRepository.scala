package civil.repositories

import civil.models.{ErrorInfo, InternalServerError, TribunalJuryMembers}
import civil.models.InternalServerError
import zio.{Has, ZIO, ZLayer}

import java.util.UUID

trait TribunalJuryRepository {
  def insertJuryMember(
      userId: String,
      contentId: UUID
  ): ZIO[Any, ErrorInfo, Unit]

}

object TribunalJuryRepository {
  def insertJuryMember(
      userId: String,
      contentId: UUID
  ): ZIO[Has[TribunalJuryRepository], ErrorInfo, Unit] =
    ZIO.serviceWith[TribunalJuryRepository](
      _.insertJuryMember(userId, contentId)
    )

}



case class TribunalJuryRepositoryLive() extends TribunalJuryRepository {
  import QuillContextHelper.ctx._

  override def insertJuryMember(userId: String, contentId: UUID): ZIO[Any, ErrorInfo, Unit] = {
    for {
      _ <- ZIO.effect(run(
        query[TribunalJuryMembers].insert(lift(TribunalJuryMembers(userId, contentId, contentType = "TOPIC" )))
      )).mapError(e => InternalServerError(e.toString))
    } yield ()
  }
}


object TribunalJuryRepositoryLive {
  val live: ZLayer[Any, Throwable, Has[TribunalJuryRepository]] =
    ZLayer.succeed(TribunalJuryRepositoryLive())
}

