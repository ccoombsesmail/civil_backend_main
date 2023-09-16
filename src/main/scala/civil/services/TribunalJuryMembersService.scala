package civil.services

import civil.errors.AppError
import civil.models.JuryDuty
import zio.{URLayer, ZIO, ZLayer}
import civil.repositories.TribunalJuryMembersRepository

trait TribunalJuryMembersService {
  def getUserJuryDuties(
      jwt: String,
      jwtType: String
  ): ZIO[Any, AppError, List[JuryDuty]]

}

object TribunalJuryMembersService {
  def getUserJuryDuties(
      jwt: String,
      jwtType: String
  ): ZIO[TribunalJuryMembersService, AppError, List[JuryDuty]] =
    ZIO.serviceWithZIO[TribunalJuryMembersService](
      _.getUserJuryDuties(jwt, jwtType)
    )

}

case class TribunalJuryMembersServiceLive(
    juryMembersRepo: TribunalJuryMembersRepository,
    authenticationService: AuthenticationService
) extends TribunalJuryMembersService {

  override def getUserJuryDuties(
      jwt: String,
      jwtType: String
  ): ZIO[Any, AppError, List[JuryDuty]] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      juryDuties <- juryMembersRepo.getUserJuryDuties(
        userData.userId
      )
    } yield juryDuties
  }

}

object TribunalJuryMembersServiceLive {
  val layer: URLayer[
    TribunalJuryMembersRepository with AuthenticationService,
    TribunalJuryMembersService
  ] =
    ZLayer.fromFunction(TribunalJuryMembersServiceLive.apply _)
}
