package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models.TribunalJuryMembers
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import java.util.UUID
import javax.sql.DataSource

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

case class TribunalJuryRepositoryLive(dataSource: DataSource)
    extends TribunalJuryRepository {
  import civil.repositories.QuillContext._
  override def insertJuryMember(
      userId: String,
      contentId: UUID
  ): ZIO[Any, AppError, Unit] = {
    for {
      _ <- run(
        query[TribunalJuryMembers].insertValue(
          lift(TribunalJuryMembers(userId, contentId, contentType = "TOPIC"))
        )
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
    } yield ()
  }
}

object TribunalJuryRepositoryLive {
  val layer: URLayer[DataSource, TribunalJuryRepository] =
    ZLayer.fromFunction(TribunalJuryRepositoryLive.apply _)
}
