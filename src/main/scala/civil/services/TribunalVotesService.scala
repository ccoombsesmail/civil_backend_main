package civil.services

import civil.errors.AppError
import civil.models.{TribunalVote, TribunalVotes}
import civil.repositories.TribunalVotesRepository
import io.scalaland.chimney.dsl.TransformerOps
import zio.{URLayer, ZIO, ZLayer}

trait TribunalVotesService {
  def addTribunalVote(jwt: String, jwtType: String, tribunalVote: TribunalVote): ZIO[Any, AppError, TribunalVote]
}



object TribunalVotesService {
  def addTribunalVote(jwt: String, jwtType: String, tribunalVote: TribunalVote): ZIO[TribunalVotesService, AppError, TribunalVote] =
    ZIO.serviceWithZIO[TribunalVotesService](
      _.addTribunalVote(jwt, jwtType, tribunalVote)
    )
}


case class TribunalVotesServiceLive(tribunalVotesRepo: TribunalVotesRepository) extends TribunalVotesService {

  override def addTribunalVote(jwt: String, jwtType: String, tribunalVote: TribunalVote): ZIO[Any, AppError, TribunalVote] = {
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
  val layer: URLayer[TribunalVotesRepository, TribunalVotesService] = ZLayer.fromFunction(TribunalVotesServiceLive.apply _)
}

