package civil.controllers


import java.util.UUID
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import civil.repositories.{UsersRepository, UsersRepositoryLive}
import civil.services.{UsersService, UsersServiceLive}
import civil.models.Users
import civil.apis.UsersApi._
import civil.models.ErrorInfo
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._
import zhttp.http.Method


object UsersController  {


    val upsertDidUserEndpointRoute: Http[Has[UsersService], Throwable, Request, Response[Any, Throwable]] = {
     ZioHttpInterpreter().toHttp(upsertDidUserEndpoint)(incomingUser => {
       UsersService.upsertDidUser(incomingUser)
         .map(user => {
           Right(user)
         }).catchAll(e => ZIO.succeed(Left(e)))
         .provideLayer(UsersRepositoryLive.live >>> UsersServiceLive.live)
     })
   }



  val getUserEndpointRoute: Http[Has[UsersService], Throwable, Request, Response[Any, Throwable]] = {
   ZioHttpInterpreter().toHttp(getUserEndpoint) { case (jwt, jwtType, userId) =>
     UsersService.getUser(jwt, jwtType, userId)
        .map(user => {
          Right(user)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(UsersRepositoryLive.live >>> UsersServiceLive.live)
    }
  }


  val updateUserIconEndpointRoute: Http[Has[UsersService], Throwable, Request, Response[Any, Throwable]] = {
   ZioHttpInterpreter().toHttp(updateUserIconEndpoint)(data => { 
      UsersService.updateUserIcon(data.username, data.iconSrc)
        .map(user => {
          Right(user)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(UsersRepositoryLive.live >>> UsersServiceLive.live)
    }) 
  }

  val updateUserBioInformationEndpointRoute: Http[Has[UsersService], Throwable, Request, Response[Any, Throwable]] = {
   ZioHttpInterpreter().toHttp(updateUserBioInformationEndpoint){ case (jwt, jwtType, bioInfo) =>
      UsersService.updateUserBio(jwt, jwtType, bioInfo)
        .map(user => {
          Right((user))
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(UsersRepositoryLive.live >>> UsersServiceLive.live)
    }
  }

  val uploadUserIconEndpointRoute: Http[Has[UsersService], Throwable, Request, Response[Any, Throwable]] = {
   ZioHttpInterpreter().toHttp(uploadUserIconEndpoint)(data => {
      UsersService.updateUserIcon(data.username, data.iconSrc)
        .map(user => {
          Right(user)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(UsersRepositoryLive.live >>> UsersServiceLive.live)
    })
  }

  val createUserTagEndpointRoute: Http[Has[UsersService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(createUserTagEndpoint) { case (jwt, jwtType, tagData) =>
      UsersService.createUserTag(jwt, jwtType, tagData.tag)
        .map(user => {
          Right(user)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(UsersRepositoryLive.live >>> UsersServiceLive.live)
    }
  }


  val checkIfTagExistsEndpointRoute: Http[Has[UsersService], Throwable, Request, Response[Any, Throwable]] = {
    ZioHttpInterpreter().toHttp(checkIfTagExistsEndpoint) { case (tag) =>
      UsersService.checkIfTagExists(tag)
        .map(tagExistence => {
          Right(tagExistence)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(UsersRepositoryLive.live >>> UsersServiceLive.live)
    }
  }

  val receiveWebHookEndpointRoute: Http[Has[UsersService], Throwable, Request, Response[Any, Throwable]] = {
   ZioHttpInterpreter().toHttp(receiveWebHookEndpoint)(event => {
      UsersService.insertOrUpdateUserHook(event.data, event.`type`)
        .map(user => {
          Right(user)
        }).catchAll(e => ZIO.succeed(Left(e)))
        .provideLayer(UsersRepositoryLive.live >>> UsersServiceLive.live)
    })
  }
 
}




