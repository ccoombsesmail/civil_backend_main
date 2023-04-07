package civil.services

import civil.errors.AppError
import civil.models.{IncomingPollVote, OutgoingPollVote, PollVotes}
import civil.repositories.PollVotesRepository
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZIO, ZLayer}

import java.util.UUID

trait PollVotesService {
  def createPollVote(jwt: String, jwtType: String, pollVote: IncomingPollVote): ZIO[Any, AppError, OutgoingPollVote]
  def deletePollVote(jwt: String, jwtType: String, pollOptionId: UUID): ZIO[Any, AppError, OutgoingPollVote]
  def getPollVoteData(jwt: String, jwtType: String, pollOptionIds: List[UUID]): ZIO[Any, AppError, List[OutgoingPollVote]]

}



object PollVotesService {
  def createPollVote(jwt: String, jwtType: String, pollVote: IncomingPollVote): ZIO[PollVotesService, AppError, OutgoingPollVote] =
    ZIO.serviceWithZIO[PollVotesService](
      _.createPollVote(jwt, jwtType, pollVote)
    )

  def deletePollVote(jwt: String, jwtType: String, pollOptionId: UUID): ZIO[PollVotesService, AppError, OutgoingPollVote] =
    ZIO.serviceWithZIO[PollVotesService](
      _.deletePollVote(jwt, jwtType, pollOptionId)
    )

  def getPollVoteData(jwt: String, jwtType: String, pollOptionIds: List[UUID]): ZIO[PollVotesService, AppError, List[OutgoingPollVote]] =
    ZIO.serviceWithZIO[PollVotesService](
      _.getPollVoteData(jwt, jwtType, pollOptionIds)
    )
}


case class PollVotesServiceLive(pollVotesRepo: PollVotesRepository) extends PollVotesService {
  val authenticationService = AuthenticationServiceLive()

  override def createPollVote(jwt: String, jwtType: String, pollVotes: IncomingPollVote): ZIO[Any, AppError, OutgoingPollVote] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      vote <- pollVotesRepo.createPollVote(
        pollVotes.into[PollVotes]
          .withFieldConst(_.userId, userData.userId)
          .transform
      )
    } yield vote
  }

  override def deletePollVote(jwt: String, jwtType: String, pollOptionId: UUID): ZIO[Any, AppError, OutgoingPollVote] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      vote <- pollVotesRepo.deletePollVote(pollOptionId, userData.userId)
    } yield vote

  }

  override def getPollVoteData(jwt: String, jwtType: String, pollOptionIds: List[UUID]): ZIO[Any, AppError, List[OutgoingPollVote]] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      vote <- pollVotesRepo.getPollVoteData(pollOptionIds, userData.userId)
    } yield vote
  }

}



object PollVotesServiceLive {

  val layer: URLayer[PollVotesRepository, PollVotesService] = ZLayer.fromFunction(PollVotesServiceLive.apply _)

}
