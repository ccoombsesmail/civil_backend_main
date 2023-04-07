package civil.apis

import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import civil.apis.BaseApi.{baseEndpoint, baseEndpointAuthenticated}
import civil.models.{AppError, FollowedUserId, OutgoingUser}
import sttp.tapir._
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zhttp.http.{Http, Request, Response}
import zio._


object FollowsApi {
  
  val newFollowEndpoint: Endpoint[(String, String, FollowedUserId), AppError, OutgoingUser, Any] =
    endpoint.post
      .in("follows")
      .in(jsonBody[FollowedUserId])
      .out(jsonBody[OutgoingUser])

  val deleteFollowEndpoint: Endpoint[(String, String, String), AppError, OutgoingUser, Any] =
    endpoint.delete
      .in("follows")
      .in(query[String]("followedUserId"))
      .out(jsonBody[OutgoingUser])


  val getAllFollowersEndpoint: Endpoint[String, Unit, List[OutgoingUser], Any] =
    endpoint.in("api" / "v1").get
      .in("follows" / "followers")
      .in(query[String]("userId"))
      .out(jsonBody[List[OutgoingUser]])

  val getAllFollowedEndpoint: Endpoint[(String), Unit, List[OutgoingUser], Any] =
    endpoint.in("api" / "v1").get
      .in("follows" / "followed")
      .in(query[String]("userId"))
      .out(jsonBody[List[OutgoingUser]])


}
