package civil.services

import civil.models.{ErrorInfo, IncomingPollVote, OutgoingPollVote, PollVotes}
import civil.repositories.PollVotesRepository
import io.scalaland.chimney.dsl.TransformerOps
import zio.{Has, ZIO, ZLayer}

import java.util.UUID

trait PollVotesService {
  def createPollVote(jwt: String, jwtType: String, pollVote: IncomingPollVote): ZIO[Any, ErrorInfo, OutgoingPollVote]
  def deletePollVote(jwt: String, jwtType: String, pollOptionId: UUID): ZIO[Any, ErrorInfo, OutgoingPollVote]
  def getPollVoteData(jwt: String, jwtType: String, pollOptionIds: List[UUID]): ZIO[Any, ErrorInfo, List[OutgoingPollVote]]

}



object PollVotesService {
  def createPollVote(jwt: String, jwtType: String, pollVote: IncomingPollVote): ZIO[Has[PollVotesService], ErrorInfo, OutgoingPollVote] =
    ZIO.serviceWith[PollVotesService](
      _.createPollVote(jwt, jwtType, pollVote)
    )

  def deletePollVote(jwt: String, jwtType: String, pollOptionId: UUID): ZIO[Has[PollVotesService], ErrorInfo, OutgoingPollVote] =
    ZIO.serviceWith[PollVotesService](
      _.deletePollVote(jwt, jwtType, pollOptionId)
    )

  def getPollVoteData(jwt: String, jwtType: String, pollOptionIds: List[UUID]): ZIO[Has[PollVotesService], ErrorInfo, List[OutgoingPollVote]] =
    ZIO.serviceWith[PollVotesService](
      _.getPollVoteData(jwt, jwtType, pollOptionIds)
    )
}


case class PollVotesServiceLive(pollVotesRepo: PollVotesRepository) extends PollVotesService {
  val authenticationService = AuthenticationServiceLive()

  override def createPollVote(jwt: String, jwtType: String, pollVotes: IncomingPollVote): ZIO[Any, ErrorInfo, OutgoingPollVote] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      vote <- pollVotesRepo.createPollVote(
        pollVotes.into[PollVotes]
          .withFieldConst(_.userId, userData.userId)
          .transform
      )
    } yield vote
  }

  override def deletePollVote(jwt: String, jwtType: String, pollOptionId: UUID): ZIO[Any, ErrorInfo, OutgoingPollVote] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      vote <- pollVotesRepo.deletePollVote(pollOptionId, userData.userId)
    } yield vote

  }

  override def getPollVoteData(jwt: String, jwtType: String, pollOptionIds: List[UUID]): ZIO[Any, ErrorInfo, List[OutgoingPollVote]] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      vote <- pollVotesRepo.getPollVoteData(pollOptionIds, userData.userId)
    } yield vote
  }

}



object PollVotesServiceLive {
  val live: ZLayer[Has[PollVotesRepository], Nothing, Has[
    PollVotesService
  ]] = {
    for {
      pollVotesRepo <- ZIO.service[PollVotesRepository]
    } yield PollVotesServiceLive(pollVotesRepo)
  }.toLayer
}
