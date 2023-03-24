package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import sttp.tapir.server._
import sttp.tapir._
import sttp.tapir.CodecFormat._
import civil.controllers.UsersController._
import civil.apis.BaseApi.{baseEndpoint, baseEndpointAuthenticated}
import civil.models.{ErrorInfo, IncomingUser, OutgoingUser, TagData, TagExists, UpdateUserBio, UpdateUserIcon, WebHookEvent}
import sttp.model.Part

import java.io.File

object UsersApi {
  val upsertDidUserEndpoint: Endpoint[IncomingUser, ErrorInfo, OutgoingUser, Any] =
    baseEndpoint
      .post
      .in("users" / "did-user")
      .in(jsonBody[IncomingUser])
      .out(jsonBody[OutgoingUser])

  val getUserEndpoint: Endpoint[(String, String, String), ErrorInfo, OutgoingUser, Any] =
    baseEndpointAuthenticated
      .get
      .in("users")
      .in(query[String]("userId"))
      .out(jsonBody[OutgoingUser])

  val updateUserIconEndpoint: Endpoint[UpdateUserIcon, ErrorInfo, OutgoingUser, Any] =
    baseEndpoint
      .put
      .in("users")
      .in(jsonBody[UpdateUserIcon])
      .out(jsonBody[OutgoingUser])

  val updateUserBioInformationEndpoint: Endpoint[(String, String, UpdateUserBio), ErrorInfo, OutgoingUser, Any] =
    baseEndpointAuthenticated
      .patch
      .in("users" / "bio-experience")
      .in(jsonBody[UpdateUserBio])
      .out(jsonBody[OutgoingUser])

  val uploadUserIconEndpoint: Endpoint[UpdateUserIcon, ErrorInfo, OutgoingUser, Any] =
    baseEndpoint
      .post
      .in("users" / "upload")
      .in(jsonBody[UpdateUserIcon])
      .out(jsonBody[OutgoingUser])

  val createUserTagEndpoint: Endpoint[(String, String, TagData), ErrorInfo, OutgoingUser, Any] =
    baseEndpointAuthenticated
      .patch
      .in("users" / "tag")
      .in(jsonBody[TagData])
      .out(jsonBody[OutgoingUser])

  val checkIfTagExistsEndpoint: Endpoint[(String), ErrorInfo, TagExists, Any] =
    baseEndpoint
      .get
      .in("users" / "tag-exists")
      .in(query[String]("tag"))
      .out(jsonBody[TagExists])

  val receiveWebHookEndpoint: Endpoint[WebHookEvent, ErrorInfo, Unit, Any] =
    baseEndpoint
      .post
      .in("users" / "clerk-event")
      .in(jsonBody[WebHookEvent])
  // .out(jsonBody[Users])
}