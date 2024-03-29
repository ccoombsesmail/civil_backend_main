package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models.{TribunalVote, TribunalVotes}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import javax.sql.DataSource

trait TribunalVotesRepository {
  def addTribunalVote(
      tribunalVote: TribunalVotes
  ): ZIO[Any, AppError, TribunalVote]
}

object TribunalVotesRepository {
  def addTopicTribunalVote(
      tribunalVote: TribunalVotes
  ): ZIO[TribunalVotesRepository, AppError, TribunalVote] =
    ZIO.serviceWithZIO[TribunalVotesRepository](
      _.addTribunalVote(tribunalVote)
    )
}

case class TribunalVotesRepositoryLive(dataSource: DataSource)
    extends TribunalVotesRepository {
  import civil.repositories.QuillContext._

  override def addTribunalVote(
      tribunalVote: TribunalVotes
  ): ZIO[Any, AppError, TribunalVote] = {
    for {
//      juryMember <- ZIO.effect(run(
//        query[TopicTribunalJury].filter(ttj => ttj.userId == lift(tribunalVote.userId))
//      )).mapError(DatabaseError(_))
//      _ <- ZIO.fail(
//        Unauthorized("Must Be Selected As A Jury Member To Vote")
//      ).unless(juryMember.nonEmpty)
      vote <- run(
        query[TribunalVotes]
          .insertValue(lift(tribunalVote))
          .onConflictUpdate(_.userId, _.contentId)(
            (t, _) => t.voteToAcquit -> lift(tribunalVote.voteToAcquit),
            (t, _) => t.voteToStrike -> lift(tribunalVote.voteToStrike)
          )
          .returning(r => r)
      ).mapError(DatabaseError(_))
        .provideEnvironment(ZEnvironment(dataSource))
    } yield vote.into[TribunalVote].transform
  }
}

object TribunalVotesRepositoryLive {
  val layer: URLayer[DataSource, TribunalVotesRepository] =
    ZLayer.fromFunction(TribunalVotesRepositoryLive.apply _)
}
