package civil.services.spaces

import civil.errors.AppError
import civil.models._
import civil.directives.OutgoingHttp._
import civil.models.enums.UserVerificationType.{
  CAPTCHA_VERIFIED,
  FACE_ID_AND_CAPTCHA_VERIFIED,
  FACE_ID_VERIFIED,
  NO_VERIFICATION
}
import civil.repositories.PollsRepository
import civil.repositories.spaces.SpacesRepository
import civil.services.AuthenticationService
import io.scalaland.chimney.dsl._
import zio._

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID
import scala.language.postfixOps

trait SpacesService {
  def insertSpace(
      jwt: String,
      jwtType: String,
      incomingSpace: IncomingSpace
  ): ZIO[Any, AppError, OutgoingSpace]
  def getSpaces: ZIO[Any, AppError, List[OutgoingSpace]]

  def getSpacesAuthenticated(
      jwt: String,
      jwtType: String,
      offset: Int
  ): ZIO[Any, AppError, List[OutgoingSpace]]
  def getSpace(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, AppError, OutgoingSpace]

  def getUserSpaces(
      jwt: String,
      jwtType: String,
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingSpace]]

  def getFollowedSpaces(
      jwt: String,
      jwtType: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingSpace]]

  def getSimilarSpaces(
      jwt: String,
      jwtType: String,
      spaceId: UUID
  ): ZIO[Any, AppError, List[OutgoingSpace]]
}

object SpacesService {
  def insertSpace(
      jwt: String,
      jwtType: String,
      incomingSpace: IncomingSpace
  ): ZIO[SpacesService, AppError, OutgoingSpace] =
    ZIO.serviceWithZIO[SpacesService](
      _.insertSpace(jwt, jwtType, incomingSpace)
    )

  def getSpaces(): ZIO[SpacesService, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesService](_.getSpaces)

  def getSpacesAuthenticated(
      jwt: String,
      jwtType: String,
      offset: Int
  ): ZIO[SpacesService, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesService](
      _.getSpacesAuthenticated(jwt, jwtType, offset)
    )

  def getSpace(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[SpacesService, AppError, OutgoingSpace] =
    ZIO.serviceWithZIO[SpacesService](_.getSpace(jwt, jwtType, id))

  def getUserSpaces(
      jwt: String,
      jwtType: String,
      userId: String,
      skip: Int
  ): ZIO[SpacesService, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesService](
      _.getUserSpaces(jwt, jwtType, userId, skip)
    )

  def getFollowedSpaces(
      jwt: String,
      jwtType: String,
      skip: Int
  ): ZIO[SpacesService, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesService](_.getFollowedSpaces(jwt, jwtType, skip))

  def getSimilarSpaces(
      jwt: String,
      jwtType: String,
      spaceId: UUID
  ): ZIO[SpacesService, AppError, List[OutgoingSpace]] =
    ZIO.serviceWithZIO[SpacesService](_.getSimilarSpaces(jwt, jwtType, spaceId))
}

case class SpacesServiceLive(
    spacesRepository: SpacesRepository,
    pollsRepository: PollsRepository,
    authService: AuthenticationService
) extends SpacesService {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  override def insertSpace(
      jwt: String,
      jwtType: String,
      incomingSpace: IncomingSpace
  ): ZIO[Any, AppError, OutgoingSpace] = {

    for {
      userData <- authService.extractUserData(jwt, jwtType)
      spaceId = UUID.randomUUID()
      insertedSpace <- spacesRepository.insertSpace(
        incomingSpace
          .into[Spaces]
          .withFieldConst(_.id, spaceId)
          .withFieldConst(_.likes, 0)
          .withFieldConst(
            _.createdAt,
            ZonedDateTime.now(ZoneId.systemDefault())
          )
          .withFieldConst(
            _.updatedAt,
            ZonedDateTime.now(ZoneId.systemDefault())
          )
          .withFieldConst(_.id, UUID.randomUUID())
          .withFieldConst(
            _.createdByUserId,
            incomingSpace.createdByUserId.getOrElse(userData.userId)
          )
          .withFieldConst(
            _.createdByUsername,
            incomingSpace.createdByUsername.getOrElse(userData.username)
          )
          .withFieldConst(
            _.userVerificationType,
            userData.permissions match {
              case Permissions(false, false) => NO_VERIFICATION
              case Permissions(false, true)  => CAPTCHA_VERIFIED
              case Permissions(true, false)  => FACE_ID_VERIFIED
              case Permissions(true, true)   => FACE_ID_AND_CAPTCHA_VERIFIED
            }
          )
          .enableDefaultValues
          .transform
      )
    } yield insertedSpace
  }

  override def getSpaces: ZIO[Any, AppError, List[OutgoingSpace]] = {
    spacesRepository.getSpaces
  }

  override def getSpacesAuthenticated(
      jwt: String,
      jwtType: String,
      offset: Int
  ): ZIO[Any, AppError, List[OutgoingSpace]] = {

    for {
      userData <- authService.extractUserData(jwt, jwtType)
      spaces <- spacesRepository.getSpacesAuthenticated(
        userData.userId,
        userData,
        offset
      )
    } yield spaces
  }

  override def getSpace(
      jwt: String,
      jwtType: String,
      id: UUID
  ): ZIO[Any, AppError, OutgoingSpace] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      space <- spacesRepository
        .getSpace(id, userData.userId)
    } yield space
  }

  override def getUserSpaces(
      jwt: String,
      jwtType: String,
      userId: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingSpace]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      spaces <- spacesRepository.getUserSpaces(
        userData.userId,
        userId,
        skip
      )
    } yield spaces
  }

  override def getFollowedSpaces(
      jwt: String,
      jwtType: String,
      skip: Int
  ): ZIO[Any, AppError, List[OutgoingSpace]] = {
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      spaces <- spacesRepository.getFollowedSpaces(
        userData.userId,
        skip
      )
    } yield spaces
  }

  override def getSimilarSpaces(
      jwt: String,
      jwtType: String,
      spaceId: UUID
  ): ZIO[Any, AppError, List[OutgoingSpace]] =
    for {
      userData <- authService.extractUserData(jwt, jwtType)
      spaces <- spacesRepository.getSimilarSpaces(
        spaceId
      )
    } yield spaces
}

object SpacesServiceLive {

  val layer: URLayer[
    SpacesRepository with PollsRepository with AuthenticationService,
    SpacesService
  ] = ZLayer.fromFunction(SpacesServiceLive.apply _)
}
