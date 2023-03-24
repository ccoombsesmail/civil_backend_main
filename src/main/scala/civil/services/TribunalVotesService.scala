package civil.services

import civil.models.{ErrorInfo, TribunalVote, TribunalVotes}
import civil.repositories.TribunalVotesRepository
import io.scalaland.chimney.dsl.TransformerOps
import zio.{Has, ZIO, ZLayer}

trait TribunalVotesService {
  def addTribunalVote(jwt: String, jwtType: String, tribunalVote: TribunalVote): ZIO[Any, ErrorInfo, TribunalVote]
}



object TribunalVotesService {
  def addTribunalVote(jwt: String, jwtType: String, tribunalVote: TribunalVote): ZIO[Has[TribunalVotesService], ErrorInfo, TribunalVote] =
    ZIO.serviceWith[TribunalVotesService](
      _.addTribunalVote(jwt, jwtType, tribunalVote)
    )
}


case class TribunalVotesServiceLive(tribunalVotesRepo: TribunalVotesRepository) extends TribunalVotesService {

  override def addTribunalVote(jwt: String, jwtType: String, tribunalVote: TribunalVote): ZIO[Any, ErrorInfo, TribunalVote] = {
    val authenticationService = AuthenticationServiceLive()

    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      vote <- tribunalVotesRepo.addTribunalVote(
        tribunalVote.into[TribunalVotes]
          .withFieldConst(_.userId, userData.userId)
          .transform
      )
    } yield vote
  }

}



object TribunalVotesServiceLive {
  val live: ZLayer[Has[TribunalVotesRepository], Nothing, Has[
    TribunalVotesService
  ]] = {
    for {
      tribunalVotesRepo <- ZIO.service[TribunalVotesRepository]
    } yield TribunalVotesServiceLive(tribunalVotesRepo)
  }.toLayer
}

