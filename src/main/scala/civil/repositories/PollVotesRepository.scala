package civil.repositories


import civil.errors.AppError
import civil.errors.AppError.InternalServerError
import civil.models.{OutgoingPollVote, PollVotes}
import civil.services.{PollVotesService, PollVotesServiceLive}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZIO, ZLayer}

import java.util.UUID

trait PollVotesRepository {
  def createPollVote(pollVotes: PollVotes): ZIO[Any, AppError, OutgoingPollVote]
  def deletePollVote(pollOptionId: UUID, userId: String): ZIO[Any, AppError, OutgoingPollVote]
  def getPollVoteData(pollOptionIds: List[UUID], userId: String): ZIO[Any, AppError, List[OutgoingPollVote]]

}



object PollVotesRepository {
  def createPollVote(pollVotes: PollVotes): ZIO[PollVotesRepository, AppError, OutgoingPollVote] =
    ZIO.serviceWithZIO[PollVotesRepository](
      _.createPollVote(pollVotes)
    )

  def deletePollVote(pollOptionId: UUID, userId: String): ZIO[PollVotesRepository, AppError, OutgoingPollVote] =
    ZIO.serviceWithZIO[PollVotesRepository](
      _.deletePollVote(pollOptionId, userId)
    )

  def getPollVoteData(pollOptionIds: List[UUID], userId: String): ZIO[PollVotesRepository, AppError, List[OutgoingPollVote]] =
    ZIO.serviceWithZIO[PollVotesRepository](
      _.getPollVoteData(pollOptionIds, userId)
    )
}


case class PollVotesRepositoryLive() extends PollVotesRepository {
  import QuillContextHelper.ctx._


  override def createPollVote(pollVotes: PollVotes): ZIO[Any, AppError, OutgoingPollVote] = {
    for {
      vote <- ZIO.attempt(run(
        query[PollVotes]
          .insertValue(lift(pollVotes))
          .returning(r => r)
      )).mapError(e => InternalServerError(e.toString))
      totalVotes <- ZIO.attempt(run(
        query[PollVotes].filter(_.pollOptionId == lift(pollVotes.pollOptionId))
      ).length).mapError(e => InternalServerError(e.toString))
    } yield vote.into[OutgoingPollVote].withFieldConst(_.voteCast, true).withFieldConst(_.totalVotes, totalVotes).transform
  }

  override def deletePollVote(pollOptionId: UUID, userId: String): ZIO[Any, AppError, OutgoingPollVote] = {
    for {
      vote <- ZIO.attempt(run(
        query[PollVotes]
          .filter(_.pollOptionId == lift(pollOptionId))
          .delete
          .returning(r => r)
      )).mapError(e => InternalServerError(e.toString))
      totalVotes <- ZIO.attempt(run(
        query[PollVotes].filter(_.pollOptionId == lift(pollOptionId))
      ).length).mapError(e => InternalServerError(e.toString))
    } yield vote.into[OutgoingPollVote].withFieldConst(_.voteCast, false).withFieldConst(_.totalVotes, totalVotes).transform
  }

  override def getPollVoteData(pollOptionIds: List[UUID], userId: String): ZIO[Any, AppError, List[OutgoingPollVote]] = {
    for {
      votes <- ZIO.attempt(run(
        query[PollVotes]
          .filter(pv => liftQuery(pollOptionIds.toSet).contains(pv.pollOptionId))
          .groupBy(_.pollOptionId)
          .map { case (id, pv) => (id, pv.size)  }
      ).toMap).mapError(e => InternalServerError(e.toString))
      userVotes <- ZIO.attempt(run(
        query[PollVotes]
          .filter(pv => liftQuery(pollOptionIds.toSet).contains(pv.pollOptionId) && pv.userId == lift(userId))
      )).mapError(e => InternalServerError(e.toString))
      voteMap = userVotes.foldLeft(Map[UUID, PollVotes]()) { (m, pv) =>
        m + (pv.pollOptionId -> pv)
      }
      res = pollOptionIds.map(id => OutgoingPollVote(
        pollOptionId = id,
        voteCast = voteMap.contains(id),
        totalVotes = votes.getOrElse(id, 0L).toInt
      ))
    } yield res
  }
}



object PollVotesRepositoryLive {
  val layer: URLayer[Any, PollVotesRepository] = ZLayer.fromFunction(PollVotesRepositoryLive.apply _)
}

