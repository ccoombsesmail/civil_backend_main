package civil.services

import civil.errors.AppError
import civil.models.{IncomingUser, OutgoingUser, TagExists, UpdateUserBio, WebHookData}
import civil.models.enums.ClerkEventType

import civil.repositories.UsersRepository

import zio._

trait UsersService {
  def upsertDidUser(incomingUser: IncomingUser): ZIO[Any, AppError, OutgoingUser]

  def getUser(jwt: String, jwtType: String, id: String):  ZIO[Any, AppError, OutgoingUser]
  def updateUserIcon(username: String, iconSrc: String):  ZIO[Any, AppError, OutgoingUser]
  def updateUserBio(jwt: String, jwtType: String, bioInfo: UpdateUserBio): ZIO[Any, AppError, OutgoingUser]
  def createUserTag(jwt: String, jwtType: String, tag: String): ZIO[Any, AppError, OutgoingUser]
  def checkIfTagExists(tag: String): ZIO[Any, AppError, TagExists]

}


object UsersService {

  def upsertDidUser(incomingUser: IncomingUser): ZIO[UsersService, AppError, OutgoingUser]=
    ZIO.serviceWithZIO[UsersService](_.upsertDidUser(incomingUser))

  
  def getUser(jwt: String, jwtType: String, id: String):  ZIO[UsersService, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[UsersService](_.getUser(jwt, jwtType, id))

  def updateUserIcon(username: String, iconSrc: String):  ZIO[UsersService, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[UsersService](_.updateUserIcon(username, iconSrc))

  def updateUserBio(jwt: String, jwtType: String, bioInfo: UpdateUserBio): ZIO[UsersService, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[UsersService](_.updateUserBio(jwt, jwtType, bioInfo))

  def createUserTag(
     jwt: String,
     jwtType: String,
     tag: String
  ): ZIO[UsersService, AppError, OutgoingUser] =
    ZIO.serviceWithZIO[UsersService](_.createUserTag(jwt, jwtType, tag))

  def checkIfTagExists(tag: String): ZIO[UsersService, AppError, TagExists] =
    ZIO.serviceWithZIO[UsersService](_.checkIfTagExists(tag))
}



case class UsersServiceLive(usersRepository: UsersRepository, authenticationService: AuthenticationService) extends UsersService  {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global

  override def upsertDidUser(incomingUser: IncomingUser): ZIO[Any, AppError, OutgoingUser] = {
    usersRepository.upsertDidUser(incomingUser)
  }

  override def getUser(jwt: String, jwtType: String, id: String): ZIO[Any, AppError, OutgoingUser] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      outgoingUser <- usersRepository.getUser(id, userData.userId)
    } yield outgoingUser.copy(iconSrc = Some(userData.userIconSrc), permissions = userData.permissions)
  }

  override def updateUserIcon(username: String, iconSrc: String): ZIO[Any, AppError, OutgoingUser] = {
    usersRepository.updateUserIcon(username, iconSrc)
  }

  override def updateUserBio(jwt: String, jwtType: String, bioInfo: UpdateUserBio): ZIO[Any, AppError, OutgoingUser] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      outgoingUser <- usersRepository.updateUserBio(userData.userId, bioInfo)
    } yield outgoingUser.copy(iconSrc = Some(userData.userIconSrc), username = userData.username, permissions = userData.permissions)
  }

  override def createUserTag(jwt: String, jwtType: String, tag: String): ZIO[Any, AppError, OutgoingUser] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      outgoingUser <- usersRepository.createUserTag(userData.userId, tag)
    } yield outgoingUser.copy(iconSrc = Some(userData.userIconSrc), username = userData.username, permissions = userData.permissions)
  }

  override def checkIfTagExists(tag: String): ZIO[Any, AppError, TagExists] = {
    usersRepository.checkIfTagExists(tag)
  }
}


object UsersServiceLive {
  val layer: URLayer[UsersRepository with AuthenticationService, UsersService] = ZLayer.fromFunction(UsersServiceLive.apply _)
}