package civil.services

import civil.models.{ErrorInfo, IncomingUser, OutgoingUser, TagExists, UpdateUserBio, WebHookData}
import civil.models.enums.ClerkEventType

import civil.repositories.{UsersRepository}

import zio._

trait UsersService {
  def upsertDidUser(incomingUser: IncomingUser): ZIO[Any, ErrorInfo, OutgoingUser]

  def insertOrUpdateUserHook(webHookData: WebHookData, eventType: ClerkEventType):  ZIO[Any, ErrorInfo, Unit]

  def getUser(jwt: String, jwtType: String, id: String):  ZIO[Any, ErrorInfo, OutgoingUser]
  def updateUserIcon(username: String, iconSrc: String):  ZIO[Any, ErrorInfo, OutgoingUser]
  def updateUserBio(jwt: String, jwtType: String, bioInfo: UpdateUserBio): ZIO[Any, ErrorInfo, OutgoingUser]
  def createUserTag(jwt: String, jwtType: String, tag: String): ZIO[Any, ErrorInfo, OutgoingUser]
  def checkIfTagExists(tag: String): ZIO[Any, ErrorInfo, TagExists]

}


object UsersService {

  def upsertDidUser(incomingUser: IncomingUser): ZIO[Has[UsersService], ErrorInfo, OutgoingUser]=
    ZIO.serviceWith[UsersService](_.upsertDidUser(incomingUser))

  def insertOrUpdateUserHook(webHookData: WebHookData, eventType: ClerkEventType): ZIO[Has[UsersService], ErrorInfo, Unit] =
    ZIO.serviceWith[UsersService](_.insertOrUpdateUserHook(webHookData, eventType))
  
  def getUser(jwt: String, jwtType: String, id: String):  ZIO[Has[UsersService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersService](_.getUser(jwt, jwtType, id))

  def updateUserIcon(username: String, iconSrc: String):  ZIO[Has[UsersService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersService](_.updateUserIcon(username, iconSrc))

  def updateUserBio(jwt: String, jwtType: String, bioInfo: UpdateUserBio): ZIO[Has[UsersService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersService](_.updateUserBio(jwt, jwtType, bioInfo))

  def createUserTag(
     jwt: String,
     jwtType: String,
     tag: String
  ): ZIO[Has[UsersService], ErrorInfo, OutgoingUser] =
    ZIO.serviceWith[UsersService](_.createUserTag(jwt, jwtType, tag))

  def checkIfTagExists(tag: String): ZIO[Has[UsersService], ErrorInfo, TagExists] =
    ZIO.serviceWith[UsersService](_.checkIfTagExists(tag))
}



case class UsersServiceLive(usersRepository: UsersRepository) extends UsersService  {
  implicit val ec: scala.concurrent.ExecutionContext =
    scala.concurrent.ExecutionContext.global
  val authenticationService = AuthenticationServiceLive()

  override def upsertDidUser(incomingUser: IncomingUser): ZIO[Any, ErrorInfo, OutgoingUser] = {
    usersRepository.upsertDidUser(incomingUser)
  }

  override def insertOrUpdateUserHook(webHookData: WebHookData, eventType: ClerkEventType): ZIO[Any, ErrorInfo, Unit] = {
    usersRepository.insertOrUpdateUserHook(webHookData, eventType)
  }

  override def getUser(jwt: String, jwtType: String, id: String): ZIO[Any, ErrorInfo, OutgoingUser] = {
    println(id)
    println(jwt)
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      outgoingUser <- usersRepository.getUser(id, userData.userId)
    } yield outgoingUser.copy(iconSrc = Some(userData.userIconSrc), permissions = userData.permissions)
  }

  override def updateUserIcon(username: String, iconSrc: String): ZIO[Any, ErrorInfo, OutgoingUser] = {
    usersRepository.updateUserIcon(username, iconSrc)
  }

  override def updateUserBio(jwt: String, jwtType: String, bioInfo: UpdateUserBio): ZIO[Any, ErrorInfo, OutgoingUser] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      outgoingUser <- usersRepository.updateUserBio(userData.userId, bioInfo)
    } yield outgoingUser.copy(iconSrc = Some(userData.userIconSrc), username = userData.username, permissions = userData.permissions)
  }

  override def createUserTag(jwt: String, jwtType: String, tag: String): ZIO[Any, ErrorInfo, OutgoingUser] = {
    for {
      userData <- authenticationService.extractUserData(jwt, jwtType)
      outgoingUser <- usersRepository.createUserTag(userData.userId, tag)
    } yield outgoingUser.copy(iconSrc = Some(userData.userIconSrc), username = userData.username, permissions = userData.permissions)
  }

  override def checkIfTagExists(tag: String): ZIO[Any, ErrorInfo, TagExists] = {
    usersRepository.checkIfTagExists(tag)
  }
}


object UsersServiceLive {
  val live: ZLayer[Has[UsersRepository], Throwable, Has[UsersService]] = {
    for {
      usersRepo <- ZIO.service[UsersRepository]
    } yield UsersServiceLive(usersRepo)
  }.toLayer
}