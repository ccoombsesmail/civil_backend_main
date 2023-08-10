package civil.repositories

import civil.errors.AppError
import civil.errors.AppError.{DatabaseError, InternalServerError}
import civil.models.{OutgoingPollVote, PollVotes}
import civil.services.{PollVotesService, PollVotesServiceLive}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZEnvironment, ZIO, ZLayer}

import java.util.UUID
import javax.sql.DataSource

trait PollVotesRepository {
  def createPollVote(pollVotes: PollVotes): ZIO[Any, AppError, OutgoingPollVote]

  def deletePollVote(
      pollOptionId: UUID,
      userId: String
  ): ZIO[Any, AppError, OutgoingPollVote]

  def getPollVoteData(
      pollOptionIds: List[UUID],
      userId: String
  ): ZIO[Any, AppError, List[OutgoingPollVote]]

}

object PollVotesRepository {
  def createPollVote(
      pollVotes: PollVotes
  ): ZIO[PollVotesRepository, AppError, OutgoingPollVote] =
    ZIO.serviceWithZIO[PollVotesRepository](
      _.createPollVote(pollVotes)
    )

  def deletePollVote(
      pollOptionId: UUID,
      userId: String
  ): ZIO[PollVotesRepository, AppError, OutgoingPollVote] =
    ZIO.serviceWithZIO[PollVotesRepository](
      _.deletePollVote(pollOptionId, userId)
    )

  def getPollVoteData(
      pollOptionIds: List[UUID],
      userId: String
  ): ZIO[PollVotesRepository, AppError, List[OutgoingPollVote]] =
    ZIO.serviceWithZIO[PollVotesRepository](
      _.getPollVoteData(pollOptionIds, userId)
    )
}

case class PollVotesRepositoryLive(dataSource: DataSource)
    extends PollVotesRepository {

  import civil.repositories.QuillContext._

  override def createPollVote(
      pollVotes: PollVotes
  ): ZIO[Any, AppError, OutgoingPollVote] = {
    for {
      vote <- run(
        query[PollVotes]
          .insertValue(lift(pollVotes))
          .returning(r => r)
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
      totalVotes <- run(
        query[PollVotes].filter(_.pollOptionId == lift(pollVotes.pollOptionId))
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
    } yield vote
      .into[OutgoingPollVote]
      .withFieldConst(_.voteCast, true)
      .withFieldConst(_.totalVotes, totalVotes.length)
      .transform
  }

  override def deletePollVote(
      pollOptionId: UUID,
      userId: String
  ): ZIO[Any, AppError, OutgoingPollVote] = {
    for {
      vote <- run(
        query[PollVotes]
          .filter(_.pollOptionId == lift(pollOptionId))
          .delete
          .returning(r => r)
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
      totalVotes <- run(
        query[PollVotes].filter(_.pollOptionId == lift(pollOptionId))
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
    } yield vote
      .into[OutgoingPollVote]
      .withFieldConst(_.voteCast, false)
      .withFieldConst(_.totalVotes, totalVotes.length)
      .transform
  }

  override def getPollVoteData(
      pollOptionIds: List[UUID],
      userId: String
  ): ZIO[Any, AppError, List[OutgoingPollVote]] = {
    for {
      votes <- run(
        query[PollVotes]
          .filter(pv =>
            liftQuery(pollOptionIds.toSet).contains(pv.pollOptionId)
          )
          .groupBy(_.pollOptionId)
          .map { case (id, pv) => (id, pv.size) }
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
      totalVotesMap = votes.toMap
      userVotes <- run(
        query[PollVotes]
          .filter(pv =>
            liftQuery(pollOptionIds.toSet).contains(
              pv.pollOptionId
            ) && pv.userId == lift(userId)
          )
      ).mapError(DatabaseError(_)).provideEnvironment(ZEnvironment(dataSource))
      voteMap = userVotes.foldLeft(Map[UUID, PollVotes]()) { (m, pv) =>
        m + (pv.pollOptionId -> pv)
      }
      res = pollOptionIds.map(id =>
        OutgoingPollVote(
          pollOptionId = id,
          voteCast = voteMap.contains(id),
          totalVotes = totalVotesMap.getOrElse(id, 0L).toInt
        )
      )
    } yield res
  }
}

object PollVotesRepositoryLive {
  val layer: URLayer[DataSource, PollVotesRepository] =
    ZLayer.fromFunction(PollVotesRepositoryLive.apply _)
}
