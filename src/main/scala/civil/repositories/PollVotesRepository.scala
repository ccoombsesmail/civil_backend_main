package civil.repositories

import civil.models.{ErrorInfo, InternalServerError, OutgoingPollVote, PollVotes}
import io.scalaland.chimney.dsl.TransformerOps
import zio.{Has, ZIO, ZLayer}

import java.util.UUID

trait PollVotesRepository {
  def createPollVote(pollVotes: PollVotes): ZIO[Any, ErrorInfo, OutgoingPollVote]
  def deletePollVote(pollOptionId: UUID, userId: String): ZIO[Any, ErrorInfo, OutgoingPollVote]
  def getPollVoteData(pollOptionIds: List[UUID], userId: String): ZIO[Any, ErrorInfo, List[OutgoingPollVote]]

}



object PollVotesRepository {
  def createPollVote(pollVotes: PollVotes): ZIO[Has[PollVotesRepository], ErrorInfo, OutgoingPollVote] =
    ZIO.serviceWith[PollVotesRepository](
      _.createPollVote(pollVotes)
    )

  def deletePollVote(pollOptionId: UUID, userId: String): ZIO[Has[PollVotesRepository], ErrorInfo, OutgoingPollVote] =
    ZIO.serviceWith[PollVotesRepository](
      _.deletePollVote(pollOptionId, userId)
    )

  def getPollVoteData(pollOptionIds: List[UUID], userId: String): ZIO[Has[PollVotesRepository], ErrorInfo, List[OutgoingPollVote]] =
    ZIO.serviceWith[PollVotesRepository](
      _.getPollVoteData(pollOptionIds, userId)
    )
}


case class PollVotesRepositoryLive() extends PollVotesRepository {
  import QuillContextHelper.ctx._


  override def createPollVote(pollVotes: PollVotes): ZIO[Any, ErrorInfo, OutgoingPollVote] = {
    for {
      vote <- ZIO.effect(run(
        query[PollVotes]
          .insert(lift(pollVotes))
          .returning(r => r)
      )).mapError(e => InternalServerError(e.toString))
      totalVotes <- ZIO.effect(run(
        query[PollVotes].filter(_.pollOptionId == lift(pollVotes.pollOptionId))
      ).length).mapError(e => InternalServerError(e.toString))
    } yield vote.into[OutgoingPollVote].withFieldConst(_.voteCast, true).withFieldConst(_.totalVotes, totalVotes).transform
  }

  override def deletePollVote(pollOptionId: UUID, userId: String): ZIO[Any, ErrorInfo, OutgoingPollVote] = {
    for {
      vote <- ZIO.effect(run(
        query[PollVotes]
          .filter(_.pollOptionId == lift(pollOptionId))
          .delete
          .returning(r => r)
      )).mapError(e => InternalServerError(e.toString))
      totalVotes <- ZIO.effect(run(
        query[PollVotes].filter(_.pollOptionId == lift(pollOptionId))
      ).length).mapError(e => InternalServerError(e.toString))
    } yield vote.into[OutgoingPollVote].withFieldConst(_.voteCast, false).withFieldConst(_.totalVotes, totalVotes).transform
  }

  override def getPollVoteData(pollOptionIds: List[UUID], userId: String): ZIO[Any, ErrorInfo, List[OutgoingPollVote]] = {
    for {
      votes <- ZIO.effect(run(
        query[PollVotes]
          .filter(pv => liftQuery(pollOptionIds.toSet).contains(pv.pollOptionId))
          .groupBy(_.pollOptionId)
          .map { case (id, pv) => (id, pv.size)  }
      ).toMap).mapError(e => InternalServerError(e.toString))
      userVotes <- ZIO.effect(run(
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
  val live: ZLayer[Any, Nothing, Has[PollVotesRepository]] = ZLayer.succeed(PollVotesRepositoryLive())
}

